import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class P9_TwoSum {

    // -------------------------------------------------------
    // Transaction class
    // -------------------------------------------------------
    static class Transaction {
        int    id;
        double amount;
        String merchant;
        String accountId;
        long   timestamp;  // milliseconds

        Transaction(int id, double amount,
                    String merchant, String accountId, long timestamp) {
            this.id        = id;
            this.amount    = amount;
            this.merchant  = merchant;
            this.accountId = accountId;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "TXN#" + id + " | $" + amount
                    + " | " + merchant + " | " + accountId;
        }
    }

    // -------------------------------------------------------
    // Result classes
    // -------------------------------------------------------
    static class TwoSumResult {
        Transaction txn1;
        Transaction txn2;
        double      sum;

        TwoSumResult(Transaction t1, Transaction t2) {
            this.txn1 = t1;
            this.txn2 = t2;
            this.sum  = t1.amount + t2.amount;
        }

        @Override
        public String toString() {
            return "(" + txn1 + ") + (" + txn2 + ")"
                    + " = $" + String.format("%.2f", sum);
        }
    }

    // List of all transactions
    private List<Transaction> transactions;

    // HashMap<amount, List<Transaction>> for O(1) complement lookup
    private HashMap<Double, List<Transaction>> amountIndex;

    // HashMap<merchantKey, List<Transaction>> for duplicate detection
    // key = amount_merchant
    private HashMap<String, List<Transaction>> merchantAmountIndex;

    /**
     * Constructor - initializes transaction store
     */
    public P9_TwoSum() {
        transactions        = new ArrayList<>();
        amountIndex         = new HashMap<>();
        merchantAmountIndex = new HashMap<>();

        // Simulate base timestamp (10:00 AM today)
        long base = System.currentTimeMillis()
                - (3 * 3600000); // 3 hours ago

        // Pre-populate with sample transactions
        addTransaction(new Transaction(1,  500.00, "Store A",  "acc_001", base));
        addTransaction(new Transaction(2,  300.00, "Store B",  "acc_002", base + 900000));
        addTransaction(new Transaction(3,  200.00, "Store C",  "acc_003", base + 1800000));
        addTransaction(new Transaction(4,  700.00, "Store D",  "acc_004", base + 2700000));
        addTransaction(new Transaction(5,  150.00, "Store A",  "acc_005", base + 3600000));
        addTransaction(new Transaction(6,  350.00, "Store E",  "acc_006", base + 4500000));
        addTransaction(new Transaction(7,  500.00, "Store A",  "acc_007", base + 5400000));
        addTransaction(new Transaction(8,  100.00, "Store F",  "acc_008", base + 6300000));
        addTransaction(new Transaction(9,  400.00, "Store G",  "acc_009", base + 7200000));
        addTransaction(new Transaction(10, 600.00, "Store H",  "acc_010", base + 8100000));
    }

    /**
     * Add a transaction to the store and update indexes.
     *
     * @param txn the transaction to add
     */
    public void addTransaction(Transaction txn) {
        transactions.add(txn);

        // Index by amount for complement lookup
        amountIndex.computeIfAbsent(txn.amount,
                k -> new ArrayList<>()).add(txn);

        // Index by amount_merchant for duplicate detection
        String key = txn.amount + "_" + txn.merchant;
        merchantAmountIndex.computeIfAbsent(key,
                k -> new ArrayList<>()).add(txn);
    }

    /**
     * Classic Two-Sum: Find all pairs that sum to target.
     * Uses HashMap for O(n) time complexity.
     *
     * @param target the target sum
     * @return list of matching pairs
     */
    public List<TwoSumResult> findTwoSum(double target) {
        long startTime = System.nanoTime();
        List<TwoSumResult> results = new ArrayList<>();

        // HashMap<amount, Transaction> for complement lookup
        HashMap<Double, Transaction> seen = new HashMap<>();
        HashSet<String> addedPairs        = new HashSet<>();

        for (Transaction txn : transactions) {
            double complement = target - txn.amount;

            if (seen.containsKey(complement)) {
                Transaction other = seen.get(complement);

                // Avoid duplicate pairs
                String pairKey = Math.min(txn.id, other.id)
                        + "_" + Math.max(txn.id, other.id);

                if (!addedPairs.contains(pairKey)) {
                    results.add(new TwoSumResult(other, txn));
                    addedPairs.add(pairKey);
                }
            }
            seen.put(txn.amount, txn);
        }

        long elapsed = System.nanoTime() - startTime;
        System.out.println("⏱ Two-Sum completed in " + elapsed + " ns");
        return results;
    }

    /**
     * Two-Sum with time window: Pairs within specified hours.
     *
     * @param target      the target sum
     * @param windowHours time window in hours
     * @return list of matching pairs within window
     */
    public List<TwoSumResult> findTwoSumWithWindow(
            double target, double windowHours) {
        long startTime   = System.nanoTime();
        long windowMs    = (long)(windowHours * 3600000);
        List<TwoSumResult> results = new ArrayList<>();
        HashSet<String> addedPairs = new HashSet<>();

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t1 = transactions.get(i);

            // HashMap for complement lookup within window
            HashMap<Double, Transaction> windowMap = new HashMap<>();

            for (int j = 0; j < transactions.size(); j++) {
                if (i == j) continue;
                Transaction t2 = transactions.get(j);

                // Check time window
                if (Math.abs(t1.timestamp - t2.timestamp) <= windowMs) {
                    windowMap.put(t2.amount, t2);
                }
            }

            double complement = target - t1.amount;
            if (windowMap.containsKey(complement)) {
                Transaction t2 = windowMap.get(complement);
                String pairKey = Math.min(t1.id, t2.id)
                        + "_" + Math.max(t1.id, t2.id);

                if (!addedPairs.contains(pairKey)) {
                    results.add(new TwoSumResult(t1, t2));
                    addedPairs.add(pairKey);
                }
            }
        }

        long elapsed = System.nanoTime() - startTime;
        System.out.println("⏱ Two-Sum (window=" + windowHours
                + "h) completed in " + elapsed + " ns");
        return results;
    }

    /**
     * K-Sum: Find K transactions that sum to target.
     * Uses recursion with hash table memoization.
     *
     * @param k      number of transactions
     * @param target the target sum
     * @return list of K-transaction groups
     */
    public List<List<Transaction>> findKSum(int k, double target) {
        long startTime = System.nanoTime();

        // Sort by amount for efficient k-sum
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort((a, b) -> Double.compare(a.amount, b.amount));

        List<List<Transaction>> results = new ArrayList<>();
        kSumHelper(sorted, k, target, 0,
                new ArrayList<>(), results);

        long elapsed = System.nanoTime() - startTime;
        System.out.println("⏱ K-Sum (k=" + k + ") completed in "
                + elapsed + " ns");
        return results;
    }

    /**
     * Recursive helper for K-Sum.
     */
    private void kSumHelper(List<Transaction> txns, int k,
                            double target, int start,
                            List<Transaction> current,
                            List<List<Transaction>> results) {

        if (k == 2) {
            // Use HashMap for two-sum base case
            HashMap<Double, Transaction> seen = new HashMap<>();
            for (int i = start; i < txns.size(); i++) {
                Transaction t = txns.get(i);
                double complement = target - t.amount;
                if (seen.containsKey(complement)) {
                    List<Transaction> group =
                            new ArrayList<>(current);
                    group.add(seen.get(complement));
                    group.add(t);
                    results.add(group);
                }
                seen.put(t.amount, t);
            }
            return;
        }

        for (int i = start; i < txns.size(); i++) {
            current.add(txns.get(i));
            kSumHelper(txns, k - 1,
                    target - txns.get(i).amount,
                    i + 1, current, results);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Detect duplicate transactions:
     * Same amount + same merchant, different accounts.
     *
     * @return map of suspicious duplicate groups
     */
    public HashMap<String, List<Transaction>> detectDuplicates() {
        long startTime = System.nanoTime();
        HashMap<String, List<Transaction>> duplicates = new HashMap<>();

        for (Map.Entry<String, List<Transaction>> entry
                : merchantAmountIndex.entrySet()) {
            List<Transaction> txns = entry.getValue();

            // Check for different accounts with same amount + merchant
            HashSet<String> accounts = new HashSet<>();
            boolean hasDuplicate     = false;

            for (Transaction t : txns) {
                if (accounts.contains(t.accountId)) {
                    hasDuplicate = true;
                } else {
                    accounts.add(t.accountId);
                }
            }

            if (txns.size() > 1 && accounts.size() > 1) {
                duplicates.put(entry.getKey(), txns);
            }
        }

        long elapsed = System.nanoTime() - startTime;
        System.out.println("⏱ Duplicate detection completed in "
                + elapsed + " ns");
        return duplicates;
    }

    /**
     * Display all transactions.
     */
    public void displayTransactions() {
        System.out.println("\n========== All Transactions ==========");
        System.out.printf("%-6s %-10s %-12s %-12s%n",
                "ID", "Amount", "Merchant", "Account");
        System.out.println("---------------------------------------");
        for (Transaction t : transactions) {
            System.out.printf("%-6d $%-9.2f %-12s %-12s%n",
                    t.id, t.amount, t.merchant, t.accountId);
        }
        System.out.println("Total: " + transactions.size()
                + " transactions");
        System.out.println("======================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P9_TwoSum processor = new P9_TwoSum();
        Scanner scanner     = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("  Two-Sum Financial Transaction Analyzer");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Find Two-Sum Pairs");
            System.out.println("2. Find Two-Sum with Time Window");
            System.out.println("3. Find K-Sum Groups");
            System.out.println("4. Detect Duplicate Transactions");
            System.out.println("5. Add New Transaction");
            System.out.println("6. Display All Transactions");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter target amount ($): ");
                    double target = scanner.nextDouble();
                    scanner.nextLine();
                    List<TwoSumResult> pairs =
                            processor.findTwoSum(target);
                    System.out.println("\n🔍 Two-Sum Results (target=$"
                            + target + "):");
                    if (pairs.isEmpty()) {
                        System.out.println("  No pairs found.");
                    } else {
                        for (TwoSumResult r : pairs) {
                            System.out.println("  → " + r);
                        }
                    }
                    break;

                case 2:
                    System.out.print("Enter target amount ($): ");
                    double wTarget = scanner.nextDouble();
                    System.out.print("Enter time window (hours): ");
                    double window = scanner.nextDouble();
                    scanner.nextLine();
                    List<TwoSumResult> windowPairs =
                            processor.findTwoSumWithWindow(wTarget, window);
                    System.out.println("\n🔍 Two-Sum Window Results:");
                    if (windowPairs.isEmpty()) {
                        System.out.println("  No pairs found.");
                    } else {
                        for (TwoSumResult r : windowPairs) {
                            System.out.println("  → " + r);
                        }
                    }
                    break;

                case 3:
                    System.out.print("Enter K (number of transactions): ");
                    int k = scanner.nextInt();
                    System.out.print("Enter target amount ($): ");
                    double kTarget = scanner.nextDouble();
                    scanner.nextLine();
                    List<List<Transaction>> groups =
                            processor.findKSum(k, kTarget);
                    System.out.println("\n🔍 K-Sum Results (k=" + k
                            + ", target=$" + kTarget + "):");
                    if (groups.isEmpty()) {
                        System.out.println("  No groups found.");
                    } else {
                        int g = 1;
                        for (List<Transaction> group : groups) {
                            System.out.print("  Group " + g++ + ": ");
                            double sum = 0;
                            for (Transaction t : group) {
                                System.out.print("TXN#" + t.id
                                        + "($" + t.amount + ") ");
                                sum += t.amount;
                            }
                            System.out.printf("= $%.2f%n", sum);
                        }
                    }
                    break;

                case 4:
                    HashMap<String, List<Transaction>> dups =
                            processor.detectDuplicates();
                    System.out.println("\n🚨 Duplicate Transactions:");
                    if (dups.isEmpty()) {
                        System.out.println("  No duplicates found.");
                    } else {
                        for (Map.Entry<String, List<Transaction>>
                                entry : dups.entrySet()) {
                            System.out.println("  Key: "
                                    + entry.getKey());
                            for (Transaction t : entry.getValue()) {
                                System.out.println("    → " + t);
                            }
                        }
                    }
                    break;

                case 5:
                    System.out.print("Enter amount ($): ");
                    double amt = scanner.nextDouble();
                    scanner.nextLine();
                    System.out.print("Enter merchant: ");
                    String merchant = scanner.nextLine();
                    System.out.print("Enter account ID: ");
                    String account = scanner.nextLine();
                    int newId = processor.transactions.size() + 1;
                    processor.addTransaction(new Transaction(
                            newId, amt, merchant, account,
                            System.currentTimeMillis()));
                    System.out.println("✅ Transaction TXN#"
                            + newId + " added!");
                    break;

                case 6:
                    processor.displayTransactions();
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