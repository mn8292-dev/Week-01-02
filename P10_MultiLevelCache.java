import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class P10_MultiLevelCache {

    // -------------------------------------------------------
    // VideoData class - represents a video object
    // -------------------------------------------------------
    static class VideoData {
        String videoId;
        String title;
        String resolution;
        long   sizeBytes;
        int    accessCount;

        VideoData(String videoId, String title,
                  String resolution, long sizeBytes) {
            this.videoId     = videoId;
            this.title       = title;
            this.resolution  = resolution;
            this.sizeBytes   = sizeBytes;
            this.accessCount = 0;
        }

        @Override
        public String toString() {
            return "[" + videoId + "] \"" + title
                    + "\" (" + resolution + ", "
                    + (sizeBytes / 1024 / 1024) + " MB)";
        }
    }

    // -------------------------------------------------------
    // CacheLevel class - generic LRU cache using LinkedHashMap
    // -------------------------------------------------------
    static class CacheLevel {
        String  name;
        int     maxSize;
        long    accessTimeMs;   // simulated access time
        int     hits;
        int     misses;

        // LRU LinkedHashMap (access-order = true)
        LinkedHashMap<String, VideoData> cache;

        // Access count threshold for promotion
        int promotionThreshold;

        CacheLevel(String name, int maxSize,
                   long accessTimeMs, int promotionThreshold) {
            this.name               = name;
            this.maxSize            = maxSize;
            this.accessTimeMs       = accessTimeMs;
            this.promotionThreshold = promotionThreshold;
            this.hits               = 0;
            this.misses             = 0;

            // Access-order LinkedHashMap for LRU eviction
            this.cache = new LinkedHashMap<String, VideoData>(
                    maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, VideoData> eldest) {
                    if (size() > maxSize) {
                        System.out.println("  ♻️  LRU Evicted from "
                                + name + ": " + eldest.getKey());
                        return true;
                    }
                    return false;
                }
            };
        }

        boolean contains(String videoId) {
            return cache.containsKey(videoId);
        }

        VideoData get(String videoId) {
            return cache.get(videoId);
        }

        void put(String videoId, VideoData data) {
            cache.put(videoId, data);
        }

        void remove(String videoId) {
            cache.remove(videoId);
        }

        double getHitRate() {
            int total = hits + misses;
            return total > 0 ? (hits * 100.0 / total) : 0;
        }

        int size() {
            return cache.size();
        }
    }

    // -------------------------------------------------------
    // Cache lookup result
    // -------------------------------------------------------
    static class CacheResult {
        VideoData data;
        String    foundAt;     // L1, L2, L3, or NOT_FOUND
        long      lookupTimeMs;
        boolean   promoted;

        CacheResult(VideoData data, String foundAt,
                    long lookupTimeMs, boolean promoted) {
            this.data         = data;
            this.foundAt      = foundAt;
            this.lookupTimeMs = lookupTimeMs;
            this.promoted     = promoted;
        }
    }

    // Cache levels
    private CacheLevel l1Cache;  // In-memory (fastest)
    private CacheLevel l2Cache;  // SSD-backed (medium)

    // L3 Database - HashMap<videoId, VideoData> (all videos)
    private HashMap<String, VideoData> l3Database;

    // Access count tracker across all levels
    private HashMap<String, Integer> globalAccessCount;

    // Promotion thresholds
    private static final int L2_TO_L1_THRESHOLD = 5;
    private static final int L3_TO_L2_THRESHOLD = 2;

    // Global stats
    private int totalRequests;
    private long totalResponseTime;

    /**
     * Constructor - initializes all cache levels and database
     */
    public P10_MultiLevelCache() {
        // L1: 5 slots, 1ms access (demo sizes)
        l1Cache = new CacheLevel("L1-Memory", 5, 1, L2_TO_L1_THRESHOLD);

        // L2: 10 slots, 5ms access
        l2Cache = new CacheLevel("L2-SSD", 10, 5, L3_TO_L2_THRESHOLD);

        // L3: Database (unlimited, 150ms access)
        l3Database         = new HashMap<>();
        globalAccessCount  = new HashMap<>();
        totalRequests      = 0;
        totalResponseTime  = 0;

        // Populate L3 database with videos
        addToDatabase("vid_001", "Stranger Things S4",    "4K",  4096);
        addToDatabase("vid_002", "The Crown S5",          "4K",  3800);
        addToDatabase("vid_003", "Wednesday",             "1080p", 2048);
        addToDatabase("vid_004", "Squid Game S2",         "4K",  4200);
        addToDatabase("vid_005", "Money Heist Final",     "4K",  3900);
        addToDatabase("vid_006", "Breaking Bad S1",       "1080p", 2200);
        addToDatabase("vid_007", "Dark S3",               "4K",  3500);
        addToDatabase("vid_008", "Narcos Mexico S3",      "1080p", 2800);
        addToDatabase("vid_009", "Ozark S4",              "4K",  4100);
        addToDatabase("vid_010", "The Witcher S3",        "4K",  3700);
        addToDatabase("vid_011", "Black Mirror S6",       "1080p", 2100);
        addToDatabase("vid_012", "Peaky Blinders S6",     "4K",  3300);
    }

    /**
     * Add a video directly to L3 database.
     */
    private void addToDatabase(String id, String title,
                               String resolution, long sizeMb) {
        l3Database.put(id, new VideoData(
                id, title, resolution, sizeMb * 1024 * 1024));
        globalAccessCount.put(id, 0);
    }

    /**
     * Get a video from the cache hierarchy.
     * Checks L1 → L2 → L3 in order.
     *
     * @param videoId the video to retrieve
     * @return CacheResult with data and performance metrics
     */
    public CacheResult getVideo(String videoId) {
        long startTime = System.currentTimeMillis();
        totalRequests++;
        boolean promoted = false;

        // Update global access count
        globalAccessCount.put(videoId,
                globalAccessCount.getOrDefault(videoId, 0) + 1);
        int accessCount = globalAccessCount.get(videoId);

        // ── L1 Cache Check ──────────────────────────────
        if (l1Cache.contains(videoId)) {
            l1Cache.hits++;
            VideoData data = l1Cache.get(videoId);
            data.accessCount++;

            long elapsed = System.currentTimeMillis() - startTime
                    + l1Cache.accessTimeMs;
            totalResponseTime += elapsed;

            System.out.println("⚡ L1 Cache HIT  → " + data.title
                    + " [" + elapsed + "ms]");
            return new CacheResult(data, "L1", elapsed, false);
        }
        l1Cache.misses++;

        // ── L2 Cache Check ──────────────────────────────
        if (l2Cache.contains(videoId)) {
            l2Cache.hits++;
            VideoData data = l2Cache.get(videoId);
            data.accessCount++;

            long elapsed = System.currentTimeMillis() - startTime
                    + l2Cache.accessTimeMs;

            // Promote L2 → L1 if access count exceeds threshold
            if (accessCount >= L2_TO_L1_THRESHOLD) {
                l1Cache.put(videoId, data);
                l2Cache.remove(videoId);
                promoted = true;
                System.out.println("🚀 Promoted L2 → L1: " + data.title);
            }

            totalResponseTime += elapsed;
            System.out.println("💾 L2 Cache HIT  → " + data.title
                    + " [" + elapsed + "ms]"
                    + (promoted ? " (promoted to L1)" : ""));
            return new CacheResult(data, "L2", elapsed, promoted);
        }
        l2Cache.misses++;

        // ── L3 Database Check ───────────────────────────
        if (l3Database.containsKey(videoId)) {
            VideoData data = l3Database.get(videoId);
            data.accessCount++;

            // Simulated DB access delay
            try { Thread.sleep(150); } catch (InterruptedException e) {}

            long elapsed = System.currentTimeMillis() - startTime;

            // Add to L2 cache
            l2Cache.put(videoId, data);
            promoted = true;
            System.out.println("🗄️  L3 DB HIT     → " + data.title
                    + " [" + elapsed + "ms] (cached in L2)");

            // Promote to L1 if already popular
            if (accessCount >= L2_TO_L1_THRESHOLD) {
                l1Cache.put(videoId, data);
                System.out.println("🚀 Promoted L3 → L1: " + data.title);
            }

            totalResponseTime += elapsed;
            return new CacheResult(data, "L3", elapsed, promoted);
        }

        // ── Not Found ───────────────────────────────────
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("❌ Video not found: " + videoId);
        return new CacheResult(null, "NOT_FOUND", elapsed, false);
    }

    /**
     * Invalidate a video across all cache levels.
     * Called when content is updated.
     *
     * @param videoId the video to invalidate
     */
    public void invalidateCache(String videoId) {
        boolean found = false;

        if (l1Cache.contains(videoId)) {
            l1Cache.remove(videoId);
            System.out.println("🗑️  Invalidated from L1: " + videoId);
            found = true;
        }
        if (l2Cache.contains(videoId)) {
            l2Cache.remove(videoId);
            System.out.println("🗑️  Invalidated from L2: " + videoId);
            found = true;
        }
        if (!found) {
            System.out.println("ℹ️  Video not in any cache: " + videoId);
        }
    }

    /**
     * Add a new video to the database.
     */
    public void addVideo(String id, String title,
                         String resolution, long sizeMb) {
        addToDatabase(id, title, resolution, sizeMb);
        System.out.println("✅ Added to L3 DB: [" + id + "] "
                + title);
    }

    /**
     * Display current state of all cache levels.
     */
    public void displayCacheLevels() {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║           Cache Level Status             ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // L1
        System.out.println("\n⚡ L1 Cache (In-Memory) ["
                + l1Cache.size() + "/" + l1Cache.maxSize + " slots]:");
        if (l1Cache.cache.isEmpty()) {
            System.out.println("   Empty");
        } else {
            for (Map.Entry<String, VideoData> e
                    : l1Cache.cache.entrySet()) {
                System.out.println("   → " + e.getValue()
                        + " | Access: " + globalAccessCount
                        .getOrDefault(e.getKey(), 0));
            }
        }

        // L2
        System.out.println("\n💾 L2 Cache (SSD) ["
                + l2Cache.size() + "/" + l2Cache.maxSize + " slots]:");
        if (l2Cache.cache.isEmpty()) {
            System.out.println("   Empty");
        } else {
            for (Map.Entry<String, VideoData> e
                    : l2Cache.cache.entrySet()) {
                System.out.println("   → " + e.getValue()
                        + " | Access: " + globalAccessCount
                        .getOrDefault(e.getKey(), 0));
            }
        }

        // L3
        System.out.println("\n🗄️  L3 Database ["
                + l3Database.size() + " videos total]");
        System.out.println("═══════════════════════════════════════════");
    }

    /**
     * Display performance statistics for all cache levels.
     */
    public void displayStats() {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║         Cache Performance Statistics     ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Total Requests : " + totalRequests);

        double avgResponse = totalRequests > 0
                ? (double) totalResponseTime / totalRequests : 0;
        System.out.printf("Avg Response   : %.1f ms%n", avgResponse);

        System.out.println("\n  Level  │ Hits  │ Misses │ Hit Rate │ Access Time");
        System.out.println("  ───────┼───────┼────────┼──────────┼────────────");
        System.out.printf("  %-6s │ %-5d │ %-6d │ %-8.1f │ %dms%n",
                "L1", l1Cache.hits, l1Cache.misses,
                l1Cache.getHitRate(), l1Cache.accessTimeMs);
        System.out.printf("  %-6s │ %-5d │ %-6d │ %-8.1f │ %dms%n",
                "L2", l2Cache.hits, l2Cache.misses,
                l2Cache.getHitRate(), l2Cache.accessTimeMs);

        int l3Hits = totalRequests - l1Cache.hits - l2Cache.hits;
        System.out.printf("  %-6s │ %-5d │ %-6s │ %-8.1f │ %dms%n",
                "L3", Math.max(0, l3Hits), "-",
                totalRequests > 0
                        ? (Math.max(0, l3Hits) * 100.0 / totalRequests) : 0,
                150);

        System.out.println("═══════════════════════════════════════════");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P10_MultiLevelCache cache = new P10_MultiLevelCache();
        Scanner scanner           = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("  Multi-Level Cache System (Netflix-Style)");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Get Video");
            System.out.println("2. Invalidate Cache");
            System.out.println("3. Add New Video");
            System.out.println("4. Display Cache Levels");
            System.out.println("5. Display Statistics");
            System.out.println("6. Simulate Popular Video (10 requests)");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter Video ID (e.g. vid_001): ");
                    String videoId = scanner.nextLine();
                    CacheResult result = cache.getVideo(videoId);
                    if (result.data != null) {
                        System.out.println("   Title   : "
                                + result.data.title);
                        System.out.println("   Found At: "
                                + result.foundAt);
                        System.out.println("   Time    : "
                                + result.lookupTimeMs + "ms");
                    }
                    break;

                case 2:
                    System.out.print("Enter Video ID to invalidate: ");
                    String invId = scanner.nextLine();
                    cache.invalidateCache(invId);
                    break;

                case 3:
                    System.out.print("Enter Video ID: ");
                    String newId = scanner.nextLine();
                    System.out.print("Enter Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Enter Resolution (4K/1080p): ");
                    String res = scanner.nextLine();
                    System.out.print("Enter Size (MB): ");
                    long size = scanner.nextLong();
                    scanner.nextLine();
                    cache.addVideo(newId, title, res, size);
                    break;

                case 4:
                    cache.displayCacheLevels();
                    break;

                case 5:
                    cache.displayStats();
                    break;

                case 6:
                    System.out.print(
                            "Enter Video ID to simulate popularity: ");
                    String popId = scanner.nextLine();
                    System.out.println(
                            "\n🚀 Simulating 10 requests for "
                                    + popId + "...\n");
                    for (int i = 1; i <= 10; i++) {
                        System.out.print("Request #" + i + ": ");
                        cache.getVideo(popId);
                    }
                    break;

                case 7:
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