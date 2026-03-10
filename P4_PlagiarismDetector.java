import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class P4_PlagiarismDetector {

    // -------------------------------------------------------
    // Document class - stores document metadata
    // -------------------------------------------------------
    static class Document {
        String id;
        String title;
        String content;
        List<String> ngrams;

        Document(String id, String title, String content) {
            this.id      = id;
            this.title   = title;
            this.content = content;
            this.ngrams  = new ArrayList<>();
        }
    }

    // -------------------------------------------------------
    // Result class - stores plagiarism check results
    // -------------------------------------------------------
    static class PlagiarismResult {
        String documentId;
        String documentTitle;
        int    matchingNgrams;
        double similarityPercent;

        PlagiarismResult(String id, String title,
                         int matching, double similarity) {
            this.documentId       = id;
            this.documentTitle    = title;
            this.matchingNgrams   = matching;
            this.similarityPercent = similarity;
        }
    }

    // N-gram size (5-grams for better accuracy)
    private static final int N = 5;

    // HashMap<ngram, Set<documentId>> for ngram → documents mapping
    private HashMap<String, Set<String>> ngramIndex;

    // HashMap<documentId, Document> for document storage
    private HashMap<String, Document> documentDatabase;

    /**
     * Constructor - initializes the plagiarism detection system
     */
    public P4_PlagiarismDetector() {
        ngramIndex        = new HashMap<>();
        documentDatabase  = new HashMap<>();

        // Pre-populate with sample database essays
        addDocument("essay_001", "Introduction to Java",
                "Java is a high level programming language Java was designed " +
                        "to be simple and object oriented Java runs on the JVM which " +
                        "makes it platform independent Java is widely used in enterprise " +
                        "applications and Android development");

        addDocument("essay_002", "Data Structures Overview",
                "Data structures are ways of organizing data in a computer " +
                        "so that it can be accessed and modified efficiently common " +
                        "data structures include arrays linked lists stacks queues " +
                        "trees and graphs each has different time and space complexity");

        addDocument("essay_003", "Object Oriented Programming",
                "Object oriented programming is a paradigm that uses objects " +
                        "and classes to structure software programs the four pillars " +
                        "of object oriented programming are encapsulation inheritance " +
                        "polymorphism and abstraction these concepts help in building " +
                        "modular and reusable code");

        addDocument("essay_004", "Hash Tables in Java",
                "Hash tables are data structures that store key value pairs " +
                        "they use a hash function to compute an index into an array " +
                        "of buckets from which the desired value can be found hash " +
                        "tables provide O(1) average time complexity for lookup " +
                        "insert and delete operations in Java HashMap implements this");
    }

    /**
     * Generate n-grams from a text string.
     * An n-gram is a contiguous sequence of N words.
     *
     * @param text the input text
     * @return list of n-grams
     */
    private List<String> generateNgrams(String text) {
        List<String> ngrams = new ArrayList<>();
        String[] words = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .split("\\s+");

        // Sliding window of size N
        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder ngram = new StringBuilder();
            for (int j = i; j < i + N; j++) {
                ngram.append(words[j]);
                if (j < i + N - 1) ngram.append(" ");
            }
            ngrams.add(ngram.toString());
        }
        return ngrams;
    }

    /**
     * Add a document to the database and index its n-grams.
     *
     * @param id      document ID
     * @param title   document title
     * @param content document content
     */
    public void addDocument(String id, String title, String content) {
        Document doc = new Document(id, title, content);
        doc.ngrams    = generateNgrams(content);

        // Store document
        documentDatabase.put(id, doc);

        // Index each n-gram → document mapping
        for (String ngram : doc.ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(id);
        }

        System.out.println("📄 Added: [" + id + "] \"" + title + "\""
                + " (" + doc.ngrams.size() + " n-grams extracted)");
    }

    /**
     * Analyze a new document for plagiarism against the database.
     * Finds matching n-grams and calculates similarity in O(n) time.
     *
     * @param content the document content to check
     * @return list of plagiarism results sorted by similarity
     */
    public List<PlagiarismResult> analyzeDocument(String content) {

        List<String> newNgrams = generateNgrams(content);
        System.out.println("\n🔍 Extracted " + newNgrams.size() + " n-grams from new document.");

        // Count matching n-grams per document
        // HashMap<documentId, matchCount>
        HashMap<String, Integer> matchCounts = new HashMap<>();

        for (String ngram : newNgrams) {
            if (ngramIndex.containsKey(ngram)) {
                Set<String> matchingDocs = ngramIndex.get(ngram);
                for (String docId : matchingDocs) {
                    matchCounts.put(docId,
                            matchCounts.getOrDefault(docId, 0) + 1);
                }
            }
        }

        // Build results list
        List<PlagiarismResult> results = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            String docId   = entry.getKey();
            int    matches = entry.getValue();
            double similarity = (matches * 100.0) / newNgrams.size();

            results.add(new PlagiarismResult(
                    docId,
                    documentDatabase.get(docId).title,
                    matches,
                    similarity
            ));
        }

        // Sort by similarity descending
        results.sort((a, b) ->
                Double.compare(b.similarityPercent, a.similarityPercent));

        return results;
    }

    /**
     * Display plagiarism analysis results.
     *
     * @param results list of plagiarism results
     */
    public void displayResults(List<PlagiarismResult> results) {
        System.out.println("\n========== Plagiarism Analysis Results ==========");

        if (results.isEmpty()) {
            System.out.println("✅ No matching documents found. Original content!");
            return;
        }

        System.out.printf("%-12s %-35s %-10s %-12s %s%n",
                "Doc ID", "Title", "Matches", "Similarity", "Verdict");
        System.out.println("------------------------------------------------------------------"
                + "----------");

        for (PlagiarismResult result : results) {
            String verdict;
            if (result.similarityPercent >= 50)
                verdict = "🚨 PLAGIARISM";
            else if (result.similarityPercent >= 20)
                verdict = "⚠️  SUSPICIOUS";
            else
                verdict = "✅ ACCEPTABLE";

            System.out.printf("%-12s %-35s %-10d %-12.1f %s%n",
                    result.documentId,
                    result.documentTitle,
                    result.matchingNgrams,
                    result.similarityPercent,
                    verdict);
        }
        System.out.println("=================================================");
    }

    /**
     * Display performance comparison: Hash vs Linear search.
     *
     * @param content document content to benchmark
     */
    public void benchmarkPerformance(String content) {
        List<String> ngrams = generateNgrams(content);

        // Hash-based lookup
        long startHash = System.nanoTime();
        for (String ngram : ngrams) {
            ngramIndex.containsKey(ngram);
        }
        long hashTime = System.nanoTime() - startHash;

        // Linear search simulation
        List<String> allNgrams = new ArrayList<>(ngramIndex.keySet());
        long startLinear = System.nanoTime();
        for (String ngram : ngrams) {
            for (String indexed : allNgrams) {
                if (indexed.equals(ngram)) break;
            }
        }
        long linearTime = System.nanoTime() - startLinear;

        System.out.println("\n========== Performance Benchmark ==========");
        System.out.println("N-grams checked : " + ngrams.size());
        System.out.println("Index size      : " + ngramIndex.size() + " entries");
        System.out.println("Hash lookup     : " + hashTime + " ns");
        System.out.println("Linear search   : " + linearTime + " ns");
        System.out.printf("Speedup         : %.1fx faster%n",
                (double) linearTime / hashTime);
        System.out.println("==========================================");
    }

    /**
     * Display database statistics.
     */
    public void displayStats() {
        System.out.println("\n========== Database Statistics ==========");
        System.out.println("Documents in DB  : " + documentDatabase.size());
        System.out.println("Unique N-grams   : " + ngramIndex.size());
        System.out.println("N-gram size (N)  : " + N);
        System.out.println("=========================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P4_PlagiarismDetector detector = new P4_PlagiarismDetector();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n==========================================");
        System.out.println("       Plagiarism Detection System");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Analyze Document for Plagiarism");
            System.out.println("2. Add Document to Database");
            System.out.println("3. Benchmark Hash vs Linear Search");
            System.out.println("4. Display Database Statistics");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("Enter document content to check:");
                    String content = scanner.nextLine();
                    List<PlagiarismResult> results =
                            detector.analyzeDocument(content);
                    detector.displayResults(results);
                    break;

                case 2:
                    System.out.print("Enter Document ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Enter Document Title: ");
                    String title = scanner.nextLine();
                    System.out.println("Enter Document Content:");
                    String docContent = scanner.nextLine();
                    detector.addDocument(id, title, docContent);
                    break;

                case 3:
                    System.out.println("Enter text to benchmark:");
                    String benchText = scanner.nextLine();
                    detector.benchmarkPerformance(benchText);
                    break;

                case 4:
                    detector.displayStats();
                    break;

                case 5:
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
        scanner.close();
    }
}