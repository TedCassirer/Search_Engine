/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Hedvig Kjellström, 2012
 */

package ir;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class BiWordQuery {

    public LinkedList<String> terms = new LinkedList<String>();
    public LinkedList<Double> weights = new LinkedList<Double>();

    /**
     * Creates a new empty Query
     */
    public BiWordQuery() {
    }

    /**
     * Creates a new Query from a string of words
     */
    public BiWordQuery(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        if (tok.hasMoreTokens()) {
            String tok1 = tok.nextToken();
            while (tok.hasMoreTokens()) {
                String tok2 = tok.nextToken();
                String token = tok1 + " " + tok2;
                terms.add(token);
                weights.add(new Double(1));
                tok1 = tok2;
            }
        }

    }

    /**
     * Returns the number of terms
     */
    public int size() {
        return terms.size();
    }

    /**
     * Returns a shallow copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        queryCopy.terms = (LinkedList<String>) terms.clone();
        queryCopy.weights = (LinkedList<Double>) weights.clone();
        return queryCopy;
    }

    /**
     * Expands the Query using Relevance Feedback
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Indexer indexer) {

        double alpha = 0.8;
        double beta = 0.6;

        HashSet<String> allTerms = new HashSet<>();

        //Puts all terms in the query into a vector (hash) with each element being a specific term.
        HashMap<String, Double> q0 = new HashMap<>();
        for (String token : terms) {
            double idf = idf(token, indexer);
            if (idf != 0) {
                allTerms.add(token);
                if (!q0.containsKey(token)) {
                    q0.put(token, idf);
                } else {
                    q0.put(token, q0.get(token) + idf);
                }
            }
        }
        //Calculates the query vector length
        double q0Length = 0;
        for (double val : q0.values()) {
            q0Length += Math.pow(val, 2);
        }
        q0Length = Math.sqrt(q0Length);

        //Scales the original query vector with its euclidean distance and the alpha value.
        double a = alpha / q0Length;
        for (Iterator<Map.Entry<String, Double>> it = q0.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> q0Temp = it.next();
            q0Temp.setValue(a * q0Temp.getValue());
        }

        ArrayList<PostingsEntry> dr = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (docIsRelevant[i]) dr.add(results.get(i));
        }
        // If no feedback was given, just do a normal query.
        if (dr.size() == 0) {
            return;
        }

        //Reads through every document labeled relevant and adds each token to 'words'. The end result is a relevant
        //documents vector.
        //tf is implicitly added here since if a term is repeated idf(token) is added again to the value.
        HashMap<String, Double> words = new HashMap<>();
        for (PostingsEntry e : dr) {
            String path = indexer.index.docIDs.get("" + e.docID);
            File f = new File(path);
            try {
                Reader reader = new FileReader(f);
                SimpleTokenizer tok = new SimpleTokenizer(reader);
                String token1 = "";
                if(tok.hasMoreTokens()) {
                    token1 = tok.nextToken();
                }
                while ( tok.hasMoreTokens() ) {
                    String token2 = tok.nextToken();
                    String token = token1 + " " + token2;
                    double idf = idf(token, indexer);
                    if (idf != 0) {
                        if (!words.containsKey(token)) {
                            words.put(token, idf);
                        } else {
                            words.put(token, words.get(token) + idf);
                        }
                        allTerms.add(token);
                    }
                    token1 = token2;
                }

                reader.close();
            } catch (IOException er) {
                er.printStackTrace();
            }
        }

        double drLength = 0;
        for (double val : words.values()) {
            drLength += Math.pow(val, 2);
        }
        drLength = Math.sqrt(drLength);
        System.out.println("drLength: " + drLength);

        System.out.println("q0Length: " + q0Length);

        //Scales the relevant document vector
        double b = beta / drLength;
        for (Iterator<Map.Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> wordTemp = it.next();
            wordTemp.setValue(b * wordTemp.getValue());
        }

        //The original query and the relevant document vector has now been calculated and normalized.
        //Only thing left is to sum them together.
        BiWordQuery qm = new BiWordQuery();
        LinkedList<SortingClass> sorter = new LinkedList<SortingClass>();
        for (String token : allTerms) {
            //System.out.println(token);

            double q0Val = q0.containsKey(token) ? q0.get(token) : 0.0;
            double wordsVal = words.containsKey(token) ? words.get(token) : 0.0;

            sorter.add(new SortingClass(token, q0Val + wordsVal));
        }
        Collections.sort(sorter);
        int i = 0;
        //Ugly filter of the top 20% weights
        for(Iterator<SortingClass> it = sorter.iterator(); it.hasNext() && i < sorter.size()*0.2; i++){
            SortingClass temp = it.next();
            qm.terms.add(temp.term);
            qm.weights.add(temp.weight);
        }
        terms = qm.terms;
        weights = qm.weights;

        System.out.println("Terms in the new Query: " + terms.size());
    }

    //If the token appears in more than 10% of all documents it returns 0 (Which means it will be discarded).
    double idf(String token, Indexer indexer) {
        double idf = indexer.biWordIndex.idf(token);
        return idf;
    }

    private class SortingClass implements Comparable<SortingClass>{
        public String term;
        public Double weight;
        SortingClass(String term, Double weight){
            this.term = term;
            this.weight = weight;
        }

        @Override
        public int compareTo(SortingClass o) {
            if(this.weight <= o.weight) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}


