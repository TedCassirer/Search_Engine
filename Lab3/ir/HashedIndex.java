/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */


package ir;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;


/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /**
     * The index as a hashtable.
     */
    private HashMap<String, PostingsList> index = new HashMap<>();
    //public HashMap<String, String> docIDs = new HashMap<String,String>();
    //public HashMap<String, Integer> docLengths = new HashMap<String,Integer>();
    public HashMap<String, Double> euclideanDocLengths = new HashMap<>();


    /**
     * Inserts this token in the index.
     */
    public void insert(String token, int docID, int offset) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingsList());
        }
        PostingsEntry e = new PostingsEntry(docID);
        index.get(token).add(e, offset);
    }

    /**
     * Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
        return index.keySet().iterator();
    }

    public int size() {
        return index.size();
    }

    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        PostingsList p = index.get(token);
        return p != null ? p : new PostingsList();
    }


    /**
     * Searches the index for postings matching the query.
     */
    public PostingsList search(Query query, int queryType, int rankingType, int structureType) {
        PostingsList results = null;
        int term_count = query.size();
        for (int i = 0; i < term_count; i++) {

        }
        switch (queryType) {
            case Index.INTERSECTION_QUERY:
                results = intersectionQuery(query);
                break;
            case Index.PHRASE_QUERY:
                results = phraseQuery(query);
                break;
            case Index.RANKED_QUERY:
                results = rankedQuery(query, rankingType);
                break;
        }
        return results;
    }

    public PostingsList rankedQuery(Query q, int rankingType) {

        int nb_of_terms = q.size();
        if (nb_of_terms == 0) {
            return null;
        }
        PostingsList result = null;
        switch (rankingType) {
            case Index.TF_IDF:
                result = tfidfScore(q);
                for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
                    PostingsEntry e = entryIterator.next();
                    e.score = e.tfidf;
                }
                break;
            case Index.PAGERANK:
                result = pagerankScore(q);
                for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
                    PostingsEntry e = entryIterator.next();
                    e.score = e.pagerank;
                }
                break;
            case Index.COMBINATION:
                tfidfScore(q);
                result = pagerankScore(q);
                double tfidfsum = 0;
                double prsum = 0;
                for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
                    PostingsEntry e = entryIterator.next();
                    tfidfsum += e.tfidf;
                    prsum += e.pagerank;
                }
                for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
                    PostingsEntry e = entryIterator.next();
                    e.tfidf /= tfidfsum;
                    e.pagerank /= prsum;
                    //e.score = e.pagerank * e.tfidf;
                    e.score = (2 * e.tfidf * e.pagerank) / (e.tfidf + e.pagerank); //Harmonic mean
                    //double w = 0.5;
                    //e.score = w * e.tfidf + (1 - w) * e.pagerank; //Weighted average.
                }
                break;
        }
        if (result.size() == 0) {
            return null;
        }
        return sortByScore(result);
    }


    /**
     * Searches the index for postings matching the query.
     * If you want to improve this method, sort the PostingsLists retrieved from every token by size.
     * Starting with the smallest one first will yield the quickest result.
     * Also possible to add skip pointers.
     */

    public PostingsList intersectionQuery(Query query) {

        if (query.size() == 0) {
            return null;
        }
        Iterator<String> it = query.terms.iterator();
        PostingsList merge = getPostings(it.next());
        while (it.hasNext()) {
            String token = it.next();
            merge = intersectPostings(merge, getPostings(token));
        }
        return merge;
    }

    //Finds the intersection (?) of two postinglists.
    public PostingsList intersectPostings(PostingsList p1, PostingsList p2) {
        ListIterator<PostingsEntry> it1 = p1.getIterator();
        ListIterator<PostingsEntry> it2 = p2.getIterator();
        PostingsList result = new PostingsList();

        while (it1.hasNext() && it2.hasNext()) {
            PostingsEntry e1 = it1.next();
            PostingsEntry e2 = it2.next();
            if (e1.docID == e2.docID) {
                result.add(e1);
            }
            if (e1.docID > e2.docID) {
                it1.previous();
            } else {
                it2.previous();
            }
        }

        return result;
    }

    /**
     * Retrieves postings from first two terms in query.
     * If same document and pos1 == pos2 - 1 create new Entry with this docID and add it to tmp
     * merge = tmp
     * Proceeds to next term and does the algorithm again with the previous token2 as token1 now and new token as token2
     * merge = tmp again
     * tmp will only ever not be empty if pos1 == pos2 - 1. So merge will be empty if not all tokens come after
     * eachother in a doc
     *
     * @param query
     * @return
     */
    public PostingsList phraseQuery(Query query) {
        if (query.size() == 0) {
            return new PostingsList();
        }
        Iterator<String> it = query.terms.iterator();
        PostingsList merge = getPostings(it.next());
        while (it.hasNext()) {
            String token = it.next();
            PostingsList tmp = new PostingsList();
            ListIterator<PostingsEntry> eIt1 = merge.getIterator();
            ListIterator<PostingsEntry> eIt2 = getPostings(token).getIterator();
            while (eIt1.hasNext() && eIt2.hasNext()) {
                PostingsEntry e1 = eIt1.next();
                PostingsEntry e2 = eIt2.next();
                if (e1.docID == e2.docID) {
                    PostingsEntry e = new PostingsEntry(e1.docID);
                    ListIterator<Integer> posIt1 = e1.getIterator();
                    ListIterator<Integer> posIt2 = e2.getIterator();
                    while (posIt1.hasNext() && posIt2.hasNext()) {
                        int pos1 = posIt1.next();
                        int pos2 = posIt2.next();
                        if (pos1 == pos2 - 1) {
                            e.addPos(pos2);
                        } else if (pos1 > pos2) {
                            posIt1.previous();
                        } else {
                            posIt2.previous();
                        }
                    }
                    if (e.size() > 0) {
                        tmp.add(e);
                    }
                } else if (e1.docID > e2.docID) {
                    eIt1.previous();
                } else {
                    eIt2.previous();
                }
            }
            merge = tmp;
        }
        return merge;
    }

    public PostingsList tfidfScore(Query q) {

        double[] scores = new double[docIDs.size()];
        PostingsList result = new PostingsList();

        for (Iterator<String> terms = q.terms.iterator(); terms.hasNext(); ) {
            String token = terms.next();
            double idf = idf(token);
            if (idf != 0) { //If idf = 0 it means that no documents or every document had the term.
                PostingsList p = getPostings(token);
                if (p.size() != 0) {
                    for (Iterator<PostingsEntry> it = p.getIterator(); it.hasNext(); ) {
                        PostingsEntry e = it.next();
                        scores[e.docID] += tf(token, e.docID) * idf;
                        result.add(e);
                    }
                }
            }
        }

        for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
            PostingsEntry e = entryIterator.next();
            e.tfidf = scores[e.docID] / euclideanDocLengths.get("" + e.docID);
            //e.tfidf = scores[e.docID] / docLengths.get("" + e.docID);
            //e.tfidf = scores[e.docID] / Math.sqrt(docLengths.get("" + e.docID));

        }
        return result;
    }

    public PostingsList pagerankScore(Query q) {

        //Loads the titles index

        HashMap<String, Integer> titles = loadTitles();
        double[] pagerank = loadPageRank();
        PostingsList pList = new PostingsList();
        for (Iterator<String> it1 = q.terms.iterator(); it1.hasNext(); ) {
            String token = it1.next();
            for (Iterator<PostingsEntry> it2 = getPostings(token).getIterator(); it2.hasNext(); ) {
                PostingsEntry e = it2.next();
                String docTitle = docIDs.get("" + e.docID);
                docTitle = docTitle.substring(docTitle.lastIndexOf("/") + 1, docTitle.lastIndexOf("."));
                e.pagerank = pagerank[titles.get(docTitle)];
                pList.add(e);
            }
        }

        return pList;

    }


    public double tf(String term, int docID) {
        Iterator<PostingsEntry> it = getPostings(term).getIterator();
        while (it.hasNext()) {
            PostingsEntry e = it.next();
            if (e.docID == docID) {
                return e.size();
            }
        }
        return 0;
    }

    public double idf(String token) {
        PostingsList p = getPostings(token);
        double df = p.size();
        //return Math.log10((double) docIDs.size() / df);
        return Math.log10((double) docIDs.size() / df);
    }

    public PostingsList sortByScore(PostingsList pList) {
        LinkedList<PostingsEntry> list = pList.getList();
        Collections.sort(list, new Comparator<PostingsEntry>() {
            public int compare(PostingsEntry o1, PostingsEntry o2) {
                if(o1.score < o2.score){
                    return 1;
                } else if (o1.score > o2.score){
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        PostingsList result = new PostingsList();
        result.addList(list);
        return result;
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    double[] loadPageRank() {
        ObjectInputStream in = null;
        double[] array = null;
        try {
            in = new ObjectInputStream(new FileInputStream("results/exact.ser"));
            array = (double[]) in.readObject();

        } catch (IOException e) {
            // report
        } catch (ClassNotFoundException fi) {
            // report
        } finally {
            try {
                in.close();
            } catch (Exception ex) {/*ignore*/}
        }
        return array;
    }

    HashMap<String, Integer> loadTitles() {
        ObjectInputStream in = null;
        HashMap<String, Integer> titles = null;
        try {
            in = new ObjectInputStream(new FileInputStream("results/titles.ser"));
            titles = (HashMap<String, Integer>) in.readObject();

        } catch (IOException e) {
            // report
        } catch (ClassNotFoundException fi) {
            // report
        } catch (NullPointerException e) {
        } finally {
            try {
                in.close();
            } catch (Exception ex) {/*ignore*/}
        }
        return titles;
    }


    /**
     * No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }


}