import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class P3_DNSCache {

    // -------------------------------------------------------
    // DNS Entry class - stores domain metadata
    // -------------------------------------------------------
    static class DNSEntry {
        String domain;
        String ipAddress;
        long   timestamp;    // when entry was cached (ms)
        long   expiryTime;   // when entry expires (ms)
        int    ttlSeconds;   // TTL in seconds

        DNSEntry(String domain, String ipAddress, int ttlSeconds) {
            this.domain     = domain;
            this.ipAddress  = ipAddress;
            this.ttlSeconds = ttlSeconds;
            this.timestamp  = System.currentTimeMillis();
            this.expiryTime = this.timestamp + (ttlSeconds * 1000L);
        }

        /**
         * Check if this entry has expired.
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        /**
         * Get remaining TTL in seconds.
         */
        long getRemainingTTL() {
            long remaining = (expiryTime - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }
    }

    // -------------------------------------------------------
    // LRU Cache using LinkedHashMap (access-order)
    // -------------------------------------------------------
    private final int MAX_CACHE_SIZE = 5;

    // LinkedHashMap in access-order for LRU eviction
    private LinkedHashMap<String, DNSEntry> cache;

    // Simulated upstream DNS database
    private HashMap<String, String> upstreamDNS;

    // Performance metrics
    private int cacheHits;
    private int cacheMisses;
    private int cacheExpired;
    private long totalLookupTime;
    private int totalLookups;

    /**
     * Constructor - initializes DNS cache and upstream DNS
     */
    public P3_DNSCache() {
        cacheHits       = 0;
        cacheMisses     = 0;
        cacheExpired    = 0;
        totalLookupTime = 0;
        totalLookups    = 0;

        // LRU LinkedHashMap - removes eldest entry when full
        cache = new LinkedHashMap<String, DNSEntry>(
                MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, DNSEntry> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        // Simulated upstream DNS database
        upstreamDNS = new HashMap<>();
        upstreamDNS.put("google.com",    "172.217.14.206");
        upstreamDNS.put("github.com",    "140.82.113.4");
        upstreamDNS.put("youtube.com",   "142.250.80.78");
        upstreamDNS.put("facebook.com",  "157.240.241.35");
        upstreamDNS.put("amazon.com",    "205.251.242.103");
        upstreamDNS.put("netflix.com",   "54.237.226.164");
        upstreamDNS.put("twitter.com",   "104.244.42.193");
        upstreamDNS.put("linkedin.com",  "108.174.10.10");
    }

    /**
     * Resolve a domain name to IP address.
     * Checks cache first, then queries upstream DNS on miss.
     *
     * @param domain the domain to resolve
     * @return IP address string
     */
    public String resolve(String domain) {
        long startTime = System.nanoTime();
        totalLookups++;
        String result;

        // Check if entry exists in cache
        if (cache.containsKey(domain)) {
            DNSEntry entry = cache.get(domain);

            // Check if entry has expired
            if (entry.isExpired()) {
                cache.remove(domain);
                cacheExpired++;
                System.out.println("⏰ Cache EXPIRED for \"" + domain + "\"");
                result = queryUpstream(domain);
            } else {
                // Cache HIT
                cacheHits++;
                long lookupTime = System.nanoTime() - startTime;
                totalLookupTime += lookupTime;
                System.out.println("✅ Cache HIT → " + entry.ipAddress
                        + " (TTL remaining: " + entry.getRemainingTTL() + "s)"
                        + " [" + lookupTime + " ns]");
                return entry.ipAddress;
            }
        } else {
            // Cache MISS
            cacheMisses++;
            System.out.println("❌ Cache MISS for \"" + domain + "\"");
            result = queryUpstream(domain);
        }

        long lookupTime = System.nanoTime() - startTime;
        totalLookupTime += lookupTime;
        return result;
    }

    /**
     * Query upstream DNS server (simulated with 100ms delay).
     *
     * @param domain domain to query
     * @return IP address from upstream
     */
    private String queryUpstream(String domain) {
        // Simulate upstream DNS delay (100ms)
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        if (upstreamDNS.containsKey(domain)) {
            String ip  = upstreamDNS.get(domain);
            int    ttl = 30; // 30 seconds TTL

            // Store in cache
            cache.put(domain, new DNSEntry(domain, ip, ttl));
            System.out.println("🌐 Upstream DNS → " + ip
                    + " (TTL: " + ttl + "s) [cached]");
            return ip;
        }

        System.out.println("⚠️  Domain \"" + domain + "\" not found!");
        return "NXDOMAIN";
    }

    /**
     * Manually add an entry to the cache.
     *
     * @param domain     domain name
     * @param ip         IP address
     * @param ttlSeconds TTL in seconds
     */
    public void addEntry(String domain, String ip, int ttlSeconds) {
        cache.put(domain, new DNSEntry(domain, ip, ttlSeconds));
        System.out.println("📝 Added: " + domain + " → " + ip
                + " (TTL: " + ttlSeconds + "s)");
    }

    /**
     * Remove expired entries from cache.
     */
    public void cleanExpiredEntries() {
        int removed = 0;
        cache.entrySet().removeIf(entry -> {
            return entry.getValue().isExpired();
        });
        System.out.println("🧹 Cleaned expired entries from cache.");
    }

    /**
     * Display current cache contents.
     */
    public void displayCache() {
        System.out.println("\n========== DNS Cache Contents ==========");
        System.out.printf("%-20s %-18s %-8s %-10s%n",
                "Domain", "IP Address", "TTL(s)", "Status");
        System.out.println("--------------------------------------------------");

        if (cache.isEmpty()) {
            System.out.println("  Cache is empty.");
        } else {
            for (Map.Entry<String, DNSEntry> entry : cache.entrySet()) {
                DNSEntry dns = entry.getValue();
                String status = dns.isExpired() ? "EXPIRED" : "VALID";
                System.out.printf("%-20s %-18s %-8d %-10s%n",
                        dns.domain,
                        dns.ipAddress,
                        dns.getRemainingTTL(),
                        status);
            }
        }
        System.out.println("Cache Size: " + cache.size()
                + "/" + MAX_CACHE_SIZE);
        System.out.println("=========================================");
    }

    /**
     * Display cache performance statistics.
     */
    public void displayStats() {
        System.out.println("\n========== Cache Statistics ==========");
        System.out.println("Total Lookups  : " + totalLookups);
        System.out.println("Cache Hits     : " + cacheHits);
        System.out.println("Cache Misses   : " + cacheMisses);
        System.out.println("Cache Expired  : " + cacheExpired);

        double hitRate = totalLookups > 0
                ? (cacheHits * 100.0 / totalLookups) : 0;
        System.out.printf("Hit Rate       : %.1f%%%n", hitRate);

        double avgTime = totalLookups > 0
                ? (totalLookupTime / totalLookups) : 0;
        System.out.printf("Avg Lookup Time: %.0f ns%n", avgTime);
        System.out.println("======================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P3_DNSCache dns = new P3_DNSCache();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("       DNS Cache with TTL System");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Resolve Domain");
            System.out.println("2. Add DNS Entry Manually");
            System.out.println("3. Clean Expired Entries");
            System.out.println("4. Display Cache");
            System.out.println("5. Display Statistics");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter domain (e.g. google.com): ");
                    String domain = scanner.nextLine();
                    String ip = dns.resolve(domain);
                    System.out.println("Resolved: " + domain + " → " + ip);
                    break;

                case 2:
                    System.out.print("Enter domain: ");
                    String d = scanner.nextLine();
                    System.out.print("Enter IP address: ");
                    String ipAddr = scanner.nextLine();
                    System.out.print("Enter TTL (seconds): ");
                    int ttl = scanner.nextInt();
                    scanner.nextLine();
                    dns.addEntry(d, ipAddr, ttl);
                    break;

                case 3:
                    dns.cleanExpiredEntries();
                    break;

                case 4:
                    dns.displayCache();
                    break;

                case 5:
                    dns.displayStats();
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