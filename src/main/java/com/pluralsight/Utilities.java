package com.pluralsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

public final class Utilities {

    //static comparator
    private static final Comparator<Transaction> BY_DATETIME_DESCENDING = Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getTime).reversed();
    //Static variables
    static Scanner sc = new Scanner(System.in); //added a static Scanner to avoid passing it to methods repeatedly
    static String fileName = "transactions.csv";//file we will read from and write to
    static List<Transaction> ledger = DataStore.ledger;
    // at top of Utilities (next to fileName)
    static String profilesFileName = "profiles.csv";
    static User currentUser;
    static HashMap<Integer, User> idToUser = new HashMap<>();
    static Set<Transaction> seen = new HashSet<>(ledger);

    private Utilities() {
    } // constructor that prevents instantiation

    public static void startCliApplication() {
        readUsersFromFile();
        readFromFileAndAddToLedger(); //load transactions into the program
        setCurrentUser();
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
            operation = sc.nextLine().toLowerCase().charAt(0);
            switch (operation) {
                case 'd' -> {
                    //since we are making a deposit we will only allow positive transaction
                    performTransaction(true); //calling helper function with a boolean flag to indicate transaction type
                }
                case 'p' -> {
                    //since we are making a payment we will only allow positive transaction
                    performTransaction(false); //calling helper function with a boolean flag to indicate transaction type
                }
                case 'l' -> ledgerMenu();
                case 'o' -> logOut();
                case 'x' -> {
                    System.out.println("Exiting...");
                }
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press X to quit");

                }
            }

        }
    }

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

    public static void readUsersFromFile() {
        idToUser.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(profilesFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty()) continue;

                String[] userFields = s.split("\\|", -1);

                // Skip header anywhere
                if (userFields.length >= 4
                        && userFields[0].trim().equalsIgnoreCase("userid")
                        && userFields[1].trim().equalsIgnoreCase("name")
                        && userFields[2].trim().equalsIgnoreCase("pin")
                        && userFields[3].trim().equalsIgnoreCase("access")) {
                    continue;
                }

                // Need at least: id | name | pin  (access optional but preferred)
                if (userFields.length < 3) continue;

                try {
                    int id = Integer.parseInt(userFields[0].trim());
                    String name = userFields[1].trim();
                    String pin = userFields[2].trim();

                    boolean isAdmin = false;
                    if (userFields.length >= 4) {
                        // any case: "true"/"false"
                        isAdmin = Boolean.parseBoolean(userFields[3].trim());
                    }

                    User u = new User(id, name, pin, isAdmin);
                    idToUser.put(id, u);
                } catch (Exception ignore) {
                    // bad row -> skip
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: profiles file not found: " + profilesFileName);
        } catch (IOException e) {
            System.err.println("I/O error reading " + profilesFileName + ": " + e.getMessage());
        }
    }

    private static boolean isAdmin() {
        return currentUser != null && currentUser.isAdminAccess();
    }

    private static boolean canView(Transaction record) {
        // Admin sees everything; users see only their own transactions
        return isAdmin() || (currentUser != null && record.getUserId() == currentUser.getId());
    }
    //Filter ledger to what the current user can see, and sort latest first.
    public static List<Transaction> visibleSorted() {
        return ledger.stream()
                .filter(Utilities::canView)
                .sorted(BY_DATETIME_DESCENDING)
                .toList();
    }

    public static void logOut(){
        System.out.println("Logging out... " + currentUser.getName());
        currentUser = null;
        startCliApplication();
    }

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
                case 'd' -> printByTypeSorted("debit");
                case 'p' -> printByTypeSorted("credit");
                case 'r' -> reportsMenu();
                case 'h' -> {
                    System.out.println("Exiting...");
                }
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press X to quit");

                }
            }

        }
    }

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
                    if (sc.hasNextLine()) sc.nextLine();
                    System.out.println("Exiting...");
                }
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press 0 to quit");

                }
            }

        }

    }

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
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press 0 to quit");

                }
            }
        }
    }

    private static void customSearch() {
        // Clear any leftover newline from previous nextInt/nextDouble
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

        LocalDate startDate = null, endDate = null;
        Double amountQuery = null;
        try { if (!startDateInput.isEmpty()) startDate = LocalDate.parse(startDateInput); } catch (Exception ignored) {}
        try { if (!endDateInput.isEmpty())   endDate   = LocalDate.parse(endDateInput);   } catch (Exception ignored) {}
        try { if (!amountInput.isEmpty())    amountQuery = Double.parseDouble(amountInput); } catch (Exception ignored) {}

        String descriptionQuery = descriptionInput.isEmpty() ? null : descriptionInput.toLowerCase();
        String vendorQuery      = vendorInput.isEmpty()      ? null : vendorInput.toLowerCase();

        // *** Visibility first: only rows the current user is allowed to see ***
        List<Transaction> visible = ledger.stream()
                .filter(Utilities::canView)      // admin => all; user => only their userId
                .sorted(BY_DATETIME_DESCENDING)        // newest first
                .toList();

        boolean anyPrinted = false;

        for (Transaction record : visible) {
            LocalDate d = record.getDate();

            if (startDate != null && d.isBefore(startDate)) continue;
            if (endDate   != null && d.isAfter(endDate))   continue;

            if (descriptionQuery != null &&
                    !record.getDescription().toLowerCase().contains(descriptionQuery)) continue;

            if (vendorQuery != null &&
                    !record.getVendor().toLowerCase().contains(vendorQuery)) continue;

            if (amountQuery != null && record.getAmount() != amountQuery) continue;

            printFormatted(record);
            anyPrinted = true;
        }

        if (!anyPrinted) System.out.println("No transactions match your filters.");
    }

    private static void searchByVendor() {
        searchByField(Transaction::getVendor, "vendor name");
    }

    private static void searchByField(Function<Transaction, String> getter, String prompt) {
        if (sc.hasNextLine()) sc.nextLine(); // clear leftover newline from prior nextInt/nextDouble
        System.out.print("Enter " + prompt + ": ");
        String query = sc.nextLine().trim().toLowerCase();

        // 1) Start from only-visible rows (admin => all, user => own)
        List<Transaction> visible = ledger.stream()
                .filter(Utilities::canView)
                .sorted(BY_DATETIME_DESCENDING)   // newest first
                .toList();

        // 2) Match case-insensitively on the chosen field
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


    private static void searchByDescription() {
        searchByField(Transaction::getDescription, "transaction description");
    }

    private static void printPreviousYear() {
        LocalDate today = LocalDate.now();
        int prevYear = today.getYear() - 1;
        LocalDate firstDayPrevYear = LocalDate.of(prevYear, 1, 1);
        LocalDate lastDayPrevYear = LocalDate.of(prevYear, 12, 31);
        printByDuration(firstDayPrevYear, lastDayPrevYear);
    }

    private static void printYearToDate() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        printByDuration(firstDayOfYear, today);
    }

    private static void printPreviousMonth() {
        LocalDate today = LocalDate.now(); //first I get current date
        //used yearMonth from java time to do calendric arithmetic as Paul calls it
        java.time.YearMonth prev = java.time.YearMonth.from(today).minusMonths(1); //then previous month
        LocalDate firstOfPrevious = prev.atDay(1);//then the first day of prev month
        LocalDate lastOfPrevious = prev.atEndOfMonth(); //then last day of the prev month

        printByDuration(firstOfPrevious, lastOfPrevious);
    }

    private static void printMonthToDate() {
        LocalDate today = LocalDate.now();
        LocalDate month = today.withDayOfMonth(1);
        printByDuration(month, today);
    }

    private static void printByDuration(LocalDate start, LocalDate end) {
        // Normalize bounds if passed in reversed
        if (start.isAfter(end)) {
            LocalDate tmp = start; start = end; end = tmp;
        }

        System.out.println("Displaying transactions between " + start + " and " + end);

        // Only the transactions the current user can see, already sorted newest-first
        List<Transaction> view = visibleSorted();

        for (Transaction t : view) {
            LocalDate d = t.getDate();
            // inclusive range: start <= d <= end
            if (!d.isBefore(start) && !d.isAfter(end)) {
                printFormatted(t);
            }
        }
    }

    private static void printByTypeSorted(String transactionType) {
        // Normalizing input anything not "credit"/"debit" falls back to "all"
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

    private static void performTransaction(boolean depositOnly) {
        System.out.println(depositOnly ? "DEPOSIT SCREEN" : "PAYMENT SCREEN");

        System.out.print("Enter the Transaction Description: ");
        String description = sc.nextLine();

        System.out.print("Enter the name of the vendor: ");
        String vendor = sc.nextLine();

        // read a number then normalize the sign based on depositOnly
        double amount;
        while (true) {
            System.out.print("Enter the amount: ");
            if (sc.hasNextDouble()) {
                double entered = sc.nextDouble();
                sc.nextLine(); // consume newline
                amount = depositOnly ? Math.abs(entered) : -Math.abs(entered);
                break;
            } else {
                System.out.println("Invalid number. Please enter a numeric amount (e.g., 123.45 or -67.89).");
                sc.nextLine(); //discard
            }
        }

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0);

        Transaction record = new Transaction(date, time, description, vendor, amount, currentUser.getId());
        if (seen.add(record)) {
            ledger.add(record);
        }

        writeToFile(record);
        System.out.printf("%s added successfully! (Amount: %,.2f)%n",
                depositOnly ? "Deposit" : "Payment", amount);
    }


    public static void writeToFile(Transaction record) {
        if (fileName == null || fileName.isEmpty()) {
            System.err.println("Output file name is not set. Cannot write.");
            return;
        }
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // userid|date|time|description|vendor|amount
            out.printf("%d|%s|%s|%s|%s|%.2f%n",
                    record.getUserId(),
                    record.getDate(),
                    record.getTime().toString(),  // ensure you set withNano(0) when creating
                    record.getDescription(),
                    record.getVendor(),
                    record.getAmount());
        } catch (IOException e) {
            System.err.println("Could not write to file: " + e.getMessage());
        }
    }

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
                    LocalDate date = LocalDate.parse(t[1].trim());      // YYYY-MM-DD
                    LocalTime time = LocalTime.parse(t[2].trim());      // HH:mm:ss
                    String description = t[3].trim();
                    String vendor = t[4].trim();
                    double amount = Double.parseDouble(t[5].trim());

                    // Use the userId from the file (do NOT use currentUser here)
                    Transaction record = new Transaction(date, time, description, vendor, amount, userId);
                    if (seen.add(record)) {//this will return false if it is seen before
                        ledger.add(record); //only add if the duplicate entry does not exist
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

    private static void printFormatted(Transaction record) {
        // date | description | vendor | amount | type | time
        String dateString = (record.getDate() == null) ? "" : record.getDate().toString(); // YYYY-MM-DD

        // Always HH:mm:ss with zero padding because my time was missing 00 sometimes
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
    //webserver methods
    public static String formatTransaction(Transaction record) {
        return String.format(
                "%s | %-30s | %-18s | %10.2f | %-6s | %s",
                record.getDate(), record.getDescription(), record.getVendor(),
                record.getAmount(), record.transactionType(), record.getTime()
        );
    }
    // Created a new method to use for webserver
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
