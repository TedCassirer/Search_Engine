/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    private LinkedList<Integer> pos = new LinkedList<Integer>();
    public double score;

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
        return Double.compare( other.score, score );
    }

    //
    //  YOUR CODE HERE
    //
    public PostingsEntry( int id ) {
        docID = id;
    }

    public void addPos( int p ) {
        pos.add(p);
    }

    public ListIterator<Integer> getIterator() {
        return pos.listIterator(0);
    }

    public int size() {
        return pos.size();
    }
}