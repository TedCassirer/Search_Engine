/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Johan Boye, 2012
 */

// java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt
package pagerank;

import java.util.*;
import java.io.*;

public class PageRank{

	/**
	 *   Maximal number of documents. We're assuming here that we
	 *   don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	int NUMBER_OF_DOCS;

	/**
	 *   Mapping from document names to document numbers.
	 */
	Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 *   A memory-efficient representation of the transition matrix.
	 *   The outlinks are represented as a Hashtable, whose keys are
	 *   the numbers of the documents linked from.<p>
	 *
	 *   The value corresponding to key i is a Hashtable whose keys are
	 *   all the numbers of documents j that i links to.<p>
	 *
	 *   If there are no outlinks from i, then the value corresponding
	 *   key i is null.
	 */
	Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

	/**
	 *   The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * Given the title of the document the docID is returned
	 */
	HashMap<String, Integer> titles = new HashMap<>();
	/**
	 *   The number of documents with no outlinks.
	 */
	int numberOfSinks = 0;

	/**
	 *   The probability that the surfer will be bored, stop
	 *   following links, and take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 *   Convergence criterion: Transition probabilities do not
	 *   change more that EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/**
	 *   Never do more than this number of iterations regardless
	 *   of whether the transistion probabilities converge or not.
	 */
	final static int MAX_NUMBER_OF_ITERATIONS = 100000;

	double[] exact;
	HashMap<String, Double> exactPageRank = new HashMap<>();
	ArrayList<Pair> exactResults;

	/* --------------------------------------------- */
	double N;
	int check = 20;
	public PageRank( String filename ) {
		int noOfDocs = readDocs( filename );
		NUMBER_OF_DOCS = noOfDocs;

		//exact = readArray("exact");
		//exactResults = sort(exact);


		//System.out.println("\nIndexing titles...");
		indexTitles("pagerank/articleTitles.txt");
		//System.out.println("\nExact: ");
		//computePagerank();


	}


	/* --------------------------------------------- */
	void indexTitles( String filename ){
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf(";");
				String docID = line.substring(0, index);
				String title = line.substring(index + 1);

				titles.put(title, docNumber.get(docID)); //They start at 1 here
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );

		ObjectOutputStream ops = null;
		try {
			ops = new ObjectOutputStream(
					new FileOutputStream("results/titles.ser")
			);
			ops.writeObject(titles);
		} catch(IOException e) {
			// report
		} finally{
			try {
				ops.flush();
				ops.close();
			} catch (Exception ex) {/*ignore*/}
		}

	}

	/**
	 *   Reads the documents and creates the docs table. When this method
	 *   finishes executing then the @code{out} vector of outlinks is
	 *   initialised for each doc, and the @code{p} matrix is filled with
	 *   zeroes (that indicate direct links) and NO_LINK (if there is no
	 *   direct link. <p>
	 *
	 *   @return the number of documents read.
	 */
	int readDocs( String filename ) {
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put( title, fromdoc );
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get( otherTitle );
					if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if ( link.get(fromdoc) == null ) {
						link.put(fromdoc, new Hashtable<Integer,Boolean>());
					}
					if ( link.get(fromdoc).get(otherDoc) == null ) {
						link.get(fromdoc).put( otherDoc, true );
						out[fromdoc]++;
					}
				}
			}
			if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
			}
			else {
				System.err.print( "done. " );
			}
			// Compute the number of sinks.
			for ( int i=0; i<fileIndex; i++ ) {
				if ( out[i] == 0 )
					numberOfSinks++;
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
	}


    /* --------------------------------------------- */


	/*
     *   Computes the pagerank of each document.
     */
	void computePagerank() {
		N = NUMBER_OF_DOCS ;
		double[] mc1 = new double[NUMBER_OF_DOCS];
		List<Double> diffArray1 = new ArrayList<>();
		List<Double> diffArrayWorst1 = new ArrayList<>();

		double[] mc2 = new double[NUMBER_OF_DOCS];
		List<Double> diffArray2 = new ArrayList<>();
		List<Double> diffArrayWorst2 = new ArrayList<>();

		double[] mc3 = new double[NUMBER_OF_DOCS];
		List<Double> diffArray3 = new ArrayList<>();
		List<Double> diffArrayWorst3 = new ArrayList<>();

		double[] mc4 = new double[NUMBER_OF_DOCS];
		List<Double> diffArray4 = new ArrayList<>();
		List<Double> diffArrayWorst4 = new ArrayList<>();

		double[] mc5 = new double[NUMBER_OF_DOCS];
		List<Double> diffArray5 = new ArrayList<>();
		List<Double> diffArrayWorst5 = new ArrayList<>();

		List<Integer> NArray = new ArrayList<>();
		List<Double> diffArrayExact = new ArrayList<>();


		String path = "exact";
		long startTime = System.currentTimeMillis();

		//out[d] = Number of out links in doc d

		int count = 0;
		double [] pi0 = new double[NUMBER_OF_DOCS];
		double n = NUMBER_OF_DOCS;
		for(int i = 0; i < NUMBER_OF_DOCS; i++){
			pi0[i] = (1.0)/n;
		}
		double [] pi1 = new double[NUMBER_OF_DOCS];

		while(count < 20){
			count++;
			for(int i = 0; i < NUMBER_OF_DOCS; i++){
				pi1[i] = 0;
				for(int j = 0; j < NUMBER_OF_DOCS; j++){
					double p = 0;
					if(out[j] == 0){
						p = 1.0/n;
					} else if(link.get(j).get(i) == null) {
						p = BORED/n;
					} else if(link.get(j).get(i)){
						p = BORED/n;
						p += (1.0-BORED)/(double)out[j];
					} else {
						System.out.println("I shat the bed...");
					}
					pi1[i] += pi0[j]*p;
				}
			}

			double diff = 0;
			for(int i = 0; i < NUMBER_OF_DOCS; i++){
				diff += Math.abs(pi1[i]-pi0[i]);
			}

			System.out.println("\nDiff: " + diff + ", Epsilon: " + EPSILON);
			if (diff < EPSILON){
				System.out.println("Converged");
			}

			System.out.println("Counts: " + count);
			pi0 = pi1.clone();
			exact = pi1.clone();
			normalize(exact);
			mc1 = mc1(mc1);
			mc2 = mc2(mc2);
			mc3 = mc3(mc3);
			mc4 = mc4(mc4);
			mc5 = mc5(mc5);

			diffArray1.add(goodness1(exact, mc1));
			diffArray2.add(goodness1(exact, mc2));
			diffArray3.add(goodness1(exact, mc3));
			diffArray4.add(goodness1(exact, mc4));
			diffArray5.add(goodness1(exact, mc5));

			diffArrayWorst1.add(goodness2(exact, mc1));
			diffArrayWorst2.add(goodness2(exact, mc2));
			diffArrayWorst3.add(goodness2(exact, mc3));
			diffArrayWorst4.add(goodness2(exact, mc4));
			diffArrayWorst5.add(goodness2(exact, mc5));

			NArray.add(count);
			diffArrayExact.add(diff);
		}

		long endTime   = System.currentTimeMillis();
		long totalTime = (endTime - startTime)/1000;

		/*
		printToFile(diffArray1.toArray(), "results/diffArray1");
		printToFile(diffArray2.toArray(), "results/diffArray2");
		printToFile(diffArray3.toArray(), "results/diffArray3");
		printToFile(diffArray4.toArray(), "results/diffArray4");
		printToFile(diffArray5.toArray(), "results/diffArray5");
		printToFile(diffArrayExact.toArray(), "results/diffArrayExact");

		printToFile(diffArrayWorst1.toArray(), "results/diffArrayWorst1");
		printToFile(diffArrayWorst2.toArray(), "results/diffArrayWorst2");
		printToFile(diffArrayWorst3.toArray(), "results/diffArrayWorst3");
		printToFile(diffArrayWorst4.toArray(), "results/diffArrayWorst4");
		printToFile(diffArrayWorst5.toArray(), "results/diffArrayWorst5");

		printToFile(NArray.toArray(), "results/NArray");
		printToFile(diffArrayExact.toArray(), "results/exactArray");
		*/

		sortAndPrint(pi1, path);
		exact = pi1;
		saveArray(exact, "exact");
		System.out.println("This took us: " + totalTime/60 + "  minutes and " + totalTime % 60 + " seconds."); //16
	}

	double goodness1(double[] piTemp1, double[] piTemp2){
		double[] piExact = piTemp1.clone();
		normalize(piExact);
		double[] piMC = piTemp2.clone();
		normalize(piMC);

		ArrayList<Pair> res = sort(piExact);
		double sum = 0;
		for(int i = 0; i < 50; i++){
			int docIndex = res.get(i).getIndex();
			sum += Math.pow(piExact[docIndex] - piMC[docIndex], 2);
		}
		return sum;
	}
	double goodness2(double[] piTemp1, double[] piTemp2){
		double[] piExact = piTemp1.clone();
		normalize(piExact);
		double[] piMC = piTemp2.clone();
		normalize(piMC);

		ArrayList<Pair> res = sort(piExact);
		double sum = 0;
		for(int i = NUMBER_OF_DOCS-50; i < NUMBER_OF_DOCS; i++){
			int docIndex = res.get(i).getIndex();
			sum += Math.pow(piExact[docIndex] - piMC[docIndex], 2);
		}
		return sum;
	}

	//MC end-point with random start
	double[] mc1(double[] pi){
		Random random = new Random();
		int node;
		int walks = 0;
		while(walks < N) {
			node = random.nextInt(NUMBER_OF_DOCS);
			node = walk(node, random, false);
			pi[node]++;
			walks++;
		}
		double[] pi1 = pi.clone();
		normalize(pi1);
		sortAndPrint(pi1, "mc1");
		return pi;
	}

	// MC end-point with cyclic start
	double[] mc2(double[] pi){
		Random random = new Random();
		int walks = 0;
		while(walks < N) { //m times
			for (int n = 0; n < NUMBER_OF_DOCS; n++) {
				int node = n;
				node = walk(node, random, false);
				pi[node]++;
				walks++;
			}
		}

		double[] pi1 = pi.clone();
		normalize(pi1);
		sortAndPrint(pi1, "mc2");
		return pi;
	}

	//MC complete path
	double[] mc3(double[] pi){
		Random random = new Random();
		int walks = 0;
		while(walks < N){ //m times
			for(int n = 0; n < NUMBER_OF_DOCS; n++){ //Cycles the starting point. (Each document)
				int node = n;
				pi[node]++;
				while(random.nextDouble() > BORED) {
					node = walk(node, random, true);
					pi[node]++;
				}
				walks++;
			}
		}
		double[] pi1 = pi.clone();
		normalize(pi1);
		sortAndPrint(pi1, "mc3");
		return pi;
	}

	// MC complete path stopping at dangling nodes
	double[] mc4(double[] pi){
		Random random = new Random();
		int walks= 0;
		while(walks < N){ //m times
			for(int n = 0; n < NUMBER_OF_DOCS; n++){ //Cycles the starting point. (Each document)
				int node = n;
				pi[node]++;
				while(random.nextDouble() > BORED && out[node] != 0) {
					node = walk(node, random, true);
					pi[node]++;
				}
				walks++;
			}
		}
		double[] pi1 = pi.clone();
		normalize(pi1);
		sortAndPrint(pi1, "mc4");
		return pi;
	}

	//MC complete path with random start
	double[] mc5(double[] pi){
		Random random = new Random();
		int walks = 0;
		while(walks < N) {
			int node = random.nextInt(NUMBER_OF_DOCS);//Starts the walk at a random document.
			pi[node]++;
			while(random.nextDouble() > BORED && out[node] != 0) {
				node = walk(node, random, true);
				pi[node]++;
			}
			walks++;
		}
		double[] pi1 = pi.clone();
		normalize(pi1);
		sortAndPrint(pi1, "mc5");
		return pi;
	}

	void normalize(double[] pi){
		double sum = 0;
		for(int i = 0; i < NUMBER_OF_DOCS; i++){
			sum += pi[i];
		}
		for(int i = 0; i < NUMBER_OF_DOCS; i++){
			pi[i] /= sum;
		}
	}

	int walk(int node, Random random, boolean complete) {
		float chance = random.nextFloat();
		if (out[node] == 0) {
			node = random.nextInt(NUMBER_OF_DOCS);
		} else{
			int linkNumber = random.nextInt(out[node]);
			Enumeration<Integer> links = link.get(node).keys();
			for (int i = 0; i < linkNumber; i++) {
				links.nextElement();
			}
			node = links.nextElement();
		}
		if(complete || chance < BORED){
			return node;
		} else{
			return walk(node, random, complete);
		}
	}

	void sortAndPrint(double[] pi, String filename){
		String path = "results/" + filename;
		normalize(pi);
		ArrayList<Pair> res = sort(pi);
		printToFile(res, path + ".txt");
	}

	void printToFile(ArrayList<Pair> res, String path){
		PrintStream output = null;
		try {
			output = new PrintStream(path);
			PrintStream temp = System.out;
			System.setOut(output);
			for (int i = 0; i < 50; i++) {
				System.out.println(i + 1 + ". " + res.get(i));
			}
			System.setOut(temp);
		} catch(FileNotFoundException e){

		}finally {
			try {output.close();} catch (Exception ex) {/*ignore*/}
		}
	}

	void printToFile(Object[] res, String path){
		PrintStream output = null;
		try {
			output = new PrintStream(path);
			PrintStream temp = System.out;
			System.setOut(output);
			//System.out.print("[");
			for (int i = 0; i < res.length; i++) {
				System.out.print(res[i] + ",");
			}
			//System.out.print("];");
			System.setOut(temp);

			System.out.println(path + " saved");
		} catch(FileNotFoundException e){

		}finally {
			try {output.close();} catch (Exception ex) {/*ignore*/}
		}
	}
		void saveArray(double[] pi, String path){
		ObjectOutputStream ops = null;
		try {
			ops = new ObjectOutputStream(
					new FileOutputStream("results/" + path + ".ser")
			);
			ops.writeObject(pi);
		} catch(IOException e) {
			// report
		} finally{
			try {
				ops.flush();
				ops.close();
			} catch (Exception ex) {/*ignore*/}
		}
	}

	private class Pair implements Comparable<Pair>{
		private String docNumb;
		private double pagerank;
		private int index;
		public Pair(String key, double value, int i){
			docNumb = key;
			pagerank = value;
			index = i;
		}

		public double getPagerank(){
			return pagerank;
		}

		public int getIndex(){
			return index;
		}

		public int compareTo(Pair o){
			if (this.pagerank == o.getPagerank()) return 0;
			if (this.pagerank < o.getPagerank()) return 1;
			return -1;
		}

		public String toString(){
			return String.format("%5s %f", docNumb, pagerank);
		}
	}


	ArrayList<Pair> sort(double[] pi){
		ArrayList<Pair> res = new ArrayList<Pair>();
		for(int i = 0; i < NUMBER_OF_DOCS; i++){
			res.add(new Pair(docName[i], pi[i], i));
		}
		Collections.sort(res);
		return res;
	}
    /* --------------------------------------------- */


	public static void main( String[] args ) {
		//java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt

		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the link file" );
		}
		else {
			new PageRank( args[0] );
		}

	}
}




//
//
///*
// *   This file is part of the computer assignment for the
// *   Information Retrieval course at KTH.
// *
// *   First version:  Johan Boye, 2012
// */
//
//// java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt
//
//
//package pagerank;
//
//import java.util.*;
//import java.io.*;
//
//public class PageRank{
//
//    /**
//     *   Maximal number of documents. We're assuming here that we
//     *   don't have more docs than we can keep in main memory.
//     */
//    final static int MAX_NUMBER_OF_DOCS = 2000000;
//
//	int NUMBER_OF_DOCS;
//
//    /**
//     *   Mapping from document names to document numbers.
//     */
//    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();
//
//    /**
//     *   Mapping from document numbers to document names
//     */
//    String[] docName = new String[MAX_NUMBER_OF_DOCS];
//
//    /**
//     *   A memory-efficient representation of the transition matrix.
//     *   The outlinks are represented as a Hashtable, whose keys are
//     *   the numbers of the documents linked from.<p>
//     *
//     *   The value corresponding to key i is a Hashtable whose keys are
//     *   all the numbers of documents j that i links to.<p>
//     *
//     *   If there are no outlinks from i, then the value corresponding
//     *   key i is null.
//     */
//    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();
//
//    /**
//     *   The number of outlinks from each node.
//     */
//    int[] out = new int[MAX_NUMBER_OF_DOCS];
//
//	/**
//	 * Given the title of the document the docID is returned
//	 */
//	HashMap<String, Integer> titles = new HashMap<>();
//    /**
//     *   The number of documents with no outlinks.
//     */
//    int numberOfSinks = 0;
//
//    /**
//     *   The probability that the surfer will be bored, stop
//     *   following links, and take a random jump somewhere.
//     */
//    final static double BORED = 0.15;
//
//    /**
//     *   Convergence criterion: Transition probabilities do not
//     *   change more that EPSILON from one iteration to another.
//     */
//    final static double EPSILON = 0.0001;
//
//    /**
//     *   Never do more than this number of iterations regardless
//     *   of whether the transistion probabilities converge or not.
//     */
//    final static int MAX_NUMBER_OF_ITERATIONS = 100000;
//
//	double[] exact;
//	HashMap<String, Double> exactPageRank = new HashMap<>();
//	ArrayList<Pair> exactResults;
//
//    /* --------------------------------------------- */
//	double N;
//	int check = 20;
//    public PageRank( String filename ) {
//		int noOfDocs = readDocs( filename );
//		NUMBER_OF_DOCS = noOfDocs;
//
//		exact = readArray("exact");
//		exactResults = sort(exact);
//
//		//System.out.println("\nIndexing titles...");
//		//indexTitles("pagerank/articleTitles.txt");
//		//System.out.println("\nExact: ");
//		//computePagerank();
//
//		N = NUMBER_OF_DOCS ;
//		System.out.println("\nMC1: ");
//		mc1();
//		System.out.println("\nMC2: ");
//		mc2();
//		System.out.println("\nMC3: ");
//		mc3();
//		System.out.println("\nMC4: ");
//		mc4();
//		System.out.println("\nMC5: ");
//		mc5();
//
//
//
//    }
//
//
//    /* --------------------------------------------- */
//	void indexTitles( String filename ){
//		int fileIndex = 0;
//		try {
//			System.err.print( "Reading file... " );
//			BufferedReader in = new BufferedReader( new FileReader( filename ));
//			String line;
//			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
//				int index = line.indexOf(";");
//				String docID = line.substring(0, index);
//				String title = line.substring(index + 1);
//
//				titles.put(title, docNumber.get(docID)); //They start at 1 here
//			}
//		}
//		catch ( FileNotFoundException e ) {
//			System.err.println( "File " + filename + " not found!" );
//		}
//		catch ( IOException e ) {
//			System.err.println( "Error reading file " + filename );
//		}
//		System.err.println( "Read " + fileIndex + " number of documents" );
//
//		ObjectOutputStream ops = null;
//		try {
//			ops = new ObjectOutputStream(
//					new FileOutputStream("results/titles.ser")
//			);
//			ops.writeObject(titles);
//		} catch(IOException e) {
//			// report
//		} finally{
//			try {
//				ops.flush();
//				ops.close();
//			} catch (Exception ex) {/*ignore*/}
//		}
//
//	}
//
//    /**
//     *   Reads the documents and creates the docs table. When this method
//     *   finishes executing then the @code{out} vector of outlinks is
//     *   initialised for each doc, and the @code{p} matrix is filled with
//     *   zeroes (that indicate direct links) and NO_LINK (if there is no
//     *   direct link. <p>
//     *
//     *   @return the number of documents read.
//     */
//    int readDocs( String filename ) {
//	int fileIndex = 0;
//	try {
//	    System.err.print( "Reading file... " );
//	    BufferedReader in = new BufferedReader( new FileReader( filename ));
//	    String line;
//	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
//		int index = line.indexOf( ";" );
//		String title = line.substring( 0, index );
//		Integer fromdoc = docNumber.get( title );
//		//  Have we seen this document before?
//		if ( fromdoc == null ) {
//		    // This is a previously unseen doc, so add it to the table.
//		    fromdoc = fileIndex++;
//		    docNumber.put( title, fromdoc );
//		    docName[fromdoc] = title;
//		}
//		// Check all outlinks.
//		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
//		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
//		    String otherTitle = tok.nextToken();
//		    Integer otherDoc = docNumber.get( otherTitle );
//		    if ( otherDoc == null ) {
//			// This is a previousy unseen doc, so add it to the table.
//			otherDoc = fileIndex++;
//			docNumber.put( otherTitle, otherDoc );
//			docName[otherDoc] = otherTitle;
//		    }
//		    // Set the probability to 0 for now, to indicate that there is
//		    // a link from fromdoc to otherDoc.
//		    if ( link.get(fromdoc) == null ) {
//			link.put(fromdoc, new Hashtable<Integer,Boolean>());
//		    }
//		    if ( link.get(fromdoc).get(otherDoc) == null ) {
//			link.get(fromdoc).put( otherDoc, true );
//			out[fromdoc]++;
//		    }
//		}
//	    }
//	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
//		System.err.print( "stopped reading since documents table is full. " );
//	    }
//	    else {
//		System.err.print( "done. " );
//	    }
//	    // Compute the number of sinks.
//	    for ( int i=0; i<fileIndex; i++ ) {
//		if ( out[i] == 0 )
//		    numberOfSinks++;
//	    }
//	}
//	catch ( FileNotFoundException e ) {
//	    System.err.println( "File " + filename + " not found!" );
//	}
//	catch ( IOException e ) {
//	    System.err.println( "Error reading file " + filename );
//	}
//	System.err.println( "Read " + fileIndex + " number of documents" );
//	return fileIndex;
//    }
//
//
//    /* --------------------------------------------- */
//
//
//    /*
//     *   Computes the pagerank of each document.
//     */
//	void computePagerank() {
//
//		String path = "exact";
//		long startTime = System.currentTimeMillis();
//
//		//out[d] = Number of out links in doc d
//
//		int count = 0;
//		double [] pi0 = new double[NUMBER_OF_DOCS];
//		double n = NUMBER_OF_DOCS;
//		for(int i = 0; i < NUMBER_OF_DOCS; i++){
//			pi0[i] = (1.0)/n;
//		}
//		double [] pi1 = new double[NUMBER_OF_DOCS];
//
//		while(count < MAX_NUMBER_OF_ITERATIONS){
//			count++;
//			for(int i = 0; i < NUMBER_OF_DOCS; i++){
//				pi1[i] = 0;
//				for(int j = 0; j < NUMBER_OF_DOCS; j++){
//					double p = 0;
//					if(out[j] == 0){
//						p = 1.0/n;
//					} else if(link.get(j).get(i) == null) {
//						p = BORED/n;
//					} else if(link.get(j).get(i)){
//						p = BORED/n;
//						p += (1.0-BORED)/(double)out[j];
//					} else {
//						System.out.println("Something wrong?");
//					}
//					pi1[i] += pi0[j]*p;
//				}
//			}
//
//			double diff = 0;
//			for(int i = 0; i < NUMBER_OF_DOCS; i++){
//				diff += Math.abs(pi1[i]-pi0[i]);
//			}
//
//			System.out.println("Diff: " + diff + ", Epsilon: " + EPSILON);
//			if (diff < EPSILON){
//				System.out.println("\nConverged");
//				break;
//			}
//			pi0 = pi1.clone();
//		}
//
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//		sortAndPrint(pi1, path, totalTime);
//		System.out.println("Count: " + count); //16
//
//		exact = pi1;
//	}
//
//	//MC end-point with random start
//	void mc1(){
//		String filename = "mc1";
//		long startTime = System.currentTimeMillis();
//		double[] pi = new double[NUMBER_OF_DOCS];
//		List<Double> diffArray = new ArrayList<>();
//		List<Double> diffArrayWorst = new ArrayList<>();
//		List<Integer> walksArray = new ArrayList<>();
//		Random random = new Random();
//		int node;
//		int walks = 0;
//		while(walks < N) {
//			node = random.nextInt(NUMBER_OF_DOCS);
//			node = walk(node, random, false);
//			pi[node]++;
//			walks++;
//
//			//Check progress
//			if(walks % check == 0) {
//				diffArray.add(checkConverge(pi));
//				diffArrayWorst.add(checkConvergeWorst(pi));
//				walksArray.add(walks);
//			}
//		}
//
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//
//		normalize(pi);
//		sortAndPrint(pi, filename, totalTime);
//
//		printToFile(diffArray.toArray(), "results/diffArray1");
//		printToFile(diffArrayWorst.toArray(), "results/diffArrayWorst1");
//		printToFile(walksArray.toArray(), "results/walksArray1");
//
//	}
//
//	// MC end-point with cyclic start
//	void mc2(){
//		String filename = "mc2";
//		long startTime = System.currentTimeMillis();
//		//int N =  100 * NUMBER_OF_DOCS;
//		double[] pi = new double[NUMBER_OF_DOCS];
//
//		List<Double> diffArray = new ArrayList<>();
//		List<Double> diffArrayWorst = new ArrayList<>();
//		List<Integer> walksArray = new ArrayList<>();
//
//		Random random = new Random();
//		int walks = 0;
//		while(walks < N) { //m times
//			for (int n = 0; n < NUMBER_OF_DOCS; n++) {
//				int node = n;
//				node = walk(node, random, false);
//				pi[node]++;
//				walks++;
//
//				//Check progress
//				if(walks % check == 0) {
//					diffArray.add(checkConverge(pi));
//					diffArrayWorst.add(checkConvergeWorst(pi));
//					walksArray.add(walks);
//				}
//			}
//		}
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//		normalize(pi);
//		sortAndPrint(pi, filename, totalTime);
//		printToFile(diffArray.toArray(), "results/diffArray2");
//		printToFile(diffArrayWorst.toArray(), "results/diffArrayWorst2");
//		printToFile(walksArray.toArray(), "results/walksArray2");
//
//	}
//
//	//MC complete path
//	void mc3(){
//		String filename = "mc3";
//
//		//int N =  100 * NUMBER_OF_DOCS;
//		long startTime = System.currentTimeMillis();
//		double[] pi = new double[NUMBER_OF_DOCS];
//
//		List<Double> diffArray = new ArrayList<>();
//		List<Double> diffArrayWorst = new ArrayList<>();
//		List<Integer> walksArray = new ArrayList<>();
//
//		Random random = new Random();
//		int walks = 0;
//		while(walks < N){ //m times
//			for(int n = 0; n < NUMBER_OF_DOCS; n++){ //Cycles the starting point. (Each document)
//				int node = n;
//				pi[node]++;
//				while(random.nextDouble() > BORED) {
//					node = walk(node, random, true);
//					pi[node]++;
//				}
//				walks++;
//
//				//Check progress
//				if(walks % check == 0) {
//					diffArray.add(checkConverge(pi));
//					diffArrayWorst.add(checkConvergeWorst(pi));
//					walksArray.add(walks);
//				}
//			}
//		}
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//		normalize(pi);
//		sortAndPrint(pi, filename, totalTime);
//		printToFile(diffArray.toArray(), "results/diffArray3");
//		printToFile(diffArrayWorst.toArray(), "results/diffArrayWorst3");
//		printToFile(walksArray.toArray(), "results/walksArray3");
//	}
//
//	// MC complete path stopping at dangling nodes
//	void mc4(){
//		String filename = "mc4";
//		//int N =  100 * NUMBER_OF_DOCS;
//
//		long startTime = System.currentTimeMillis();
//		double[] pi = new double[NUMBER_OF_DOCS];
//
//		List<Double> diffArray = new ArrayList<>();
//		List<Double> diffArrayWorst = new ArrayList<>();
//		List<Integer> walksArray = new ArrayList<>();
//
//		Random random = new Random();
//		int walks= 0;
//		while(walks < N){ //m times
//			for(int n = 0; n < NUMBER_OF_DOCS; n++){ //Cycles the starting point. (Each document)
//				int node = n;
//				pi[node]++;
//				while(random.nextDouble() > BORED && out[node] != 0) {
//					node = walk(node, random, true);
//					pi[node]++;
//				}
//				walks++;
//
//				//Check progress
//				if(walks % check == 0) {
//					diffArray.add(checkConverge(pi));
//					diffArrayWorst.add(checkConvergeWorst(pi));
//					walksArray.add(walks);
//				}
//			}
//		}
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//
//		normalize(pi);
//		sortAndPrint(pi, filename, totalTime);
//		printToFile(diffArray.toArray(), "results/diffArray4");
//		printToFile(diffArrayWorst.toArray(), "results/diffArrayWorst4");
//		printToFile(walksArray.toArray(), "results/walksArray4");
//
//
//	}
//
//	//MC complete path with random start
//	void mc5(){
//		String filename = "mc5";
//		long startTime = System.currentTimeMillis();
//		double[] pi = new double[NUMBER_OF_DOCS];
//
//		List<Double> diffArray = new ArrayList<>();
//		List<Double> diffArrayWorst = new ArrayList<>();
//		List<Integer> walksArray = new ArrayList<>();
//
//		int walks = 0;
//		Random random = new Random();
//		while(walks < N) {
//			int node = random.nextInt(NUMBER_OF_DOCS);//Starts the walk at a random document.
//			pi[node]++;
//			while(random.nextDouble() > BORED && out[node] != 0) {
//				node = walk(node, random, true);
//				pi[node]++;
//			}
//			walks++;
//
//			//Check progress
//			if(walks % check == 0) {
//				diffArray.add(checkConverge(pi));
//				diffArrayWorst.add(checkConvergeWorst(pi));
//				walksArray.add(walks);
//			}
//		}
//
//		long endTime   = System.currentTimeMillis();
//		long totalTime = endTime - startTime;
//
//		normalize(pi);
//		sortAndPrint(pi, filename, totalTime);
//		printToFile(diffArray.toArray(), "results/diffArray5");
//		printToFile(diffArrayWorst.toArray(), "results/diffArrayWorst5");
//		printToFile(walksArray.toArray(), "results/walksArray5");
//	}
//
//	void normalize(double[] pi){
//		double sum = 0;
//		for(int i = 0; i < NUMBER_OF_DOCS; i++){
//			sum += pi[i];
//		}
//		for(int i = 0; i < NUMBER_OF_DOCS; i++){
//			pi[i] /= sum;
//		}
//	}
//
//	int walk(int node, Random random, boolean complete) {
//		float chance = random.nextFloat();
//		if (out[node] == 0) {
//			node = random.nextInt(NUMBER_OF_DOCS);
//		} else{
//			int linkNumber = random.nextInt(out[node]);
//			Enumeration<Integer> links = link.get(node).keys();
//			for (int i = 0; i < linkNumber; i++) {
//				links.nextElement();
//			}
//			node = links.nextElement();
//		}
//		if(complete || chance < BORED){
//			return node;
//		} else{
//			return walk(node, random, complete);
//		}
//	}
//
//	double checkConverge(double[] pi){
//		double diff = 0;
//		double[] temp = pi.clone();
//		normalize(temp);
//		for(int i = 0; i < 50; i++){
//			int docID = exactResults.get(i).getDocNumber();
//			double tempPR = temp[docID];
//			double exactPR = exact[docID];
//			diff += Math.pow( (tempPR - exactPR) , 2);
//		}
//		return diff;
//	}
//
//	double checkConvergeWorst(double[] pi){
//		double diff = 0;
//		double[] temp = pi.clone();
//		normalize(temp);
//		for(int i = NUMBER_OF_DOCS-50; i < NUMBER_OF_DOCS; i++){
//			diff += Math.pow( (temp[exactResults.get(i).getIndex()] - exactResults.get(i).getPagerank()) , 2);
//		}
//		return diff;
//	}
//
//	void sortAndPrint(double[] pi, String filename, Long time){
//		String path = "results/" + filename;
//		System.out.println("Took " + time/1000 + " seconds.");
//		normalize(pi);
//		saveArray(pi, path + ".ser");
//		ArrayList<Pair> res = sort(pi);
//		printToFile(res, path + ".txt");
//	}
//
//	void printToFile(ArrayList<Pair> res, String path){
//		PrintStream output = null;
//		try {
//			output = new PrintStream(path);
//			PrintStream temp = System.out;
//			System.setOut(output);
//			for (int i = 0; i < 50; i++) {
//				System.out.println(i + 1 + ". " + res.get(i));
//			}
//			System.setOut(temp);
//
//			System.out.println(path + " saved");
//		} catch(FileNotFoundException e){
//
//		}finally {
//			try {output.close();} catch (Exception ex) {/*ignore*/}
//		}
//	}
//
//	void printToFile(Object[] res, String path){
//		PrintStream output = null;
//		try {
//			output = new PrintStream(path);
//			PrintStream temp = System.out;
//			System.setOut(output);
//			//System.out.print("[");
//			for (int i = 0; i < res.length; i++) {
//				System.out.print(res[i] + ",");
//			}
//			//System.out.print("];");
//			System.setOut(temp);
//
//			System.out.println(path + " saved");
//		} catch(FileNotFoundException e){
//
//		}finally {
//			try {output.close();} catch (Exception ex) {/*ignore*/}
//		}
//	}
//
//	private class Pair implements Comparable<Pair>{
//		private String docNumb;
//		private double pagerank;
//		private int index;
//		public Pair(String key, double value, int i){
//			docNumb = key;
//			pagerank = value;
//			index = i;
//		}
//
//		public double getPagerank(){
//			return pagerank;
//		}
//
//		public int getDocNumber(){
//			return docNumber.get(docNumb);
//		}
//
//		public int getIndex(){
//			return index;
//		}
//
//		public int compareTo(Pair o){
//			if (this.pagerank == o.getPagerank()) return 0;
//			if (this.pagerank < o.getPagerank()) return 1;
//			return -1;
//		}
//
//		public String toString(){
//			return String.format("%5s %f", docNumb, pagerank);
//		}
//	}
//
//
//	ArrayList<Pair> sort(double[] pi){
//		ArrayList<Pair> res = new ArrayList<Pair>();
//		for(int i = 0; i < NUMBER_OF_DOCS; i++){
//			res.add(new Pair(docName[i], pi[i], i));
//		}
//		Collections.sort(res);
//		return res;
//	}
//
//	void saveArray(double[] pi, String path){
//		ObjectOutputStream ops = null;
//		try {
//			ops = new ObjectOutputStream(
//					new FileOutputStream("results/" + path + ".ser")
//			);
//			ops.writeObject(pi);
//		} catch(IOException e) {
//			// report
//		} finally{
//			try {
//				ops.flush();
//				ops.close();
//			} catch (Exception ex) {/*ignore*/}
//		}
//	}
//
//	double[] readArray(String path){
//		ObjectInputStream in = null;
//		double[] array = null;
//		try{
//			in = new ObjectInputStream(new FileInputStream("results/" + path + ".ser"));
//			array = (double[]) in.readObject();
//
//		}catch(IOException e) {
//			// report
//		}catch(ClassNotFoundException fi){
//			// report
//		}
//		finally{
//			try {
//				in.close();
//			} catch (Exception ex) {/*ignore*/}
//		}
//		return array;
//	}
//
//
//    /* --------------------------------------------- */
//
//
//    public static void main( String[] args ) {
//		//java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt
//
//		if ( args.length != 1 ) {
//	    System.err.println( "Please give the name of the link file" );
//	}
//	else {
//	    new PageRank( args[0] );
//	}
//
//    }
//}
