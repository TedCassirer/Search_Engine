/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */


package ir;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.intellij.vcs.log.Hash;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.*;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;


public class BSBIIndexer{

    public BSBI bsbi_index;
    public BSBIIndexer(){
        bsbi_index = new BSBI();
    }
}




//
///**
// *   Processes a directory structure and indexes all PDF and text files.
// */
//public class BSBIndexer {
//
//
//
//    /** The index to be built up by this indexer. */
//    public BSBI index = new BSBI(this);
//
//    /** The next docID to be generated. */
//    private int lastDocID = 0;
//
//
//    /* ----------------------------------------------- */
//
//
//    /** Generates a new document identifier as an integer. */
//    private int generateDocID() {
//        return lastDocID++;
//    }
//
//    /** Generates a new document identifier based on the file name. */
//    private int generateDocID( String s ) {
//        return s.hashCode();
//    }
//
//    public int noBlocks;
//    public String[] hashIndexList; //Contains the path to each hashed index stored on disk
//
//    /* ----------------------------------------------- */
//
//
//    /**
//     *  Initializes the index as a HashedIndex.
//     */
//
//    /* ----------------------------------------------- */
//
//
//    /**
//     *  Tokenizes and indexes the file @code{f}. If @code{f} is a directory,
//     *  all its files and subdirectories are recursively processed.
//     */
//    public void processFiles(File f) {
//
//        ArrayList<File> documents = new ArrayList<>();
//        // do not try to index fs that cannot be read
//        if (f.canRead()) {
//            if (f.isDirectory()) {
//                String[] fs = f.list();
//                // an IO error could occur
//                if (fs != null) {
//                    for (int i = 0; i < fs.length; i++) {
//                        documents.add(new File(f, fs[i]));
//                    }
//
//                }
//            }
//        }
//        processDocuments(documents);
//    }
//    public void processDocuments(ArrayList<File> documents){
//        final long startTime = System.currentTimeMillis();
//
//        int noDocuments = documents.size();
//        System.out.println(noDocuments);
//        int documentsPerBlock = Math.round(Math.round(noDocuments / (Math.log(noDocuments))));
//        ArrayList<File[]> blocksList = new ArrayList<>();
//        int blockCounter = 0;
//        System.out.println("Number of documents: " + noDocuments);
//        for (int i = 0; i < noDocuments; i += documentsPerBlock) {
//            int size;
//            if (i + documentsPerBlock < noDocuments) {
//                size = documentsPerBlock;
//            } else {
//                size = noDocuments - i - 1;
//            }
//            System.out.println("Block: " + ++blockCounter + ", documents on this block: " + size);
//            File[] block = new File[size];
//            for (int j = 0; j < size; j++) {
//                block[j] = documents.get(i + j);
//            }
//            blocksList.add(block);
//        }
//        noBlocks = blocksList.size();
//        System.out.println("\n");
//
//
//        HashMap<String, Integer> termList = new HashMap<String, Integer>(400000);
//        HashMap<String, Integer> documentList = new HashMap<String, Integer>(1500);
//
//
//        hashIndexList = new String[noBlocks];
//
//        for(int i = 0; i < noBlocks; i++){
//            HashedIndex tmp = new HashedIndex();
//            String path = "hashIndex/" + i + ".ser";
//            index.writeToDisk(tmp, path);
//            hashIndexList[i] = path;
//        }
//
//        // For each block, index that block
//
//        for (int i = 0; i < blocksList.size(); i++) {
//            HashedIndex blockIndex = new HashedIndex();
//            File[] block = blocksList.get(i);
//            processBlock(block, blockIndex, i);
//
//            index.insert(blockIndex, i);
//            System.out.println("Block " + (i+1) + " out of " + blocksList.size() + " saved to the disk");
//
//        }
//
//        final long endTime = System.currentTimeMillis();
//        System.out.println("Time to index: " + (endTime - startTime)/1000 + "s");
//    }
//
//    public void processBlock(File[] block, HashedIndex blockIndex, int blockNumber){
//        ArrayList<LinkedList<Quartet>> qList = new ArrayList<>();
//        for(int i = 0; i < noBlocks; i++){
//            qList.add(new LinkedList<Quartet>());
//        }
//
//        for(File f : block) {
//
//
//            if (f.canRead()) {
//                // First register the document and get a docID
//                int docID = generateDocID();
//                index.docIDs.put("" + docID, f.getPath());
//                try {
//                    //  Read the first few bytes of the file to see if it is
//                    // likely to be a PDF
//                    Reader reader = new FileReader(f);
//                    char[] buf = new char[4];
//                    reader.read(buf, 0, 4);
//                    if (buf[0] == '%' && buf[1] == 'P' && buf[2] == 'D' && buf[3] == 'F') {
//                        // We assume this is a PDF file
//                        try {
//                            String contents = extractPDFContents(f);
//                            reader = new StringReader(contents);
//                        } catch (IOException e) {
//                            // Perhaps it wasn't a PDF file after all
//                            reader = new FileReader(f);
//                        }
//                    } else {
//                        // We hope this is ordinary text
//                        reader = new FileReader(f);
//                    }
//                    SimpleTokenizer tok = new SimpleTokenizer(reader);
//                    int offset = 0;
//                    while (tok.hasMoreTokens()) {
//                        String token = tok.nextToken();
//                        int block_chosen = hashToken(token);
//                        qList.get(block_chosen).add(new Quartet(docID, token, offset++, blockIndex));
//                    }
//                    index.docLengths.put("" + docID, offset);
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//            }
//        }
//        insertIntoIndex(qList);
//
//    }
//
//
//    /* ----------------------------------------------- */
//
//
//    /**
//     *  Extracts the textual contents from a PDF file as one long string.
//     */
//    public String extractPDFContents( File f ) throws IOException {
//        FileInputStream fi = new FileInputStream( f );
//        PDFParser parser = new PDFParser( fi );
//        parser.parse();
//        fi.close();
//        COSDocument cd = parser.getDocument();
//        PDFTextStripper stripper = new PDFTextStripper();
//        String result = stripper.getText( new PDDocument( cd ));
//        cd.close();
//        return result;
//    }
//
//
//    /* ----------------------------------------------- */
//
//
//    /**
//     *  Writes to the correct hashedIndex by reading and writing them to the disk.
//     */
//    public void insertIntoIndex(ArrayList<LinkedList<Quartet>> qList) {
//        Iterator<Quartet> qt;
//        HashedIndex hashedIndex;
//        for(int i = 0; i < hashIndexList.length; i++){
//            qt = qList.get(i).iterator();
//            hashedIndex = index.readFromDisk(hashIndexList[i]);
//            while(qt.hasNext()) {
//                Quartet q = qt.next();
//                hashedIndex.insert(q.token, q.docID, q.offset);
//            }
//            index.writeToDisk(hashedIndex, hashIndexList[i]);
//        }
//    }
//
//    public int hashToken(String token){
//        return Math.abs(token.hashCode() % noBlocks);
//    }
//}
//
