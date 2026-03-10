import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class P1_UsernameChecker {

    // HashMap for registered usernames → userId mapping
    private HashMap<String, Integer> registeredUsers;

    // HashMap to track how many times a username was attempted
    private HashMap<String, Integer> attemptFrequency;

    // Auto-increment userId counter
    private int userIdCounter;

    /**
     * Constructor - initializes hash tables with pre-registered users
     */
    public P1_UsernameChecker() {
        registeredUsers = new HashMap<>();
        attemptFrequency = new HashMap<>();
        userIdCounter = 1;

        // Pre-populate with some existing users
        registeredUsers.put("john_doe",    userIdCounter++);
        registeredUsers.put("admin",       userIdCounter++);
        registeredUsers.put("jane_smith",  userIdCounter++);
        registeredUsers.put("pranav",      userIdCounter++);
        registeredUsers.put("user123",     userIdCounter++);
    }

    /**
     * Check if a username is available in O(1) time.
     *
     * @param username the username to check
     * @return true if available, false if taken
     */
    public boolean checkAvailability(String username) {

        // Track attempt frequency
        attemptFrequency.put(username,
                attemptFrequency.getOrDefault(username, 0) + 1);

        // O(1) lookup using HashMap
        return !registeredUsers.containsKey(username);
    }

    /**
     * Register a new username if available.
     *
     * @param username the username to register
     * @return true if registered successfully, false if taken
     */
    public boolean registerUsername(String username) {
        if (checkAvailability(username)) {
            registeredUsers.put(username, userIdCounter++);
            System.out.println("✅ Username \"" + username
                    + "\" registered successfully! (UserID: "
                    + (userIdCounter - 1) + ")");
            return true;
        }
        System.out.println("❌ Username \"" + username + "\" is already taken.");
        return false;
    }

    /**
     * Suggest alternative usernames if the requested one is taken.
     * Appends numbers and modifies characters for suggestions.
     *
     * @param username the taken username
     * @return list of available alternative usernames
     */
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();

        // Suggestion 1: Append numbers (username1, username2, username3)
        for (int i = 1; i <= 3; i++) {
            String suggestion = username + i;
            if (checkAvailability(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        // Suggestion 2: Replace underscore with dot (john_doe → john.doe)
        String dotVersion = username.replace("_", ".");
        if (!dotVersion.equals(username) && checkAvailability(dotVersion)) {
            suggestions.add(dotVersion);
        }

        // Suggestion 3: Add underscore at end
        String underscoreVersion = username + "_";
        if (checkAvailability(underscoreVersion)) {
            suggestions.add(underscoreVersion);
        }

        // Suggestion 4: Add current year
        String yearVersion = username + "_2025";
        if (checkAvailability(yearVersion)) {
            suggestions.add(yearVersion);
        }

        return suggestions;
    }

    /**
     * Get the most attempted username (most searched).
     *
     * @return the most attempted username
     */
    public String getMostAttempted() {
        String mostAttempted = null;
        int maxAttempts = 0;

        for (Map.Entry<String, Integer> entry : attemptFrequency.entrySet()) {
            if (entry.getValue() > maxAttempts) {
                maxAttempts = entry.getValue();
                mostAttempted = entry.getKey();
            }
        }
        return mostAttempted + " (" + maxAttempts + " attempts)";
    }

    /**
     * Display all registered users.
     */
    public void displayAllUsers() {
        System.out.println("\n--- Registered Users ---");
        for (Map.Entry<String, Integer> entry : registeredUsers.entrySet()) {
            System.out.println("Username: " + entry.getKey()
                    + " | UserID: " + entry.getValue());
        }
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P1_UsernameChecker checker = new P1_UsernameChecker();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("  Social Media Username Availability Checker");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Check Username Availability");
            System.out.println("2. Register Username");
            System.out.println("3. Suggest Alternatives");
            System.out.println("4. Get Most Attempted Username");
            System.out.println("5. Display All Users");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter username to check: ");
                    String username = scanner.nextLine();
                    boolean available = checker.checkAvailability(username);
                    System.out.println("\"" + username + "\" is "
                            + (available ? "✅ AVAILABLE" : "❌ TAKEN"));
                    break;

                case 2:
                    System.out.print("Enter username to register: ");
                    String newUser = scanner.nextLine();
                    checker.registerUsername(newUser);
                    break;

                case 3:
                    System.out.print("Enter taken username for suggestions: ");
                    String takenUser = scanner.nextLine();
                    List<String> suggestions = checker.suggestAlternatives(takenUser);
                    System.out.println("Suggested alternatives:");
                    for (String s : suggestions) {
                        System.out.println("  → " + s);
                    }
                    break;

                case 4:
                    System.out.println("Most attempted: "
                            + checker.getMostAttempted());
                    break;

                case 5:
                    checker.displayAllUsers();
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