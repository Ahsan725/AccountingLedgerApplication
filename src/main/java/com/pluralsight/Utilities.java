package com.pluralsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

/**
 * # Utilities: CLI + helper utilities for the ledger application.
 * <p>
 * Responsibilities:
 * - Bootstraps a console application flow (menus, input handling).
 * - Loads/saves transactions to a pipe-delimited CSV file: transactions.csv.
 * - Loads user profiles from profiles.csv and authenticates a user.
 * - Enforces per-user visibility (non-admins see only their records; admins see all).
 * - Provides report/search views (date ranges, vendor, description, custom filters).
 * <p>
 * <p>
 * Notes:
 * Amounts: deposits are stored as positive numbers; payments (debits) as negative numbers.
 * Types: in this code, "credit" = payment (amount < 0) and "debit" = deposit (amount > 0).
 * This is intentionally mirrored in printByTypeSorted(String) and the menu labels.
 * Sorting: most views sort by date/time descending (newest first)
 * Duplicates: a Set<Transaction> called seen prevents duplicate inserts.
 * <p>
 * <p>
 * This class is final with a private constructor which means it's a static utility holder only.
 * Console input uses one shared Scanner to avoid repeatedly passing it around.
 *
 */
public final class Utilities {

    // Comparator for newest-first ordering (date, then time, reversed).
    private static final Comparator<Transaction> BY_DATETIME_DESCENDING =
            Comparator.comparing(Transaction::getDate)
                    .thenComparing(Transaction::getTime)
                    .reversed();

    // Static, process-wide resources
    // Shared scanner for console input to avoid passing it around.
    static Scanner sc = new Scanner(System.in);

    // Transactions CSV file (pipe-delimited: userid|date|time|description|vendor|amount).
    static String fileName = "transactions.csv";

    // The in-memory ledger that loads transactions from the file and adds new transactions go here.
    static List<Transaction> ledger = DataStore.ledger;

    // Profiles CSV file (pipe-delimited: userid|name|pin|access).
    static String profilesFileName = "profiles.csv";

    // The user currently authenticated through the CLI.
    static User currentUser;

    // Quick lookup from user id to User loaded from the profiles.
    static HashMap<Integer, User> idToUser = new HashMap<>();

    //Tracks transactions we've already seen (from file or created this session).
    //Used to prevent duplicates when re-reading/saving/logging in and out new users.
    static Set<Transaction> seen = new HashSet<>(ledger);

    // Private constructor to prevent instantiation (utility class)
    private Utilities() {
    }

    /**
     * Entry point for the CLI App.
     * - Loads users and transactions from the file
     * - Authenticates a user
     * - Shows the main menu loop: deposit, payment, ledger, logout, exit.
     */
    public static void startCliApplication() {
        readUsersFromFile();              // Load users into idToUser
        readFromFileAndAddToLedger();     // Load transactions into ledger + seen
        setCurrentUser();                 // Authenticate a user and set them as the current user

        char operation = ' ';
        while (operation != 'x') {
            System.out.println("""
                    
                    Welcome to the Ledger!
                    What would you like to do?
                    D) Add Deposits
                    P) Make Payment (Debit)
                    L) Ledger
                    O) Log Out
                    X) Exit
                    Enter command:
                    """);
            // I trim, lowercase, and take first char of the user input
            operation = sc.nextLine().toLowerCase().charAt(0);

            switch (operation) {
                case 'd' -> {
                    // Deposits are positive. Takes in a boolean argument: depositOnly
                    performTransaction(true);
                }
                case 'p' -> {
                    // Payments are negative
                    performTransaction(false);
                }
                case 'l' -> ledgerMenu(); //displays the ledger menu
                case 'o' -> logOut(); //logs a user out and lets another user log in
                case 'x' -> System.out.println("Exiting...");
                default -> System.out.println("Invalid operation... Try again or press X to quit");
            }
        }
    }

    /**
     * Prompts for user id + PIN until a valid match is found.
     * - Numeric validation for user id.
     * - PIN match against profiles file.
     * - Sets currentUser on success.
     */
    private static void setCurrentUser() {
        while (true) {
            try {
                System.out.print("Welcome! Enter your user id: ");
                int userId = Integer.parseInt(sc.nextLine().trim());

                User user = idToUser.get(userId);
                if (user == null) {
                    System.out.println("No such user id. Try again.");
                    continue;
                }

                System.out.print("Enter your PIN: ");
                String pinStr = sc.nextLine().trim();

                if (!user.getPin().equals(pinStr)) {
                    System.out.println("Incorrect PIN. Try again.");
                    continue;
                }

                currentUser = user;
                System.out.println("Hello, " + currentUser.getName()
                        + (currentUser.isAdminAccess() ? " (admin)" : "") + "!");
                break;

            } catch (NumberFormatException nfe) {
                System.out.println("User id must be numeric. Try again.");
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
            }
        }
    }

    /**
     * Loads user profiles from profilesFileName into idToUser.
     * Skips optional headers wherever they appear and ignores bad rows. I did this to ensure that the application
     * does not break if one of the line was added incorrectly.
     */
    private static void readUsersFromFile() {
        idToUser.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(profilesFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty()) continue;

                String[] userFields = s.split("\\|", -1);

                // Header-tolerant because it just skips header even if not at the top. Some reports generating programs
                //always spit out headers in my experience, and can cause problems while parsing.
                if (userFields.length >= 4
                        && userFields[0].trim().equalsIgnoreCase("userid")
                        && userFields[1].trim().equalsIgnoreCase("name")
                        && userFields[2].trim().equalsIgnoreCase("pin")
                        && userFields[3].trim().equalsIgnoreCase("access")) {
                    continue;
                }

                // Require at least id | name | pin so admin access is optional since every new User has no Admin
                //access by default
                if (userFields.length < 3) continue;

                try {
                    int id = Integer.parseInt(userFields[0].trim());
                    String name = userFields[1].trim();
                    String pin = userFields[2].trim();

                    boolean isAdmin = false;
                    if (userFields.length >= 4) {
                        // Accepts "true"/"false" in any case
                        isAdmin = Boolean.parseBoolean(userFields[3].trim());
                    }

                    User u = new User(id, name, pin, isAdmin);
                    idToUser.put(id, u);
                } catch (Exception ignore) {
                    // On bad data, skip the row
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: profiles file not found: " + profilesFileName);
        } catch (IOException e) {
            System.err.println("I/O error reading " + profilesFileName + ": " + e.getMessage());
        }
    }

    // true if a user is logged in and has admin access
    private static boolean isAdmin() {
        return currentUser != null && currentUser.isAdminAccess();
    }

    /**
     * Enforces visibility rules:
     * - Admins: can see all transactions.
     * - Non-admins: only transactions whose userId matches their own.
     */
    private static boolean canView(Transaction record) {
        return isAdmin() || (currentUser != null && record.getUserId() == currentUser.getId());
    }

    /**
     * Filters the global ledger down to only the transactions the current user is
     * allowed to see, then sorts newest-first.
     */
    private static List<Transaction> visibleSorted() {
        return ledger.stream()
                .filter(Utilities::canView)
                .sorted(BY_DATETIME_DESCENDING)
                .toList();
    }

    /**
     * Logs out the current user and restarts the CLI to authenticate again.
     * Simple flow reset via recursion into startCliApplication()
     */
    private static void logOut() {
        System.out.println("Logging out... " + currentUser.getName());
        currentUser = null;
        startCliApplication();
    }

    /**
     * Displays the "Ledger" submenu (all/deposits/payments/reports/back) and routes
     * to the appropriate view. The loop continues until the user selects 'H'.
     */
    private static void ledgerMenu() {
        char operation = ' ';
        while (operation != 'h') {

            System.out.println("""
                    
                    LEDGER MENU
                    What would you like to do?
                    A) View All Transactions
                    D) View Deposits Only
                    P) View Payments Only
                    R) View Reports
                    H) Return to Home
                    Enter command:
                    """);
            operation = sc.nextLine().toLowerCase().trim().charAt(0);

            switch (operation) {
                case 'a' -> printByTypeSorted("all");
                case 'd' -> printByTypeSorted("debit");   // "debit" == deposits (amount > 0)
                case 'p' -> printByTypeSorted("credit");  // "credit" == payments  (amount < 0)
                case 'r' -> reportsMenu();
                case 'h' -> System.out.println("Exiting...");
                default -> System.out.println("Invalid operation... Try again or press X to quit");
            }
        }
    }

    /**
     * Displays the reports menu and runs the chosen report.
     * Uses Scanner nextInt() and then consumes the trailing newline when returning.
     */
    private static void reportsMenu() {
        int operation = -1;
        while (operation != 0) {

            System.out.println("""
                    
                    REPORT MENU
                    What would you like to do?
                    1) Month To Date
                    2) Previous Month
                    3) Year To Date
                    4) Previous Year
                    5) Search By Vendor
                    6) Custom Search
                    0) Back
                    Enter command:
                    """);
            operation = sc.nextInt();
            switch (operation) {
                case 1 -> printMonthToDate();
                case 2 -> printPreviousMonth();
                case 3 -> printYearToDate();
                case 4 -> printPreviousYear();
                case 5 -> searchByVendor();
                case 6 -> searchMenu();
                case 0 -> {
                    // Consume newline so the next menu read works with nextLine()
                    if (sc.hasNextLine()) sc.nextLine();
                    System.out.println("Exiting...");
                }
                default -> System.out.println("Invalid operation... Try again or press 0 to quit");
            }
        }
    }

    /**
     * Shows the search submenu (vendor, description, full custom) and dispatches to the handlers.
     * Uses Scanner#nextInt() then consumes trailing newline on exit.
     */
    private static void searchMenu() {
        int operation = -1;
        while (operation != 0) {

            System.out.println("""
                    
                    SEARCH MENU
                    What would you like to search by?
                    1) Vendor Name
                    2) Transaction Description
                    3) Search for a specific transaction
                    0) Back
                    Enter command:
                    """);
            operation = sc.nextInt();
            switch (operation) {
                case 1 -> searchByVendor();
                case 2 -> searchByDescription();
                case 3 -> customSearch();
                case 0 -> {
                    if (sc.hasNextLine()) sc.nextLine();
                    System.out.println("Exiting... ");
                }
                default -> System.out.println("Invalid operation... Try again or press 0 to quit");
            }
        }
    }

    /**
     * Fully parameterized search across (optional) start/end date, description, vendor, and exact amount.
     * - All filters are optional; blank input is skipped
     * - Always respects visibility—filters are applied after restricting to what the current user can see.
     * - Sorts newest-first before iterating.
     */
    private static void customSearch() {
        // Clear leftover newline from prior nextInt/nextDouble before using nextLine()
        if (sc.hasNextLine()) sc.nextLine();

        System.out.println("CUSTOM SEARCH");

        System.out.print("Start Date (YYYY-MM-DD) or leave it blank: ");
        String startDateInput = sc.nextLine().trim();

        System.out.print("End Date (YYYY-MM-DD) or leave it blank: ");
        String endDateInput = sc.nextLine().trim();

        System.out.print("Description contains (blank to skip): ");
        String descriptionInput = sc.nextLine().trim();

        System.out.print("Vendor contains (blank to skip): ");
        String vendorInput = sc.nextLine().trim();

        System.out.print("Amount or leave it blank: ");
        String amountInput = sc.nextLine().trim();

        // this is where I parse optional filters absorb parse errors silently to keep UX simple
        LocalDate startDate = null, endDate = null;
        Double amountQuery = null;
        try {
            if (!startDateInput.isEmpty()) startDate = LocalDate.parse(startDateInput);
        } catch (Exception ignored) {
        }
        try {
            if (!endDateInput.isEmpty()) endDate = LocalDate.parse(endDateInput);
        } catch (Exception ignored) {
        }
        try {
            if (!amountInput.isEmpty()) amountQuery = Double.parseDouble(amountInput);
        } catch (Exception ignored) {
        }

        String descriptionQuery = descriptionInput.isEmpty() ? null : descriptionInput.toLowerCase();
        String vendorQuery = vendorInput.isEmpty() ? null : vendorInput.toLowerCase();

        // Visibility first -> then sort -> then apply filters in one pass
        List<Transaction> visible = ledger.stream()
                .filter(Utilities::canView)           // admin => all; user => only their userId
                .sorted(BY_DATETIME_DESCENDING)       // newest first
                .toList();

        boolean anyPrinted = false;

        for (Transaction record : visible) {
            LocalDate d = record.getDate();

            if (startDate != null && d.isBefore(startDate)) continue;
            if (endDate != null && d.isAfter(endDate)) continue;

            if (descriptionQuery != null &&
                    !record.getDescription().toLowerCase().contains(descriptionQuery)) continue;

            if (vendorQuery != null &&
                    !record.getVendor().toLowerCase().contains(vendorQuery)) continue;

            if (amountQuery != null && record.getAmount() != amountQuery) continue;

            printFormatted(record);
            anyPrinted = true;
        }

        if (!anyPrinted)
            System.out.println("No transactions match your filters."); //if nothing was printed let the user know
    }

    // Convenience wrapper for vendor-only search
    private static void searchByVendor() {
        searchByField(Transaction::getVendor, "vendor name");
    }

    /**
     * General-use case-insensitive search on a given string field of Transaction.
     * - It takes field getter function (vendor or description) as a parameter
     * - label used to prompt the user
     */
    private static void searchByField(Function<Transaction, String> getter, String prompt) {
        if (sc.hasNextLine()) sc.nextLine(); // Clear newline from a prior nextInt/nextDouble
        System.out.print("Enter " + prompt + ": ");
        String query = sc.nextLine().trim().toLowerCase();

        // Restrict to visible rows, newest-first
        List<Transaction> visible = ledger.stream()
                .filter(Utilities::canView)
                .sorted(BY_DATETIME_DESCENDING)
                .toList();

        boolean any = false;
        for (Transaction t : visible) {
            String field = getter.apply(t);
            if (field != null && field.toLowerCase().contains(query)) {
                printFormatted(t);
                any = true;
            }
        }
        if (!any) System.out.println("No matching transactions.");
    }

    // description-only search.
    private static void searchByDescription() {
        searchByField(Transaction::getDescription, "transaction description");
    }

    //Prints all transactions from the previous calendar year (inclusive)
    private static void printPreviousYear() {
        LocalDate today = LocalDate.now();
        int prevYear = today.getYear() - 1;
        LocalDate firstDayPrevYear = LocalDate.of(prevYear, 1, 1);
        LocalDate lastDayPrevYear = LocalDate.of(prevYear, 12, 31);
        printByDuration(firstDayPrevYear, lastDayPrevYear);
    }

    // Prints all transactions from Jan 1 of the current year through today (inclusive).
    private static void printYearToDate() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        printByDuration(firstDayOfYear, today);
    }

    // Prints all transactions from the previous calendar month (inclusive)
    private static void printPreviousMonth() {
        LocalDate today = LocalDate.now();
        // YearMonth is ideal for calendar arithmetic (avoids manual month edge cases)
        java.time.YearMonth prev = java.time.YearMonth.from(today).minusMonths(1);
        LocalDate firstOfPrevious = prev.atDay(1);
        LocalDate lastOfPrevious = prev.atEndOfMonth();

        printByDuration(firstOfPrevious, lastOfPrevious);
    }

    // Prints all transactions from the start of this month through today (inclusive)
    private static void printMonthToDate() {
        LocalDate today = LocalDate.now();
        LocalDate month = today.withDayOfMonth(1);
        printByDuration(month, today);
    }

    /**
     * Prints all visible transactions between start and end inclusive, newest-first.
     * If the caller passes reversed bounds, they are swapped
     */
    private static void printByDuration(LocalDate start, LocalDate end) {
        // Normalize bounds if reversed
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        System.out.println("Displaying transactions between " + start + " and " + end);

        // Only transactions the current user can see which is already sorted newest-first
        List<Transaction> view = visibleSorted();

        for (Transaction t : view) {
            LocalDate d = t.getDate();
            // inclusive range: start <= d <= end
            if (!d.isBefore(start) && !d.isAfter(end)) {
                printFormatted(t);
            }
        }
    }

    /**
     * Prints transactions filtered by "type":
     * - "credit": payments (amount < 0)
     * - "debit" : deposits (amount > 0)
     * - anything else: all
     * Always respects visibility and prints newest-first.
     */
    private static void printByTypeSorted(String transactionType) {
        // Normalize input; anything else falls back to "all"
        String type = (transactionType == null) ? "all" : transactionType.toLowerCase();

        // Only the transactions the current user can see, already sorted newest-first
        List<Transaction> view = visibleSorted();

        switch (type) {
            case "credit": // payments (amount < 0)
                for (Transaction t : view) {
                    if (t.getAmount() < 0) printFormatted(t);
                }
                break;

            case "debit":  // deposits (amount > 0)
                for (Transaction t : view) {
                    if (t.getAmount() > 0) printFormatted(t);
                }
                break;

            default:       // "all"
                for (Transaction t : view) {
                    printFormatted(t);
                }
        }
    }

    /**
     * Prompts for description, vendor, and amount, then creates a deposit or payment.
     * - If depositOnly is true, the amount is forced positive
     * - Otherwise, the amount is forced negative (payment)
     * - Saves to file and prints a confirmation message
     */
    private static void performTransaction(boolean depositOnly) {
        System.out.println(depositOnly ? "DEPOSIT SCREEN" : "PAYMENT SCREEN");

        System.out.print("Enter the Transaction Description: ");
        String description = sc.nextLine();

        System.out.print("Enter the name of the vendor: ");
        String vendor = sc.nextLine();

        // keep prompting until we get a valid double,
        // then normalize sign based on deposit/payment mode.
        double amount;
        while (true) {
            System.out.print("Enter the amount: ");
            if (sc.hasNextDouble()) {
                double entered = sc.nextDouble();
                sc.nextLine(); // consume newline left by nextDouble()
                amount = depositOnly ? Math.abs(entered) : -Math.abs(entered);
                break;
            } else {
                System.out.println("Invalid number. Please enter a numeric amount (e.g., 123.45 or -67.89).");
                sc.nextLine(); // discard invalid token
            }
        }

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0); // seconds precision only

        // Important: persist the user id from the authenticated user
        Transaction record = new Transaction(date, time, description, vendor, amount, currentUser.getId());

        // Only adds to memory if not seen before and also avoids duplicate file writes during load
        if (seen.add(record)) {
            ledger.add(record);
        }

        writeToFile(record);

        System.out.printf("%s added successfully! (Amount: %,.2f)%n",
                depositOnly ? "Deposit" : "Payment", amount);
    }

    /**
     * Appends a single transaction row to fileName
     * Format: userid|date|time|description|vendor|amount
     */
    private static void writeToFile(Transaction record) {
        if (fileName == null || fileName.isEmpty()) {
            System.err.println("Output file name is not set. Cannot write.");
            return;
        }
        // I am stacking three writers because each adds something different that I needed for the code
        // FileWriter(file, true) → opens the file for append and handles the low-level file writing
        // BufferedWriter → adds a memory buffer, so I don’t hit the OS for every small write
        // PrintWriter → gives me printf/format() and convenient println() APIs. which is why I can do
        // out.printf(...)

        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // userid|date|time|description|vendor|amount
            out.printf("%d|%s|%s|%s|%s|%.2f%n",
                    record.getUserId(),
                    record.getDate(),
                    record.getTime().toString(),  // seconds precision ensured at creation using withNano(0)
                    record.getDescription(),
                    record.getVendor(),
                    record.getAmount());
        } catch (IOException e) {
            System.err.println("Could not write to file: " + e.getMessage());
        }
    }

    /**
     * Loads transactions from fileName into memory.
     * - Skips headers even if they appear mid-file
     * - Parses rows; malformed rows are skipped with a warning
     * - Uses seen which is a set to avoid duplicates in ledger
     */
    public static void readFromFileAndAddToLedger() {
        // Expect rows like: userid|date|time|description|vendor|amount
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty()) continue;

                String[] t = s.split("\\|", -1);

                // Skip header anywhere (case-insensitive)
                if (t.length >= 6
                        && t[0].trim().equalsIgnoreCase("userid")
                        && t[1].trim().equalsIgnoreCase("date")
                        && t[2].trim().equalsIgnoreCase("time")
                        && t[3].trim().equalsIgnoreCase("description")
                        && t[4].trim().equalsIgnoreCase("vendor")
                        && t[5].trim().equalsIgnoreCase("amount")) {
                    continue;
                }

                if (t.length != 6) {
                    System.err.println("Skipping line: expected 6 fields (userid|date|time|description|vendor|amount)");
                    continue;
                }

                try {
                    int userId = Integer.parseInt(t[0].trim());
                    LocalDate date = LocalDate.parse(t[1].trim()); // YYYY-MM-DD
                    LocalTime time = LocalTime.parse(t[2].trim()); // HH:mm:ss
                    String description = t[3].trim();
                    String vendor = t[4].trim();
                    double amount = Double.parseDouble(t[5].trim());

                    // Use the userId from the file
                    Transaction record = new Transaction(date, time, description, vendor, amount, userId);
                    if (seen.add(record)) { // returns false if duplicate
                        ledger.add(record); // only add if not already present
                    }
                } catch (Exception ex) {
                    System.err.println("Skipping line (bad data): " + ex.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found: " + fileName);
        } catch (IOException e) {
            System.err.println("I/O error reading " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Prints a transaction as a fixed-width table row to the console.
     * Columns: date | description | vendor | amount | type | time
     */
    private static void printFormatted(Transaction record) {
        // date as YYYY-MM-DD; null-safe
        String dateString = (record.getDate() == null) ? "" : record.getDate().toString();

        // Always HH:mm:ss with zero padding (strip nanos if present)
        String timeString = "";
        if (record.getTime() != null) {
            timeString = record.getTime()
                    .withNano(0)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        System.out.printf(
                "%-10s  %-30.30s  %-30.20s  %,30.2f  %-12.6s  %-12s%n",
                dateString,
                record.getDescription(),
                record.getVendor(),
                record.getAmount(),
                record.transactionType(),
                timeString
        );
    }

    // Webserver helping methods

    /**
     * Formats a transaction as pipe-delimited string for web output.
     * 2025-01-15 | Coffee | STARBUCKS         |      -3.45 | CREDIT | 08:15:09}
     */
    public static String formatTransaction(Transaction record) {
        return String.format(
                "%s | %-30s | %-18s | %10.2f | %-6s | %s",
                record.getDate(), record.getDescription(), record.getVendor(),
                record.getAmount(), record.transactionType(), record.getTime()
        );
    }

    //Returns transactions within an inclusive date range, sorted newest-first.
    public static List<Transaction> transactionsByDuration(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        LocalDate finalStart = start;
        LocalDate finalEnd = end;
        return ledger.stream()
                .filter(t -> !t.getDate().isBefore(finalStart) && !t.getDate().isAfter(finalEnd))
                .sorted(BY_DATETIME_DESCENDING)
                .toList();
    }

}
