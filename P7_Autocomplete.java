import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class P7_Autocomplete {

    // -------------------------------------------------------
    // TrieNode class - each node holds a HashMap of children
    // -------------------------------------------------------
    static class TrieNode {
        // HashMap<character, childNode> for O(1) child access
        HashMap<Character, TrieNode> children;
        boolean isEndOfWord;
        String  fullWord;
        int     frequency;

        TrieNode() {
            children    = new HashMap<>();
            isEndOfWord = false;
            fullWord    = null;
            frequency   = 0;
        }
    }

    // -------------------------------------------------------
    // Suggestion class - stores a query with its frequency
    // -------------------------------------------------------
    static class Suggestion implements Comparable<Suggestion> {
        String query;
        int    frequency;

        Suggestion(String query, int frequency) {
            this.query     = query;
            this.frequency = frequency;
        }

        @Override
        public int compareTo(Suggestion other) {
            // Sort by frequency descending
            return Integer.compare(other.frequency, this.frequency);
        }
    }

    // Trie root node
    private TrieNode root;

    // HashMap<query, frequency> for global frequency stats
    private HashMap<String, Integer> queryFrequency;

    // Cache for popular prefix results
    private HashMap<String, List<Suggestion>> prefixCache;

    // Performance metrics
    private int totalSearches;
    private int cacheHits;
    private long totalSearchTime;

    /**
     * Constructor - initializes Trie + HashMap hybrid
     */
    public P7_Autocomplete() {
        root           = new TrieNode();
        queryFrequency = new HashMap<>();
        prefixCache    = new HashMap<>();
        totalSearches  = 0;
        cacheHits      = 0;
        totalSearchTime = 0;

        // Pre-populate with common search queries
        addQuery("java tutorial",           1234567);
        addQuery("javascript",              987654);
        addQuery("java download",           456789);
        addQuery("java 21 features",        123456);
        addQuery("java spring boot",        345678);
        addQuery("python tutorial",         876543);
        addQuery("python download",         543210);
        addQuery("python machine learning", 234567);
        addQuery("programming languages",   432109);
        addQuery("programming tutorial",    321098);
        addQuery("data structures",         654321);
        addQuery("data science",            543219);
        addQuery("data analysis",           432198);
        addQuery("database design",         321987);
        addQuery("android development",     210876);
        addQuery("android studio",          198765);
    }

    /**
     * Insert a query into the Trie and update frequency.
     *
     * @param query     the search query
     * @param frequency initial frequency count
     */
    public void addQuery(String query, int frequency) {
        String normalized = query.toLowerCase().trim();

        // Update global frequency HashMap
        queryFrequency.put(normalized,
                queryFrequency.getOrDefault(normalized, 0) + frequency);

        // Insert into Trie character by character
        TrieNode current = root;
        for (char c : normalized.toCharArray()) {
            current.children.computeIfAbsent(c, k -> new TrieNode());
            current = current.children.get(c);
        }

        // Mark end of word
        current.isEndOfWord = true;
        current.fullWord    = normalized;
        current.frequency   = queryFrequency.get(normalized);

        // Invalidate prefix cache for this query
        prefixCache.clear();
    }

    /**
     * Record a new search and update frequency.
     *
     * @param query the query being searched
     */
    public void recordSearch(String query) {
        String normalized = query.toLowerCase().trim();
        addQuery(normalized, 1);
        totalSearches++;
        System.out.println("🔍 Recorded search: \"" + normalized
                + "\" (frequency: "
                + queryFrequency.getOrDefault(normalized, 1) + ")");
    }

    /**
     * Get autocomplete suggestions for a given prefix.
     * Returns top N suggestions sorted by frequency.
     *
     * @param prefix the typed prefix
     * @param topN   number of suggestions to return
     * @return list of suggestions sorted by frequency
     */
    public List<Suggestion> getSuggestions(String prefix, int topN) {
        long startTime = System.nanoTime();
        String normalized = prefix.toLowerCase().trim();

        // Check prefix cache first
        if (prefixCache.containsKey(normalized)) {
            cacheHits++;
            long elapsed = System.nanoTime() - startTime;
            totalSearchTime += elapsed;
            return prefixCache.get(normalized)
                    .subList(0, Math.min(topN,
                            prefixCache.get(normalized).size()));
        }

        // Navigate Trie to prefix endpoint
        TrieNode current = root;
        for (char c : normalized.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return new ArrayList<>(); // no matches
            }
            current = current.children.get(c);
        }

        // Collect all words under this prefix node
        List<Suggestion> suggestions = new ArrayList<>();
        collectWords(current, suggestions);

        // Sort by frequency descending
        Collections.sort(suggestions);

        // Cache the results
        prefixCache.put(normalized, suggestions);

        long elapsed = System.nanoTime() - startTime;
        totalSearchTime += elapsed;

        return suggestions.subList(0,
                Math.min(topN, suggestions.size()));
    }

    /**
     * Recursively collect all words from a Trie node.
     *
     * @param node        current Trie node
     * @param suggestions list to collect results into
     */
    private void collectWords(TrieNode node,
                              List<Suggestion> suggestions) {
        if (node.isEndOfWord) {
            suggestions.add(new Suggestion(
                    node.fullWord, node.frequency));
        }
        for (TrieNode child : node.children.values()) {
            collectWords(child, suggestions);
        }
    }

    /**
     * Suggest typo corrections using edit distance.
     * Finds queries within edit distance of 1.
     *
     * @param query the potentially misspelled query
     * @return list of correction suggestions
     */
    public List<String> suggestCorrections(String query) {
        String normalized = query.toLowerCase().trim();
        List<String> corrections = new ArrayList<>();

        for (String storedQuery : queryFrequency.keySet()) {
            if (editDistance(normalized, storedQuery) <= 1) {
                corrections.add(storedQuery);
            }
        }

        // Sort by frequency
        corrections.sort((a, b) ->
                queryFrequency.getOrDefault(b, 0)
                        - queryFrequency.getOrDefault(a, 0));

        return corrections.subList(0,
                Math.min(5, corrections.size()));
    }

    /**
     * Calculate edit distance between two strings.
     *
     * @param a first string
     * @param b second string
     * @return minimum edit distance
     */
    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1];
                else
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * Display performance statistics.
     */
    public void displayStats() {
        System.out.println("\n========== Autocomplete Stats ==========");
        System.out.println("Total Queries Indexed : " + queryFrequency.size());
        System.out.println("Total Searches        : " + totalSearches);
        System.out.println("Cache Hits            : " + cacheHits);
        System.out.println("Prefix Cache Size     : " + prefixCache.size());
        double avgTime = totalSearches > 0
                ? (totalSearchTime / totalSearches) : 0;
        System.out.printf("Avg Search Time       : %.0f ns%n", avgTime);
        System.out.println("=========================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P7_Autocomplete autocomplete = new P7_Autocomplete();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("     Autocomplete System for Search Engine");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Search (Get Suggestions)");
            System.out.println("2. Record New Search");
            System.out.println("3. Add Query to Index");
            System.out.println("4. Suggest Typo Corrections");
            System.out.println("5. Display Stats");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Type prefix (e.g. 'jav'): ");
                    String prefix = scanner.nextLine();
                    long start = System.nanoTime();
                    List<Suggestion> suggestions =
                            autocomplete.getSuggestions(prefix, 10);
                    long elapsed = System.nanoTime() - start;

                    if (suggestions.isEmpty()) {
                        System.out.println("No suggestions found.");
                    } else {
                        System.out.println("\n🔍 Suggestions for \""
                                + prefix + "\":");
                        int rank = 1;
                        for (Suggestion s : suggestions) {
                            System.out.printf("  %d. %-35s (%,d searches)%n",
                                    rank++, s.query, s.frequency);
                        }
                        System.out.println("  ⏱ Found in " + elapsed + " ns");
                    }
                    break;

                case 2:
                    System.out.print("Enter search query: ");
                    String query = scanner.nextLine();
                    autocomplete.recordSearch(query);
                    break;

                case 3:
                    System.out.print("Enter query to add: ");
                    String newQuery = scanner.nextLine();
                    System.out.print("Enter frequency: ");
                    int freq = scanner.nextInt();
                    scanner.nextLine();
                    autocomplete.addQuery(newQuery, freq);
                    System.out.println("✅ Added: \"" + newQuery + "\"");
                    break;

                case 4:
                    System.out.print("Enter possibly misspelled query: ");
                    String typo = scanner.nextLine();
                    List<String> corrections =
                            autocomplete.suggestCorrections(typo);
                    if (corrections.isEmpty()) {
                        System.out.println("No corrections found.");
                    } else {
                        System.out.println("\n💡 Did you mean:");
                        for (String c : corrections) {
                            System.out.println("  → " + c
                                    + " (" + autocomplete.queryFrequency
                                    .get(c) + " searches)");
                        }
                    }
                    break;

                case 5:
                    autocomplete.displayStats();
                    break;

                case 6:
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