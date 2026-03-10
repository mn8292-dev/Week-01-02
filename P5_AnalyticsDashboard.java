
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class P5_AnalyticsDashboard {

    // -------------------------------------------------------
    // PageView Event class
    // -------------------------------------------------------
    static class PageViewEvent {
        String url;
        String userId;
        String source;  // google, facebook, direct, etc.
        long   timestamp;

        PageViewEvent(String url, String userId, String source) {
            this.url       = url;
            this.userId    = userId;
            this.source    = source;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // HashMap<pageUrl, visitCount> for page view counts
    private HashMap<String, Integer> pageViews;

    // HashMap<pageUrl, Set<userId>> for unique visitors per page
    private HashMap<String, HashSet<String>> uniqueVisitors;

    // HashMap<source, count> for traffic source breakdown
    private HashMap<String, Integer> trafficSources;

    // HashMap<userId, visitCount> for user activity tracking
    private HashMap<String, Integer> userActivity;

    // Total events processed
    private int totalEvents;

    // Dashboard last updated time
    private long lastUpdated;

    /**
     * Constructor - initializes all analytics hash tables
     */
    public P5_AnalyticsDashboard() {
        pageViews      = new HashMap<>();
        uniqueVisitors = new HashMap<>();
        trafficSources = new HashMap<>();
        userActivity   = new HashMap<>();
        totalEvents    = 0;
        lastUpdated    = System.currentTimeMillis();
    }

    /**
     * Process a single page view event in real-time.
     * Updates all hash tables in O(1) time.
     *
     * @param event the page view event to process
     */
    public void processEvent(PageViewEvent event) {

        // 1. Update page view count
        pageViews.put(event.url,
                pageViews.getOrDefault(event.url, 0) + 1);

        // 2. Update unique visitors for this page
        uniqueVisitors.computeIfAbsent(event.url,
                k -> new HashSet<>()).add(event.userId);

        // 3. Update traffic source count
        trafficSources.put(event.source,
                trafficSources.getOrDefault(event.source, 0) + 1);

        // 4. Update user activity
        userActivity.put(event.userId,
                userActivity.getOrDefault(event.userId, 0) + 1);

        totalEvents++;
    }

    /**
     * Process multiple events in batch.
     *
     * @param events list of page view events
     */
    public void processBatch(List<PageViewEvent> events) {
        for (PageViewEvent event : events) {
            processEvent(event);
        }
        System.out.println("✅ Processed " + events.size()
                + " events. Total: " + totalEvents);
    }

    /**
     * Get top N most visited pages.
     * Uses sorting on HashMap entries.
     *
     * @param n number of top pages to return
     * @return sorted list of top pages
     */
    public List<Map.Entry<String, Integer>> getTopPages(int n) {
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(pageViews.entrySet());

        // Sort by visit count descending
        entries.sort((a, b) -> b.getValue() - a.getValue());

        return entries.subList(0, Math.min(n, entries.size()));
    }

    /**
     * Get traffic source breakdown as percentages.
     *
     * @return LinkedHashMap sorted by count descending
     */
    public LinkedHashMap<String, Double> getTrafficSourceBreakdown() {
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(trafficSources.entrySet());

        // Sort by count descending
        entries.sort((a, b) -> b.getValue() - a.getValue());

        LinkedHashMap<String, Double> breakdown = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            double percent = (entry.getValue() * 100.0) / totalEvents;
            breakdown.put(entry.getKey(), percent);
        }
        return breakdown;
    }

    /**
     * Get most active users.
     *
     * @param n number of top users
     * @return sorted list of top users
     */
    public List<Map.Entry<String, Integer>> getTopUsers(int n) {
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(userActivity.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        return entries.subList(0, Math.min(n, entries.size()));
    }

    /**
     * Display full real-time analytics dashboard.
     */
    public void displayDashboard() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       Real-Time Analytics Dashboard              ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("Total Events Processed : " + totalEvents);
        System.out.println("Unique Pages Tracked   : " + pageViews.size());
        System.out.println("Unique Users Tracked   : " + userActivity.size());

        // Top 10 Pages
        System.out.println("\n📊 Top Pages:");
        System.out.printf("  %-5s %-35s %-10s %-10s%n",
                "Rank", "URL", "Views", "Unique");
        System.out.println("  -----------------------------------------------"
                + "------");

        List<Map.Entry<String, Integer>> topPages = getTopPages(10);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPages) {
            int unique = uniqueVisitors.containsKey(entry.getKey())
                    ? uniqueVisitors.get(entry.getKey()).size() : 0;
            System.out.printf("  %-5d %-35s %-10d %-10d%n",
                    rank++, entry.getKey(), entry.getValue(), unique);
        }

        // Traffic Sources
        System.out.println("\n🌐 Traffic Sources:");
        System.out.printf("  %-20s %-10s %-10s%n",
                "Source", "Visits", "Percentage");
        System.out.println("  ------------------------------------------");

        LinkedHashMap<String, Double> sources = getTrafficSourceBreakdown();
        for (Map.Entry<String, Double> entry : sources.entrySet()) {
            int count = trafficSources.get(entry.getKey());
            System.out.printf("  %-20s %-10d %.1f%%%n",
                    entry.getKey(), count, entry.getValue());
        }

        // Top Users
        System.out.println("\n👤 Most Active Users:");
        System.out.printf("  %-20s %-10s%n", "User ID", "Visits");
        System.out.println("  ------------------------------");

        List<Map.Entry<String, Integer>> topUsers = getTopUsers(5);
        for (Map.Entry<String, Integer> entry : topUsers) {
            System.out.printf("  %-20s %-10d%n",
                    entry.getKey(), entry.getValue());
        }

        System.out.println("\n╚══════════════════════════════════════════════════╝");
    }

    /**
     * Simulate a burst of traffic events for testing.
     */
    public void simulateTraffic() {
        System.out.println("🚀 Simulating traffic burst...");

        String[] urls = {
                "/article/breaking-news",
                "/sports/championship",
                "/tech/ai-update",
                "/business/stocks",
                "/entertainment/movies"
        };
        String[] sources  = {"google", "facebook", "direct", "twitter", "other"};
        String[] userIds  = {
                "user_001", "user_002", "user_003",
                "user_004", "user_005", "user_006",
                "user_007", "user_008", "user_009", "user_010"
        };

        List<PageViewEvent> events = new ArrayList<>();
        java.util.Random random = new java.util.Random();

        // Generate 100 random events
        for (int i = 0; i < 100; i++) {
            String url    = urls[random.nextInt(urls.length)];
            String source = sources[random.nextInt(sources.length)];
            String userId = userIds[random.nextInt(userIds.length)];
            events.add(new PageViewEvent(url, userId, source));
        }

        processBatch(events);
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P5_AnalyticsDashboard dashboard = new P5_AnalyticsDashboard();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("  Real-Time Analytics Dashboard");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Process Single Event");
            System.out.println("2. Simulate Traffic Burst (100 events)");
            System.out.println("3. View Dashboard");
            System.out.println("4. View Top Pages");
            System.out.println("5. View Traffic Sources");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter URL (e.g. /article/news): ");
                    String url = scanner.nextLine();
                    System.out.print("Enter User ID: ");
                    String userId = scanner.nextLine();
                    System.out.print("Enter Source (google/facebook/direct): ");
                    String source = scanner.nextLine();
                    dashboard.processEvent(
                            new PageViewEvent(url, userId, source));
                    System.out.println("✅ Event processed!");
                    break;

                case 2:
                    dashboard.simulateTraffic();
                    break;

                case 3:
                    dashboard.displayDashboard();
                    break;

                case 4:
                    System.out.print("How many top pages? ");
                    int n = scanner.nextInt();
                    scanner.nextLine();
                    List<Map.Entry<String, Integer>> pages =
                            dashboard.getTopPages(n);
                    System.out.println("\n📊 Top " + n + " Pages:");
                    int r = 1;
                    for (Map.Entry<String, Integer> e : pages) {
                        System.out.println("  " + r++ + ". "
                                + e.getKey() + " - " + e.getValue() + " views");
                    }
                    break;

                case 5:
                    System.out.println("\n🌐 Traffic Sources:");
                    LinkedHashMap<String, Double> sources =
                            dashboard.getTrafficSourceBreakdown();
                    for (Map.Entry<String, Double> e : sources.entrySet()) {
                        System.out.printf("  %-15s → %.1f%%%n",
                                e.getKey(), e.getValue());
                    }
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