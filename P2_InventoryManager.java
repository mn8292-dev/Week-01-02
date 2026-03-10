import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class P2_InventoryManager {

    // HashMap<productId, stockCount> for O(1) stock lookup
    private HashMap<String, Integer> inventory;

    // HashMap<productId, productName> for product details
    private HashMap<String, String> productNames;

    // LinkedHashMap<productId, Queue<userId>> for FIFO waiting list
    private HashMap<String, Queue<String>> waitingList;

    // HashMap to track total purchases per product
    private HashMap<String, Integer> totalPurchases;

    /**
     * Constructor - initializes inventory with flash sale products
     */
    public P2_InventoryManager() {
        inventory      = new HashMap<>();
        productNames   = new HashMap<>();
        waitingList    = new HashMap<>();
        totalPurchases = new HashMap<>();

        // Pre-populate flash sale inventory
        addProduct("IPHONE15_256GB", "iPhone 15 256GB", 100);
        addProduct("PS5_CONSOLE",    "PlayStation 5",   50);
        addProduct("MACBOOK_M3",     "MacBook Pro M3",  30);
        addProduct("AIRPODS_PRO",    "AirPods Pro",     200);
    }

    /**
     * Add a product to inventory.
     *
     * @param productId   unique product ID
     * @param productName display name
     * @param stock       initial stock count
     */
    public void addProduct(String productId, String productName, int stock) {
        inventory.put(productId, stock);
        productNames.put(productId, productName);
        waitingList.put(productId, new java.util.LinkedList<>());
        totalPurchases.put(productId, 0);
    }

    /**
     * Check stock for a product in O(1) time.
     *
     * @param productId the product to check
     * @return stock count, or -1 if product not found
     */
    public int checkStock(String productId) {
        return inventory.getOrDefault(productId, -1);
    }

    /**
     * Process a purchase request in O(1) time.
     * Adds to waiting list if out of stock.
     *
     * @param productId the product to purchase
     * @param userId    the user making the purchase
     */
    public void purchaseItem(String productId, String userId) {

        if (!inventory.containsKey(productId)) {
            System.out.println("❌ Product \"" + productId + "\" not found!");
            return;
        }

        int stock = inventory.get(productId);

        if (stock > 0) {
            // Decrement stock atomically
            inventory.put(productId, stock - 1);
            totalPurchases.put(productId,
                    totalPurchases.get(productId) + 1);

            System.out.println("✅ Purchase successful!");
            System.out.println("   Product : " + productNames.get(productId));
            System.out.println("   User    : " + userId);
            System.out.println("   Stock   : " + (stock - 1) + " units remaining");

        } else {
            // Add to FIFO waiting list
            waitingList.get(productId).add(userId);
            int position = waitingList.get(productId).size();

            System.out.println("⏳ Out of stock!");
            System.out.println("   Product  : " + productNames.get(productId));
            System.out.println("   User     : " + userId);
            System.out.println("   Waitlist : Position #" + position);
        }
    }

    /**
     * Restock a product and notify waiting list users.
     *
     * @param productId the product to restock
     * @param quantity  units to add
     */
    public void restockProduct(String productId, int quantity) {

        if (!inventory.containsKey(productId)) {
            System.out.println("❌ Product not found!");
            return;
        }

        int currentStock = inventory.get(productId);
        inventory.put(productId, currentStock + quantity);

        System.out.println("📦 Restocked \"" + productNames.get(productId)
                + "\" with " + quantity + " units.");

        // Notify users from waiting list
        Queue<String> waiting = waitingList.get(productId);
        int notified = 0;
        while (!waiting.isEmpty() && notified < quantity) {
            String userId = waiting.poll();
            System.out.println("🔔 Notified user: " + userId
                    + " - Item available!");
            notified++;
        }
    }

    /**
     * Display full inventory status.
     */
    public void displayInventory() {
        System.out.println("\n========== Flash Sale Inventory ==========");
        System.out.printf("%-20s %-25s %-8s %-10s %-10s%n",
                "Product ID", "Name", "Stock", "Sold", "Waiting");
        System.out.println("----------------------------------------------------------"
                + "------------");

        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String pid = entry.getKey();
            System.out.printf("%-20s %-25s %-8d %-10d %-10d%n",
                    pid,
                    productNames.get(pid),
                    entry.getValue(),
                    totalPurchases.get(pid),
                    waitingList.get(pid).size());
        }
        System.out.println("==========================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P2_InventoryManager manager = new P2_InventoryManager();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("   E-commerce Flash Sale Inventory Manager");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Check Stock");
            System.out.println("2. Purchase Item");
            System.out.println("3. Restock Product");
            System.out.println("4. Display Full Inventory");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter Product ID: ");
                    String pid = scanner.nextLine();
                    int stock = manager.checkStock(pid);
                    if (stock == -1)
                        System.out.println("❌ Product not found!");
                    else
                        System.out.println("📦 Stock for \"" + pid
                                + "\": " + stock + " units");
                    break;

                case 2:
                    System.out.print("Enter Product ID: ");
                    String productId = scanner.nextLine();
                    System.out.print("Enter User ID: ");
                    String userId = scanner.nextLine();
                    manager.purchaseItem(productId, userId);
                    break;

                case 3:
                    System.out.print("Enter Product ID: ");
                    String restockId = scanner.nextLine();
                    System.out.print("Enter quantity to restock: ");
                    int qty = scanner.nextInt();
                    scanner.nextLine();
                    manager.restockProduct(restockId, qty);
                    break;

                case 4:
                    manager.displayInventory();
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