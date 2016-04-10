package ir;

import java.util.*;

/**
 * Created by ted-cassirer on 3/11/16.
 */
public class BiWordIndex implements Index {

    /**
     * The index as a hashtable.
     */
    private HashMap<String, PostingsList> index = new HashMap<>();
    //public HashMap<String, String> docIDs = new HashMap<String,String>();
    //public HashMap<String, Integer> docLengths = new HashMap<String,Integer>();
    public HashMap<String, Double> euclideanDocLengths = new HashMap<>();

    public void insert(String token, int docID, int offset) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingsList());
        }
        PostingsEntry e = new PostingsEntry(docID);
        index.get(token).add(e, offset);
    }

    public PostingsList getPostings(String token) {
        PostingsList p = index.get(token);
        if (p == null) {
            return new PostingsList();
        }
        return p;
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

    public PostingsList search( Query query, int queryType, int rankingType, int structureType ){
        try {
            throw new Exception();
        }
        catch (Exception e){
            System.out.println("NO!");
            e.printStackTrace();
        }
        return null;
    };


    public PostingsList search(BiWordQuery query, int queryType, int rankingType, int structureType) {
        PostingsList results = null;
        if (query.size() < 1) {
            return results;
        }
        switch (queryType) {
            case Index.INTERSECTION_QUERY:
                //results = intersectionQuery(query);
                break;
            case Index.PHRASE_QUERY:
                //results = phraseQuery(query);
                break;
            case Index.RANKED_QUERY:
                results = rankedQuery(query, rankingType);
                break;
        }
        return results;
    }


    public PostingsList rankedQuery(BiWordQuery q, int rankingType) {

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
               /* result = pagerankScore(q);
                for (Iterator<PostingsEntry> entryIterator = result.getIterator(); entryIterator.hasNext(); ) {
                    PostingsEntry e = entryIterator.next();
                    e.score = e.pagerank;
                }
                */
                break;
            case Index.COMBINATION:
                /*
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
                    e.score = (2 * e.tfidf*e.pagerank) / (e.tfidf + e.pagerank); //Harmonic mean
                    //double w = 0.5;
                    //e.score = w * e.tfidf + (1 - w) * e.pagerank; //Weighted average.
                }
                */
                break;
        }
        if (result.size() == 0) {
            return null;
        }
        return sortByScore(result);
    }

    public PostingsList tfidfScore(BiWordQuery q) {

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


    public PostingsList sortByScore(PostingsList pList) {
        LinkedList<PostingsEntry> list = pList.getList();
        Collections.sort(list);
        PostingsList result = new PostingsList();
        result.addList(list);
        return result;
    }

    public double tf(String token, int docID) {
        Iterator<PostingsEntry> it = getPostings(token).getIterator();
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
        return Math.log10((double) docIDs.size() / df);
    }

    public void cleanup() {

    }

}
