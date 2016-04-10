package ir;

import java.io.*;
import java.util.*;

/**
 * Created by ted-cassirer on 2/9/16.
 */
public class BSBI implements Serializable {

    public HashMap<String, String> docIDs = new HashMap<String,String>();
    public HashMap<String,Integer> docLengths = new HashMap<String,Integer>();

    private static BSBIIndexer indexer;

    public BSBI(BSBIIndexer indexer){
        this.indexer = indexer;
    }


    public PostingsList search( Query query, int queryType, int rankingType, int structureType ){
        final long startTime = System.currentTimeMillis();


        System.out.println("\nNew search: " + query.terms);
        HashedIndex index = new HashedIndex();
        HashedIndex tempIndex;

        int term_count = query.size();

        if (term_count == 0) {
            return null;
        }

        for (Iterator<String> it = query.terms.iterator(); it.hasNext();) {
            String token = it.next();
            tempIndex = indexer.readFromDisk(indexer.indexTree.get(indexer.generateHashedToken(token)));

            for (Iterator<PostingsEntry> it2 = tempIndex.getPostings(token).getIterator(); it2.hasNext(); ) {
                PostingsEntry e = it2.next();
                index.insert(token, e);
                docIDs.put("" + e.docID, tempIndex.docIDs.get("" + e.docID));
                docLengths.put("" + e.docID, tempIndex.docLengths.get("" + e.docID));
            }
        }
        PostingsList result = index.search(query, queryType, rankingType, structureType);

        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime)/1000 + "s");
        clearHashes();
        if(result.size() != 0) {
            return result;
        }
        else{
            return null;
        }
    }

    public void clearHashes(){
        docIDs = new HashMap<>();
        docLengths = new HashMap<>();
    }

    public void cleanup(){

        System.out.println(indexer.indexTree.size());
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(
                    new FileOutputStream(indexer.indexStoragePath + "tree.ser"));
            output.writeObject(indexer.indexTree);
            System.out.println("Saved the indexTree to: " + indexer.indexStoragePath + "tree.ser");
        } catch (IOException ex) {
            // report
        } finally {
            try {output.close();} catch (Exception ex) {/*ignore*/}
        }
    }


}
