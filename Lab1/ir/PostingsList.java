/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
    /**  Returns the ith posting */
package ir;

import java.util.LinkedList;
import java.io.Serializable;
import java.util.ListIterator;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {

    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();


    /**  Number of postings in this list  */
    public int size() {
        return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    //
    //  YOUR CODE HERE
    //
    public ListIterator<PostingsEntry> getIterator() {
        return list.listIterator(0);
    }

    public void add( int docID ) {

        // Assumes docID comes in order
        if ( list.size() == 0 ||
                list.getLast().docID != docID ) {
            list.add(new PostingsEntry(docID));
        }
    }

    public void add( int docID, int pos ) {
        if ( list.size() == 0 ||
                list.getLast().docID != docID ) {
            list.add(new PostingsEntry(docID));
        }
        list.getLast().addPos(pos);
    }

    public void add( PostingsEntry p ) {
        list.add(p);
    }
}

