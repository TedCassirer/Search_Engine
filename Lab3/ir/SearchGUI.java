/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */

//java -Xmx1024m -cp .:~/Desktop/Skola/DD2476/Lab1 ir.SearchGUI -d ~/Desktop/Skola/DD2476/Lab1/davisWiki/

package ir;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 *   A graphical interface to the information retrieval system.
 */
//java -Xmx1024m -cp .:~/Desktop/Skola/DD2476/Lab1 ir.SearchGUI -d ~/Desktop/Skola/DD2476/Lab1/davisWiki/
	//java -Xmx1024m -cp .:~/Desktop/Lab1 ir.SearchGUI -d ~/Desktop/Lab1/davisWiki
// ~/Desktop/Skolan/Lab1/out/production/Lab1 $ java -Xmx24m -cp .: ir.SearchGUI -d ~/Desktop/Skolan/davisWiki


public class SearchGUI extends JFrame {

	/**
	 * The indexer creating the search index.
	 */
	Indexer indexer = new Indexer();

	/**
	 * The query posed by the user, used in search() and relevanceFeedbackSearch()
	 */
	private Query uniQuery;
	private BiWordQuery biQuery;
	/**
	 * The returned documents, used in search() and relevanceFeedbackSearch()
	 */
	private PostingsList results;

	/**
	 * Directories that should be indexed.
	 */
	LinkedList<String> dirNames = new LinkedList<String>();

	/**
	 * The query type (either intersection, phrase, or ranked).
	 */
	int queryType = Index.INTERSECTION_QUERY;

	/**
	 * The index type (either entirely in memory or partly on disk).
	 */
	int indexType = Index.HASHED_INDEX;

	/**
	 * The ranking type (either tf-idf, pagerank, or combination).
	 */
	int rankingType = Index.TF_IDF;

	/**
	 * The word structure type (either unigram, bigram, or subphrase).
	 */
	int structureType = Index.UNIGRAM;

	/**
	 * Lock to prevent simultaneous access to the index.
	 */
	Object indexLock = new Object();

	/**
	 * Directory from which the code is compiled and run.
	 */
	public static final String homeDir = "/home/ted-cassirer/Desktop/Skolan/Lab2";


	/*
     *   The nice logotype
     *   Generated at http://neswork.com/logo-generator/google-font
     */
	static final String LOGOPIC = homeDir + "/IRfifteen.jpg";
	static final String BLANKPIC = homeDir + "/pics/blank.jpg";


	/*
     *   Common GUI resources
     */
	public JTextField queryWindow = new JTextField("", 28);
	public JTextArea resultWindow = new JTextArea("", 23, 35);
	private JScrollPane resultPane = new JScrollPane(resultWindow);
	private Font queryFont = new Font("Arial", Font.BOLD, 24);
	private Font resultFont = new Font("Arial", Font.BOLD, 16);
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	JMenu optionsMenu = new JMenu("Search options");
	JMenu rankingMenu = new JMenu("Ranking score");
	JMenu structureMenu = new JMenu("Text structure");
	JMenuItem saveItem = new JMenuItem("Save index and exit");
	JMenuItem indexItem = new JMenuItem("Index files");
	JMenuItem loadIndexItem = new JMenuItem("Load saved index");
	JMenuItem quitItem = new JMenuItem("Quit");
	JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem("Intersection query");
	JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem("Phrase query");
	JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem("Ranked retrieval");
	JRadioButtonMenuItem tfidfItem = new JRadioButtonMenuItem("tf-idf");
	JRadioButtonMenuItem pagerankItem = new JRadioButtonMenuItem("PageRank");
	JRadioButtonMenuItem combinationItem = new JRadioButtonMenuItem("Combination");
	JRadioButtonMenuItem unigramItem = new JRadioButtonMenuItem("Unigram");
	JRadioButtonMenuItem bigramItem = new JRadioButtonMenuItem("Bigram");
	JRadioButtonMenuItem subphraseItem = new JRadioButtonMenuItem("Subphrase");
	ButtonGroup queries = new ButtonGroup();
	ButtonGroup ranking = new ButtonGroup();
	ButtonGroup structure = new ButtonGroup();
	public JPanel feedbackBar = new JPanel();
	JCheckBox[] feedbackButton = new JCheckBox[10];
	JToggleButton feedbackExecutor = new JToggleButton("New search");


    /* ----------------------------------------------- */


	/*
     *   Create the GUI.
     */
	private void createGUI() {
		// GUI definition
		setSize(600, 650);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		getContentPane().add(p, BorderLayout.CENTER);
		// Top menu
		menuBar.add(fileMenu);
		menuBar.add(optionsMenu);
		menuBar.add(rankingMenu);
		menuBar.add(structureMenu);
		fileMenu.add(saveItem);
		fileMenu.add(indexItem);
		fileMenu.add(loadIndexItem);
		fileMenu.add(quitItem);
		optionsMenu.add(intersectionItem);
		optionsMenu.add(phraseItem);
		optionsMenu.add(rankedItem);
		rankingMenu.add(tfidfItem);
		rankingMenu.add(pagerankItem);
		rankingMenu.add(combinationItem);
		structureMenu.add(unigramItem);
		structureMenu.add(bigramItem);
		structureMenu.add(subphraseItem);
		queries.add(intersectionItem);
		queries.add(phraseItem);
		queries.add(rankedItem);
		ranking.add(tfidfItem);
		ranking.add(pagerankItem);
		ranking.add(combinationItem);
		structure.add(unigramItem);
		structure.add(bigramItem);
		structure.add(subphraseItem);
		intersectionItem.setSelected(true);
		tfidfItem.setSelected(true);
		unigramItem.setSelected(true);
		p.add(menuBar);
		// Logo
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.add(new JLabel(new ImageIcon(LOGOPIC)));
		p.add(p1);
		JPanel p3 = new JPanel();
		// Search box
		p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
		p3.add(new JLabel(new ImageIcon(BLANKPIC)));
		p3.add(queryWindow);
		queryWindow.setFont(queryFont);
		p3.add(new JLabel(new ImageIcon(BLANKPIC)));
		p.add(p3);
		// Display area for search results
		p.add(resultPane);
		resultWindow.setFont(resultFont);
		// Relevance feedback
		for (int i = 0; i < 10; i++) {
			feedbackButton[i] = new JCheckBox(i + "");
			feedbackBar.add(feedbackButton[i]);
		}
		feedbackBar.add(feedbackExecutor);
		p.add(feedbackBar);
		// Show the interface
		setVisible(true);

		Action search = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Normalize the search string and turn it into a Query
				String queryString = SimpleTokenizer.normalize(queryWindow.getText());
				// Search and print results. Access to the index is synchronized since
				// we don't want to search at the same time we're indexing new files
				// (this might corrupt the index).
				synchronized (indexLock) {
					switch (structureType){
						case Index.UNIGRAM :
							uniQuery = new Query(queryString);
							results = indexer.index.search(uniQuery, queryType, rankingType, structureType);
							break;
						case Index.BIGRAM :
							biQuery = new BiWordQuery(queryString);
							results = indexer.biWordIndex.search(biQuery, queryType, rankingType, structureType);
							break;
						case Index.SUBPHRASE :
							double uniGramWeight = 1.0;
							double biGramWeight = 10.0;
							biQuery = new BiWordQuery(queryString);
							results = indexer.biWordIndex.search(biQuery, queryType, rankingType, Index.BIGRAM);
							if(results.size() < 10){
								System.out.println("BiWord index result too few. Performing UniWord index search... ");
								uniQuery = new Query(queryString);
								PostingsList uni = indexer.index.search(uniQuery, queryType, rankingType, Index.UNIGRAM);

								for(Iterator<PostingsEntry> it = uni.getIterator(); it.hasNext(); ){
									PostingsEntry e1 = it.next();
									PostingsEntry e2 = results.get(e1);
									if(e2 != null){
										e1.score = biGramWeight * e2.score + uniGramWeight * e1.score;
									}
								}
								Collections.sort(uni.getList());
								results = uni;
							}
							break;
					}

				}
				StringBuffer buf = new StringBuffer();
				if (results != null) {
					buf.append("\nFound " + results.size() + " matching document(s)\n\n");
					for (int i = 0; i < results.size(); i++) {
						buf.append(" " + i + ". ");
						String filename = indexer.index.docIDs.get("" + results.get(i).docID);
						if (filename == null) {
							buf.append("" + results.get(i).docID);
						} else {
							filename = filename.substring(44, filename.lastIndexOf("."));
							buf.append(filename);
						}

						if (queryType == Index.RANKED_QUERY) {
							buf.append("   " + String.format("%.5f", results.get(i).score));
						}
						buf.append("\n");
					}

					printToFile(buf.toString(), queryString);
				} else {
					buf.append("\nFound 0 matching document(s)\n\n");
				}
				resultWindow.setText(buf.toString());
				resultWindow.setCaretPosition(0);
			}
		};
		queryWindow.registerKeyboardAction(search,
				"",
				KeyStroke.getKeyStroke("ENTER"),
				JComponent.WHEN_FOCUSED);

		Action relevanceFeedbackSearch = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Long startTime = System.nanoTime();
				System.out.println("Starting new relevance feedback search...");
				// Check that a ranked search has been made prior to the relevance feedback
				StringBuffer buf = new StringBuffer();
				if ((results != null) && (queryType == Index.RANKED_QUERY)) {
					// Read user relevance feedback selections
					boolean[] docIsRelevant = {false, false, false, false, false, false, false, false, false, false};
					for (int i = 0; i < 10; i++) {
						docIsRelevant[i] = feedbackButton[i].isSelected();
					}
					switch (structureType){
						case Index.UNIGRAM :
							// Expand the current search query with the documents marked as relevant
							uniQuery.relevanceFeedback(results, docIsRelevant, indexer);

							// Perform a new search with the weighted and expanded query. Access to the index is
							// synchronized since we don't want to search at the same time we're indexing new files
							// (this might corrupt the index).
							synchronized (indexLock) {
								results = indexer.index.search(uniQuery, queryType, rankingType, structureType);
							}
							break;
						case Index.BIGRAM:
							// Expand the current search query with the documents marked as relevant
							biQuery.relevanceFeedback(results, docIsRelevant, indexer);

							// Perform a new search with the weighted and expanded query. Access to the index is
							// synchronized since we don't want to search at the same time we're indexing new files
							// (this might corrupt the index).
							synchronized (indexLock) {
								results = indexer.biWordIndex.search(biQuery, queryType, rankingType, structureType);
							}
							break;
					}

					buf.append("\nSearch after relevance feedback:\n");
					buf.append("\nFound " + results.size() + " matching document(s)\n\n");
					for (int i = 0; i < results.size(); i++) {
						buf.append(" " + i + ". ");
						String filename = indexer.index.docIDs.get("" + results.get(i).docID);
						if (filename == null) {
							buf.append("" + results.get(i).docID);
						} else {
							filename = filename.substring(44, filename.lastIndexOf("."));
							buf.append(filename);
						}
						buf.append("   " + String.format("%.5f", results.get(i).score) + "\n");

					}
				} else {
					buf.append("\nThere was no returned ranked list to give feedback on.\n\n");
				}
				resultWindow.setText(buf.toString());
				resultWindow.setCaretPosition(0);
				double totalTime = (System.nanoTime() - startTime) / 1E9;
				System.out.println("Time to execute: " + totalTime + "\n");
			}
		};
		feedbackExecutor.addActionListener(relevanceFeedbackSearch);

		Action saveAndQuit = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				resultWindow.setText("\n  Saving index...");
				indexer.saveIndex();
				//System.exit( 0 );
			}
		};
		saveItem.addActionListener(saveAndQuit);

		Action indexFiles = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				index();
			}
		};
		indexItem.addActionListener(indexFiles);



		Action quit = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		quitItem.addActionListener(quit);


		Action setIntersectionQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.INTERSECTION_QUERY;
			}
		};
		intersectionItem.addActionListener(setIntersectionQuery);

		Action setPhraseQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.PHRASE_QUERY;
			}
		};
		phraseItem.addActionListener(setPhraseQuery);

		Action setRankedQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.RANKED_QUERY;
			}
		};
		rankedItem.addActionListener(setRankedQuery);

		Action setTfidfRanking = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				rankingType = Index.TF_IDF;
			}
		};
		tfidfItem.addActionListener(setTfidfRanking);

		Action setPagerankRanking = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				rankingType = Index.PAGERANK;
			}
		};
		pagerankItem.addActionListener(setPagerankRanking);

		Action setCombinationRanking = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				rankingType = Index.COMBINATION;
			}
		};
		combinationItem.addActionListener(setCombinationRanking);

		Action setUnigramStructure = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				structureType = Index.UNIGRAM;
			}
		};
		unigramItem.addActionListener(setUnigramStructure);

		Action setBigramStructure = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				structureType = Index.BIGRAM;
			}
		};
		bigramItem.addActionListener(setBigramStructure);

		Action setSubphraseStructure = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				structureType = Index.SUBPHRASE;
			}
		};
		subphraseItem.addActionListener(setSubphraseStructure);
	}
	
 
    /* ----------------------------------------------- */


	/**
	 * Calls the indexer to index the chosen directory structure.
	 * Access to the index is synchronized since we don't want to
	 * search at the same time we're indexing new files (this might
	 * corrupt the index).
	 */
	private void index() {
		synchronized (indexLock) {
			resultWindow.setText("\n  Indexing, please wait...");
			for (int i = 0; i < dirNames.size(); i++) {
				File dokDir = new File(dirNames.get(i));
				indexer.processFiles(dokDir);
			}
		}
		//indexer.calculateWeights();
		resultWindow.setText("\n  Done!");
	}



    /* ----------------------------------------------- */


	/**
	 * Decodes the command line arguments.
	 */
	private void decodeArgs(String[] args) {
		int i = 0, j = 0;
		System.out.println(args);
		while (i < args.length) {
			if ("-d".equals(args[i])) {
				i++;
				if (i < args.length) {
					dirNames.add(args[i++]);
				}
			} else {
				System.err.println("Unknown option: " + args[i]);
				break;
			}
		}
	}

	void printToFile(String text, String path) {
		String[] lines = text.split("\n");
		if(lines.length > 3) {
			lines = Arrays.copyOfRange(lines, 3, lines.length);
			PrintStream output = null;
			path = "searches/" + path + ".txt";
			try {
				output = new PrintStream(path);
				PrintStream temp = System.out;
				System.setOut(output);
				for (String line : lines) {
					line = line.substring(line.indexOf(".") + 1);
					System.out.println(line);
				}
				System.setOut(temp);
			} catch (FileNotFoundException e) {

			} finally {
				try {
					output.close();
				} catch (Exception ex) {/*ignore*/}
			}
		}
	}

    /* ----------------------------------------------- */


	public static void main(String[] args) {
		SearchGUI s = new SearchGUI();
		s.createGUI();
		s.decodeArgs(args);
		s.index();
	}
}