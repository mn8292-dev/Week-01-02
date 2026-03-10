import java.util.Scanner;

public class P8_ParkingLot {

    // -------------------------------------------------------
    // Spot Status enum
    // -------------------------------------------------------
    enum SpotStatus {
        EMPTY, OCCUPIED, DELETED
    }

    // -------------------------------------------------------
    // ParkingSpot class - stores vehicle metadata
    // -------------------------------------------------------
    static class ParkingSpot {
        int        spotNumber;
        String     licensePlate;
        long       entryTime;
        SpotStatus status;
        int        probeCount;    // number of probes to park here

        ParkingSpot(int spotNumber) {
            this.spotNumber   = spotNumber;
            this.licensePlate = null;
            this.entryTime    = 0;
            this.status       = SpotStatus.EMPTY;
            this.probeCount   = 0;
        }
    }

    // -------------------------------------------------------
    // Billing Receipt class
    // -------------------------------------------------------
    static class Receipt {
        String licensePlate;
        int    spotNumber;
        long   entryTime;
        long   exitTime;
        double durationHours;
        double fee;

        Receipt(String plate, int spot,
                long entry, long exit) {
            this.licensePlate  = plate;
            this.spotNumber    = spot;
            this.entryTime     = entry;
            this.exitTime      = exit;
            this.durationHours = (exit - entry) / 3600000.0;
            this.fee           = Math.max(0.5,
                    Math.ceil(durationHours * 4) * 1.25); // $1.25 per 15 min
        }
    }

    // Parking lot configuration
    private static final int    TOTAL_SPOTS   = 20;  // small for demo
    private static final double RATE_PER_HOUR = 5.0; // $5 per hour

    // Array-based hash table with open addressing
    private ParkingSpot[] spots;

    // Statistics
    private int totalParked;
    private int totalProbes;
    private int currentOccupancy;
    private double totalRevenue;
    private int peakOccupancy;

    /**
     * Constructor - initializes parking lot
     */
    public P8_ParkingLot() {
        spots           = new ParkingSpot[TOTAL_SPOTS];
        totalParked     = 0;
        totalProbes     = 0;
        currentOccupancy = 0;
        totalRevenue    = 0;
        peakOccupancy   = 0;

        // Initialize all spots as EMPTY
        for (int i = 0; i < TOTAL_SPOTS; i++) {
            spots[i] = new ParkingSpot(i + 1);
        }
    }

    /**
     * Custom hash function for license plate.
     * Maps license plate string to a spot index.
     *
     * @param licensePlate vehicle license plate
     * @return preferred spot index (0 to TOTAL_SPOTS-1)
     */
    private int hashFunction(String licensePlate) {
        int hash = 0;
        for (char c : licensePlate.toCharArray()) {
            hash = (hash * 31 + c) % TOTAL_SPOTS;
        }
        return Math.abs(hash);
    }

    /**
     * Park a vehicle using linear probing for collision resolution.
     *
     * @param licensePlate vehicle license plate
     */
    public void parkVehicle(String licensePlate) {

        // Check if lot is full (load factor >= 0.8)
        double loadFactor = (double) currentOccupancy / TOTAL_SPOTS;
        if (loadFactor >= 0.9) {
            System.out.println("🚫 Parking lot is full! "
                    + "(" + currentOccupancy + "/" + TOTAL_SPOTS + " spots)");
            return;
        }

        // Check if already parked
        if (findVehicle(licensePlate) != -1) {
            System.out.println("⚠️  Vehicle " + licensePlate
                    + " is already parked at spot #"
                    + (findVehicle(licensePlate) + 1));
            return;
        }

        // Get preferred spot using hash function
        int preferredIndex = hashFunction(licensePlate);
        int currentIndex   = preferredIndex;
        int probes         = 0;

        // Linear probing - find next available spot
        while (spots[currentIndex].status == SpotStatus.OCCUPIED) {
            currentIndex = (currentIndex + 1) % TOTAL_SPOTS;
            probes++;

            // Safety check - full loop
            if (probes >= TOTAL_SPOTS) {
                System.out.println("🚫 No available spots!");
                return;
            }
        }

        // Park the vehicle
        spots[currentIndex].licensePlate = licensePlate;
        spots[currentIndex].entryTime    = System.currentTimeMillis();
        spots[currentIndex].status       = SpotStatus.OCCUPIED;
        spots[currentIndex].probeCount   = probes;

        totalParked++;
        totalProbes += probes;
        currentOccupancy++;
        peakOccupancy = Math.max(peakOccupancy, currentOccupancy);

        System.out.println("🚗 Parked: " + licensePlate);
        System.out.println("   Preferred Spot : #"
                + (preferredIndex + 1));
        System.out.println("   Assigned Spot  : #"
                + spots[currentIndex].spotNumber
                + " (" + probes + " probe"
                + (probes == 1 ? "" : "s") + ")");
        System.out.printf("   Load Factor    : %.0f%%%n",
                loadFactor * 100);
    }

    /**
     * Exit a vehicle and calculate billing.
     *
     * @param licensePlate vehicle license plate
     */
    public void exitVehicle(String licensePlate) {
        int index = findVehicle(licensePlate);

        if (index == -1) {
            System.out.println("❌ Vehicle " + licensePlate
                    + " not found in parking lot!");
            return;
        }

        ParkingSpot spot = spots[index];
        long exitTime    = System.currentTimeMillis();

        // Calculate billing
        double durationMs    = exitTime - spot.entryTime;
        double durationHours = durationMs / 3600000.0;
        double fee = Math.max(0.5,
                Math.ceil(durationHours * 4) * (RATE_PER_HOUR / 4));

        totalRevenue += fee;
        currentOccupancy--;

        // Mark as DELETED (not EMPTY) for open addressing integrity
        spot.status       = SpotStatus.DELETED;
        spot.licensePlate = null;

        System.out.println("🚙 Exited: " + licensePlate);
        System.out.println("   Spot        : #" + spot.spotNumber);
        System.out.printf("   Duration    : %.1f minutes%n",
                durationMs / 60000.0);
        System.out.printf("   Fee         : $%.2f%n", fee);
        System.out.println("   Total Revenue: $"
                + String.format("%.2f", totalRevenue));
    }

    /**
     * Find vehicle index using linear probing search.
     *
     * @param licensePlate vehicle to find
     * @return index in spots array, or -1 if not found
     */
    private int findVehicle(String licensePlate) {
        int startIndex = hashFunction(licensePlate);
        int index      = startIndex;
        int probes     = 0;

        while (spots[index].status != SpotStatus.EMPTY) {
            if (spots[index].status == SpotStatus.OCCUPIED
                    && licensePlate.equals(spots[index].licensePlate)) {
                return index;
            }
            index = (index + 1) % TOTAL_SPOTS;
            probes++;
            if (probes >= TOTAL_SPOTS) break;
        }
        return -1;
    }

    /**
     * Display current parking lot map.
     */
    public void displayParkingMap() {
        System.out.println("\n========== Parking Lot Map ==========");
        System.out.printf("%-6s %-15s %-10s%n",
                "Spot", "License Plate", "Status");
        System.out.println("-------------------------------------");

        for (ParkingSpot spot : spots) {
            String plate  = spot.status == SpotStatus.OCCUPIED
                    ? spot.licensePlate : "-";
            String status = spot.status == SpotStatus.OCCUPIED
                    ? "🚗 OCCUPIED"
                    : spot.status == SpotStatus.DELETED
                    ? "🔄 FREED" : "✅ EMPTY";

            System.out.printf("%-6d %-15s %-10s%n",
                    spot.spotNumber, plate, status);
        }

        System.out.println("=====================================");
        System.out.println("Occupancy: " + currentOccupancy
                + "/" + TOTAL_SPOTS);
        System.out.printf("Load Factor: %.0f%%%n",
                (currentOccupancy * 100.0 / TOTAL_SPOTS));
    }

    /**
     * Display parking lot statistics.
     */
    public void displayStatistics() {
        System.out.println("\n========== Parking Statistics ==========");
        System.out.println("Total Spots     : " + TOTAL_SPOTS);
        System.out.println("Current Occupancy: " + currentOccupancy
                + "/" + TOTAL_SPOTS);
        System.out.printf("Load Factor     : %.0f%%%n",
                (currentOccupancy * 100.0 / TOTAL_SPOTS));
        System.out.println("Total Parked    : " + totalParked);
        System.out.println("Peak Occupancy  : " + peakOccupancy);
        double avgProbes = totalParked > 0
                ? (double) totalProbes / totalParked : 0;
        System.out.printf("Avg Probes      : %.1f%n", avgProbes);
        System.out.printf("Total Revenue   : $%.2f%n", totalRevenue);
        System.out.println("Rate            : $"
                + RATE_PER_HOUR + "/hour");
        System.out.println("=========================================");
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {

        P8_ParkingLot lot = new P8_ParkingLot();
        Scanner scanner   = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println("   Parking Lot Management System");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Park Vehicle");
            System.out.println("2. Exit Vehicle");
            System.out.println("3. Display Parking Map");
            System.out.println("4. Display Statistics");
            System.out.println("5. Exit Program");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter License Plate: ");
                    String plate = scanner.nextLine().toUpperCase();
                    lot.parkVehicle(plate);
                    break;

                case 2:
                    System.out.print("Enter License Plate: ");
                    String exitPlate = scanner.nextLine().toUpperCase();
                    lot.exitVehicle(exitPlate);
                    break;

                case 3:
                    lot.displayParkingMap();
                    break;

                case 4:
                    lot.displayStatistics();
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