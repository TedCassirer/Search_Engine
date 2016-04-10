/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
    /**  Returns the ith posting */
package ir;

import java.util.HashSet;
import java.util.LinkedList;
import java.io.Serializable;
import java.util.ListIterator;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {

    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList();

    private HashSet<Integer> containedDocIDs = new HashSet<>();

    /**  Number of postings in this list  */
    public int size() {
        return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    public void addList(LinkedList<PostingsEntry> l){
        for(PostingsEntry e : l){
            this.add(e);
        }
    }
    public LinkedList<PostingsEntry> getList(){
        return list;
    }
    public ListIterator<PostingsEntry> getIterator() {
        return list.listIterator();
    }

    public void add( int docID, int pos ) {
        if (!containedDocIDs.contains(docID)) {
            list.add(new PostingsEntry(docID));
            containedDocIDs.add(docID);
        }
        list.getLast().addPos(pos);
    }

    public void add( PostingsEntry e ) {
        if(!containedDocIDs.contains(e.docID)){
            list.add(e);
            containedDocIDs.add(e.docID);
        }
    }
}

