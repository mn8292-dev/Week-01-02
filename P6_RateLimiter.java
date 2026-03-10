import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class P6_RateLimiter {

    // -------------------------------------------------------
    // Token Bucket class - tracks each client's rate limit
    // -------------------------------------------------------
    static class TokenBucket {
        String clientId;
        int    tokens;          // current available tokens
        int    maxTokens;       // max tokens (= request limit)
        int    refillRate;      // tokens refilled per hour
        long   lastRefillTime;  // last time bucket was refilled
        int    totalRequests;   // total requests made
        int    deniedRequests;  // total denied requests

        TokenBucket(String clientId, int maxTokens, int refillRate) {
            this.clientId      = clientId;
            this.maxTokens     = maxTokens;
            this.tokens        = maxTokens;
            this.refillRate    = refillRate;
            this.lastRefillTime = System.currentTimeMillis();
            this.totalRequests  = 0;
            this.deniedRequests = 0;
        }

        /**
         * Refill tokens based on time elapsed since last refill.
         * Simulates hourly window reset.
         */
        void refillTokens() {
            long now     = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            // Refill every 10 seconds (simulated hour for demo)
            long refillIntervalMs = 10_000;

            if (elapsed >= refillIntervalMs) {
                long periods = elapsed / refillIntervalMs;
                tokens = Math.min(maxTokens,
                        tokens + (int)(periods * refillRate));
                lastRefillTime = now;
            }
        }

        /**
         * Get seconds until next refill.
         */
        long getSecondsUntilReset() {
            long elapsed = System.currentTimeMillis() - lastRefillTime;
            return Math.max(0, (10_000 - elapsed) / 1000);
        }
    }

    // -------------------------------------------------------
    // Rate Limit Result class
    // -------------------------------------------------------
    static class RateLimitResult {
        boolean allowed;
        int     remainingTokens;
        long    retryAfterSeconds;
        String  message;

        RateLimitResult(boolean allowed, int remaining,
                        long retryAfter, String message) {
            this.allowed          = allowed;
            this.remainingTokens  = remaining;
            this.retryAfterSeconds = retryAfter;
            this.message          = message;
        }
    }

    // HashMap<clientId, TokenBucket> for O(1) client lookup
    private HashMap<String, TokenBucket> clientBuckets;

    // Rate limit configuration
    private final int MAX_REQUESTS  = 10;   // 10 per window (demo)
    private final int REFILL_RATE   = 10;   // refill full per window

    // Global stats
    private int totalAllowed;
    private int totalDenied;

    /**
     * Constructor - initializes rate limiter
     */
    public P6_RateLimiter() {
        clientBuckets = new HashMap<>();
        totalAllowed  = 0;
        totalDenied   = 0;

        // Pre-register some clients
        registerClient("client_abc123");
        registerClient("client_xyz789");
        registerClient("client_api001");
    }

    /**
     * Register a new client with default rate limit.
     *
     * @param clientId unique client API key
     */
    public void registerClient(String clientId) {
        clientBuckets.put(clientId,
                new TokenBucket(clientId, MAX_REQUESTS, REFILL_RATE));
        System.out.println("✅ Registered client: " + clientId
                + " (Limit: " + MAX_REQUESTS + " req/window)");
    }

    /**
     * Check rate limit for a client in O(1) time.
     * Consumes one token if allowed.
     *
     * @param clientId the client making the request
     * @return RateLimitResult with allow/deny decision
     */
    public RateLimitResult checkRateLimit(String clientId) {
        long startTime = System.nanoTime();

        // Auto-register unknown clients
        if (!clientBuckets.containsKey(clientId)) {
            registerClient(clientId);
        }

        TokenBucket bucket = clientBuckets.get(clientId);

        // Refill tokens based on elapsed time
        bucket.refillTokens();
        bucket.totalRequests++;

        if (bucket.tokens > 0) {
            // Allow request - consume one token
            bucket.tokens--;
            totalAllowed++;

            long elapsed = System.nanoTime() - startTime;
            return new RateLimitResult(
                    true,
                    bucket.tokens,
                    0,
                    "✅ Allowed (" + bucket.tokens
                            + " requests remaining) [" + elapsed + " ns]"
            );
        } else {
            // Deny request
            bucket.deniedRequests++;
            totalDenied++;

            long retryAfter = bucket.getSecondsUntilReset();
            long elapsed    = System.nanoTime() - startTime;
            return new RateLimitResult(
                    false,
                    0,
                    retryAfter,
                    "❌ Denied (0 remaining, retry after "
                            + retryAfter + "s) [" + elapsed + " ns]"
            );
        }
    }

    /**
     * Get rate limit status for a client.
     *
     * @param clientId the client to check
     */
    public void getRateLimitStatus(String clientId) {
        if (!clientBuckets.containsKey(clientId)) {
            System.out.println("⚠️  Client not found: " + clientId);
            return;
        }

        TokenBucket bucket = clientBuckets.get(clientId);
        bucket.refillTokens();

        System.out.println("\n========== Rate Limit Status ==========");
        System.out.println("Client ID      : " + clientId);
        System.out.println("Used           : "
                + (bucket.maxTokens - bucket.tokens)
                + "/" + bucket.maxTokens);
        System.out.println("Remaining      : " + bucket.tokens);
        System.out.println("Reset In       : "
                + bucket.getSecondsUntilReset() + "s");
        System.out.println("Total Requests : " + bucket.totalRequests);
        System.out.println("Denied         : " + bucket.deniedRequests);
        System.out.println("=======================================");
    }

    /**
     * Display all registered clients and their status.
     */
    public void displayAllClients() {
        System.out.println("\n========== All Clients ==========");
        System.out.printf("%-20s %-8s %-8s %-8s %-8s%n",
                "Client ID", "Tokens", "Used", "Total", "Denied");
        System.out.println("------------------------------------------------");

        for (Map.Entry<String, TokenBucket> entry
                : clientBuckets.entrySet()) {
            TokenBucket b = entry.getValue();
            b.refillTokens();
            System.out.printf("%-20s %-8d %-8d %-8d %-8d%n",
                    b.clientId,
                    b.tokens,
                    b.maxTokens - b.tokens,
                    b.totalRequests,
                    b.deniedRequests);
        }
        System.out.println("=================================");
    }

    /**
     * Display global rate limiter statistics.
     */
    public void displayGlobalStats() {
        System.out.println("\n========== Global Stats ==========");
        System.out.println("Total Clients  : " + clientBuckets.size());
        System.out.println("Total Allowed  : " + totalAllowed);
        System.out.println("Total Denied   : " + totalDenied);
        int total = totalAllowed + totalDenied;
        double allowRate = total > 0
                ? (totalAllowed * 100.0 / total) : 0;
        System.out.printf("Allow Rate     : %.1f%%%n", allowRate);
        System.out.println("==================================");
    }

    /**
     * Simulate burst traffic from a single client.
     *
     * @param clientId  client to simulate
     * @param requests  number of requests to fire
     */
    public void simulateBurst(String clientId, int requests) {
        System.out.println("\n🚀 Simulating " + requests
                + " requests from " + clientId + "...\n");

        for (int i = 1; i <= requests; i++) {
            RateLimitResult result = checkRateLimit(clientId);
            System.out.println("Request #" + i + " → " + result.message);
        }
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P6_RateLimiter limiter = new P6_RateLimiter();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n==========================================");
        System.out.println("   Distributed Rate Limiter for API Gateway");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Check Rate Limit (Single Request)");
            System.out.println("2. Simulate Burst Traffic");
            System.out.println("3. Get Client Status");
            System.out.println("4. Register New Client");
            System.out.println("5. Display All Clients");
            System.out.println("6. Display Global Stats");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter Client ID: ");
                    String clientId = scanner.nextLine();
                    RateLimitResult result = limiter.checkRateLimit(clientId);
                    System.out.println(result.message);
                    break;

                case 2:
                    System.out.print("Enter Client ID: ");
                    String burstClient = scanner.nextLine();
                    System.out.print("Number of requests: ");
                    int requests = scanner.nextInt();
                    scanner.nextLine();
                    limiter.simulateBurst(burstClient, requests);
                    break;

                case 3:
                    System.out.print("Enter Client ID: ");
                    String statusClient = scanner.nextLine();
                    limiter.getRateLimitStatus(statusClient);
                    break;

                case 4:
                    System.out.print("Enter new Client ID: ");
                    String newClient = scanner.nextLine();
                    limiter.registerClient(newClient);
                    break;

                case 5:
                    limiter.displayAllClients();
                    break;

                case 6:
                    limiter.displayGlobalStats();
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