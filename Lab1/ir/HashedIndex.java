/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */



package ir;

import javafx.geometry.Pos;

import java.io.Serializable;
import java.util.*;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index, Serializable{

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    public HashMap<String, String> docIDs = new HashMap<String,String>();

    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingsList());
        }
        index.get(token).add(docID, offset);
    }

    public void insert(String token, PostingsEntry pEntry) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingsList());
        }
        index.get(token).add(pEntry);
    }



    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//

        return index.keySet().iterator();

    }
    public static
    <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
	return index.get(token);
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
        PostingsList results = null;
        int term_count = query.terms.size();
        for(int i = 0; i < term_count; i++){

        }
        if(queryType == Index.INTERSECTION_QUERY){
            results = intersect(query);
        }
        if(queryType == Index.PHRASE_QUERY){
            results = phrase_search(query);
        }
	return results;
    }

    //Finds the intersection (?) of two postinglists.
    public PostingsList intersect(ListIterator<PostingsEntry> p1, ListIterator<PostingsEntry> p2){
        PostingsList result = new PostingsList();
        while(p1.hasNext() && p2.hasNext() ){
            PostingsEntry p1_entry = p1.next();
            PostingsEntry p2_entry = p2.next();
            if(p1_entry.docID == p2_entry.docID){
                result.add(p1_entry);
            }
            //The lists has advanced one step already. Take the one with a bigger ID back one step.
            else if(p1_entry.docID > p2_entry.docID){
                p1.previous();
            }else{
                p2.previous();
            }
        }

        return result;
    }

    public PostingsList intersect(Query query){


        PostingsList result = null;
        int term_count = query.size();

        if(term_count == 0) {
            return null;
        }

        //Adds all postinglists in one list so we can sort it by frequency.
        LinkedList<PostingsList> pListList = new LinkedList<>();
        for(int i = 0; i < term_count; i++){
            pListList.add(getPostings(query.terms.get(i)));
        }
        //sortByFrequency(pListList);

        if(pListList.size() != 0) {
            Iterator<PostingsList> pList = pListList.iterator();
            result = pList.next();
            while (pList.hasNext()) {
                PostingsList tmp = pList.next();
                result = intersect(result.getIterator(), tmp.getIterator());
            }

        }
        return result;
    }

    //Sorts the list by frequency. Lowerst frequency first.
    public LinkedList<PostingsList> sortByFrequency(LinkedList<PostingsList> list){
        Collections.sort(list, new Comparator<PostingsList>() {
            @Override
            public int compare(PostingsList o1, PostingsList o2) {
                return o1.size()-o2.size();
            }
        });
        return list;
    }


    public PostingsList phrase_search(Query query){
        int nb_of_terms = query.terms.size();
        if ( nb_of_terms == 0 ) {
            return null;
        }
        PostingsList merge = getPostings(query.terms.get(0));
        for ( int i = 1; i < nb_of_terms; i++ ) {
            PostingsList tmp = new PostingsList();
            ListIterator<PostingsEntry> a = merge.getIterator();
            ListIterator<PostingsEntry> b =
                    getPostings(query.terms.get(i)).getIterator();
            while ( a.hasNext() && b.hasNext() ) {
                PostingsEntry entry_a = a.next();
                PostingsEntry entry_b = b.next();
                if ( entry_a.docID == entry_b.docID ) {
                    PostingsEntry e = new PostingsEntry(entry_a.docID);
                    ListIterator<Integer> a_p = entry_a.getIterator();
                    ListIterator<Integer> b_p = entry_b.getIterator();
                    while ( a_p.hasNext() && b_p.hasNext() ) {
                        int a_pos = a_p.next();
                        int b_pos = b_p.next();
                        if ( a_pos == b_pos-1 ) {
                            e.addPos(b_pos);
                        } else if ( a_pos > b_pos ) {
                            a_p.previous();
                        } else {
                            b_p.previous();
                        }
                    }
                    if (e.size() > 0) {
                        tmp.add(e);
                    }
                }
                else if ( entry_a.docID > entry_b.docID ) {
                    a.previous();
                }
                else {
                    b.previous();
                }
            }
            merge = tmp;
        }
        return merge;
    }



    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }


}
