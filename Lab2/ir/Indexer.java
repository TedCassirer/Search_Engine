/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  


package ir;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.*;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer implements Serializable {

    /** The index to be built up by this indexer. */
    public Index index;
    
    /** The next docID to be generated. */
    private int lastDocID = 0;


    /* ----------------------------------------------- */


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
	return lastDocID++;
    }

    /** Generates a new document identifier based on the file name. */
    private int generateDocID( String s ) {
	return s.hashCode();
    }


    /* ----------------------------------------------- */


    /**
     *  Initializes the index as a HashedIndex.
     */
    public Indexer() {
		index = new HashedIndex();
    }


    /* ----------------------------------------------- */


    /**
     *  Tokenizes and indexes the file @code{f}. If @code{f} is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f ) {
	// do not try to index fs that cannot be read
		//System.out.println("Files: " + f.getName());
		if ( f.canRead() ) {
			if ( f.isDirectory() ) {
				String[] fs = f.list();
				// an IO error could occur
				if ( fs != null ) {
					for ( int i=0; i<fs.length; i++ ) {
					processFiles( new File( f, fs[i] ));
					}

				}
			} else {
				//System.err.println( "Indexing " + f.getPath() );
				// First register the document and get a docID
				int docID = generateDocID();
				index.docIDs.put( "" + docID, f.getPath() );
				try {
					//  Read the first few bytes of the file to see if it is
					// likely to be a PDF
					Reader reader = new FileReader( f );
					char[] buf = new char[4];
					reader.read( buf, 0, 4 );
					if ( buf[0] == '%' && buf[1]=='P' && buf[2]=='D' && buf[3]=='F' ) {
					// We assume this is a PDF file
					try {
						String contents = extractPDFContents( f );
						reader = new StringReader( contents );
					}
					catch ( IOException e ) {
						// Perhaps it wasn't a PDF file after all
						reader = new FileReader( f );
					}

					}
					else {
						// We hope this is ordinary text
						reader = new FileReader( f );
					}
					SimpleTokenizer tok = new SimpleTokenizer( reader );
					int offset = 0;
					HashMap<String, Double> eucDocLength = new HashMap<>();
					while ( tok.hasMoreTokens() ) {
						String token = tok.nextToken();
						if(!eucDocLength.containsKey(token)){
							eucDocLength.put(token, 1.0);
						} else {
							eucDocLength.put(token, eucDocLength.get(token) + 1.0);
						}
						insertIntoIndex( docID, token, offset++ );
					}
					double eucLength = 0.0;
					for(Iterator<Double> it = eucDocLength.values().iterator(); it.hasNext(); ){
						eucLength += Math.pow(it.next(), 2);
					}
					eucLength = Math.sqrt(eucLength);
					index.docLengths.put( "" + docID, offset );
					index.euclideanDocLengths.put("" + docID, eucLength);
					reader.close();
				}
				catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
    }


    /* ----------------------------------------------- */


    /**
     *  Extracts the textual contents from a PDF file as one long string.
     */
    public String extractPDFContents( File f ) throws IOException {
	FileInputStream fi = new FileInputStream( f );
	PDFParser parser = new PDFParser( fi );   
	parser.parse();   
	fi.close();
	COSDocument cd = parser.getDocument();   
	PDFTextStripper stripper = new PDFTextStripper();   
	String result = stripper.getText( new PDDocument( cd ));  
	cd.close();
	return result;
    }


	public void saveIndex(){
		try {
			System.out.println("SAVING SHIT FUCK");
			FileOutputStream fout = new FileOutputStream("savedIndex/index.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();
			System.out.println("SAVING SHIT FUCK");
			fout = new FileOutputStream("savedIndex/mainIndex.ser");
			oos = new ObjectOutputStream(fout);
			oos.writeObject(index);
			oos.close();

			System.out.println("Done");

		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println("SAVED INDEX HIHOHOA");
		//System.exit( 0 );
	}

	public void loadIndex(){

		ObjectInputStream in = null;
		try{
			in = new ObjectInputStream(new FileInputStream("savedIndex/index.ser"));
			index = (HashedIndex) in.readObject();
		}catch(IOException es) {
			// report
		}catch(ClassNotFoundException fi){
			// report
		}
		finally{
			try {
				in.close();
			} catch (Exception ex) {/*ignore*/}
		}
	}

    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
	index.insert( token, docID, offset );
    }
}
	
