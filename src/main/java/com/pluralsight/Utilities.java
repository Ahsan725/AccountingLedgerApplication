package com.pluralsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

public final class Utilities {

    //static comparator
    private static final Comparator<Transaction> BY_DATETIME_DESC = Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getTime).reversed();
    //Static variables
    static Scanner sc = new Scanner(System.in); //added a static Scanner to avoid passing it to methods repeatedly
    static String fileName = "transactions.csv";//file we will read from and write to
    static List<Transaction> ledger = DataStore.ledger;
    // at top of Utilities (next to fileName)
    static String profilesFileName = "profiles.csv";
    static User currentUser;
    static HashMap<Integer, User> idToUser = new HashMap<>();

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
                case 'x' -> {
                    if (sc.hasNextLine()) sc.nextLine();
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

                String[] t = s.split("\\|", -1);

                // Skip header anywhere
                if (t.length >= 4
                        && t[0].trim().equalsIgnoreCase("userid")
                        && t[1].trim().equalsIgnoreCase("name")
                        && t[2].trim().equalsIgnoreCase("pin")
                        && t[3].trim().equalsIgnoreCase("access")) {
                    continue;
                }

                // Need at least: id | name | pin  (access optional but preferred)
                if (t.length < 3) continue;

                try {
                    int id = Integer.parseInt(t[0].trim());
                    String name = t[1].trim();
                    String pin = t[2].trim();

                    boolean isAdmin = false;
                    if (t.length >= 4) {
                        // any case: "true"/"false"
                        isAdmin = Boolean.parseBoolean(t[3].trim());
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
            operation = sc.nextLine().toLowerCase().charAt(0);
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
        // If a previous menu read used nextInt/nextDouble, a newline may be left in the buffer.
        // This consumes it so the next nextLine() works as expected.
        if (sc.hasNextLine()) sc.nextLine(); // clear leftover newline

        System.out.println("CUSTOM SEARCH");
        // Prompt for optional start date filter
        System.out.print("Start Date (YYYY-MM-DD) or leave it blank: ");
        String startDateInput = sc.nextLine().trim(); // raw user input for start date
        // Prompt for optional end date filter
        System.out.print("End Date (YYYY-MM-DD) or leave it blank: ");
        String endDateInput = sc.nextLine().trim(); // raw user input for end date
        // Prompt for optional description substring filter
        System.out.print("Description contains (blank to skip): ");
        String descriptionInput = sc.nextLine().trim(); // raw user input for description
        // Prompt for optional vendor substring filter
        System.out.print("Vendor contains (blank to skip): ");
        String vendorInput = sc.nextLine().trim(); // raw user input for vendor
        // Prompt for optional exact amount filter
        System.out.print("Amount or leave it blank: ");
        String amountInput = sc.nextLine().trim(); // raw user input for amount

        // Parsed values used for filtering null means do not filter on that field
        LocalDate startDate = null, endDate = null;
        Double amountQuery = null;

        // Try to parse start date if provided; ignore errors and keep it null if bad
        try {
            if (!startDateInput.isEmpty()) startDate = LocalDate.parse(startDateInput);
        } catch (Exception ignore) {
        }

        // Try to parse end date if provided; ignore errors and keep it null if bad
        try {
            if (!endDateInput.isEmpty()) endDate = LocalDate.parse(endDateInput);
        } catch (Exception ignore) {
        }

        // Try to parse amount if provided; ignore errors and keep it null if bad
        try {
            if (!amountInput.isEmpty()) amountQuery = Double.parseDouble(amountInput);
        } catch (Exception ignore) {
        }

        // Normalize text queries to lowercase for case-insensitive matching; null means no filter
        String descriptionQuery = descriptionInput.isEmpty() ? null : descriptionInput.toLowerCase();
        String vendorQuery = vendorInput.isEmpty() ? null : vendorInput.toLowerCase();

        // Work on a copy so we do not reorder the main ledger list
        List<Transaction> ledgerCopy = new ArrayList<>(ledger);
        // Sort newest first by your static comparator
        ledgerCopy.sort(BY_DATETIME_DESC); // latest first

        // Track whether any record matched, to show a message if none do
        boolean anyPrinted = false;

        // Scan through each transaction in newest first order
        for (Transaction record : ledgerCopy) {
            // Grab the transaction date once to avoid repeated calls
            LocalDate transactionDate = record.getDate();

            // If start date is set, skip anything before it
            if (startDate != null && transactionDate.isBefore(startDate)) continue;
            // If end date is set, skip anything after it
            if (endDate != null && transactionDate.isAfter(endDate)) continue;

            // If description filter is set, require a case-insensitive substring match
            if (descriptionQuery != null &&
                    !record.getDescription().toLowerCase().contains(descriptionQuery)) continue;

            // If vendor filter is set, require a case-insensitive substring match
            if (vendorQuery != null &&
                    !record.getVendor().toLowerCase().contains(vendorQuery)) continue;

            // If amount filter is set, require exact match on double
            if (amountQuery != null && record.getAmount() != amountQuery) continue;

            // If we reach here, the record passed all active filters, so print it
            printFormatted(record);
            anyPrinted = true; // mark that we printed at least one result
        }

        // If nothing matched, let the user know
        if (!anyPrinted) System.out.println("No transactions match your filters.");
    }

    private static void searchByVendor() {
        searchByField(Transaction::getVendor, "vendor name");
    }

    private static void searchByField(Function<Transaction, String> getter, String prompt) {
        if (sc.hasNextLine()) sc.nextLine();
        System.out.print("Enter " + prompt + ": ");
        String userInput = sc.nextLine().trim().toLowerCase();

        List<Transaction> ledgerCopy = new ArrayList<>(ledger);
        ledgerCopy.sort(BY_DATETIME_DESC);

        for (Transaction record : ledgerCopy) {
            String field = getter.apply(record);
            if (field != null && field.toLowerCase().contains(userInput)) {
                printFormatted(record);
            }
        }
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
        //will add guardrails later
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        ArrayList<Transaction> ledgerCopy = new ArrayList<>(ledger);
        ledgerCopy.sort(BY_DATETIME_DESC); //latest first
        System.out.println("Displaying All Transactions between " + start + " and " + end);
        for (Transaction record : ledgerCopy) {
            LocalDate date = record.getDate();

            if ((date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end))) {
                printFormatted(record);
            }
        }
    }

    private static void printByTypeSorted(String transactionType) {
        // here I will normalize the input first null or anything else means "all"
        String type = transactionType == null ? "all" : transactionType.toLowerCase();

        List<Transaction> copy = new ArrayList<>(ledger);
        copy.sort(BY_DATETIME_DESC); // newest first

        switch (type) {
            case "credit": // payments (amount < 0)
                for (Transaction t : copy) {
                    if (t.getAmount() < 0) printFormatted(t);
                }
                break;
            case "debit":  // deposits (amount > 0)
                for (Transaction t : copy) {
                    if (t.getAmount() > 0) printFormatted(t);
                }
                break;
            default:       // "all"
                for (Transaction t : copy) {
                    printFormatted(t);
                }
        }
    }

    private static void performTransaction(boolean depositOnly) {
        System.out.println(depositOnly ? "DEPOSIT SCREEN" : "PAYMENT SCREEN");
        // If method called nextInt/nextDouble earlier, clear the leftover newline first:
//        if (sc.hasNextLine()) sc.nextLine();
        System.out.print("Enter the Transaction Description: ");
        String description = sc.nextLine();
        System.out.print("Enter the name of the vendor: ");
        String vendor = sc.nextLine();

        double amount;
        while (true) {
            System.out.print("Enter the amount: ");
            if (sc.hasNextDouble()) {
                amount = sc.nextDouble();
                sc.nextLine(); // consume the newline left by nextDouble()
                if (depositOnly && amount < 0) {
                    System.out.println("Amount is negative! Enter a positive amount to make a deposit...");
                    continue;
                }
                if (!depositOnly && amount > 0) {
                    System.out.println("Amount is positive! Enter a negative amount to make a payment...");
                    continue;
                }
                break; // valid amount
            } else {
                System.out.println("Invalid number. Please enter a numeric amount (e.g., 123.45 or -67.89).");
                sc.nextLine(); // discard bad token
            }
        }

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0); //to get rid of extra nanoseconds at the end

        Transaction record = new Transaction(date, time, description, vendor, amount);
        ledger.add(record);
//        DataStore.ledger.add(record); //for the webserver
        //cal the write to file method
        writeToFile(record);
        System.out.println(depositOnly ? "Deposit added successfully!" : "Payment added successfully!");

    }

    public static void writeToFile(Transaction record) {
        System.out.println("Writing to file...");
        if (fileName == null || fileName.isEmpty()) {
            System.err.println("Output file name is not set. Cannot write.");
            return;
        }

        try (FileWriter fw = new FileWriter(fileName, true);  //here I set append mode to true to prevent overwrite
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter output = new PrintWriter(bw)) {

            output.printf("%s|%s|%s|%s|%.2f%n",
                    record.getDate(),
                    record.getTime().toString(),
                    record.getDescription(),
                    record.getVendor(),
                    record.getAmount());

            System.out.println("Done!");
        } catch (IOException e) {
            System.err.println("Could not write to file: " + e.getMessage());
        }
    }

    public static void readFromFileAndAddToLedger() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split on | symbol and also allows extra spaces around pipes
                String[] tokens = line.split("\\|", -1);

                // Skip header lines anywhere (case-insensitive)
                if (tokens.length >= 5 && tokens[0].trim().equalsIgnoreCase("date") && tokens[1].trim().equalsIgnoreCase("time") && tokens[2].trim().equalsIgnoreCase("description") && tokens[3].trim().equalsIgnoreCase("vendor") && tokens[4].trim().equalsIgnoreCase("amount")) {
                    continue;
                }

                if (tokens.length != 5) continue; // not a valid transaction line because i need at least 5

                try {
                    LocalDate date = LocalDate.parse(tokens[0].trim());// YYYY-MM-DD
                    LocalTime time = LocalTime.parse(tokens[1].trim());// HH:mm:ss
                    String description = tokens[2].trim();
                    String vendor = tokens[3].trim();
                    double amount = Double.parseDouble(tokens[4].trim());
                    Transaction record = new Transaction(date, time, description, vendor, amount);
                    ledger.add(record);
//                    DataStore.ledger.add(record); // for webserver functionality
                } catch (Exception ignore) {
                    System.out.println("Bad Input: Skipping a row..."); //skips a row and keeps the program running
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
        // widths: 10 | 30 | 20 | 12 | 6 | 8
        String dateString = (record.getDate() == null) ? "" : record.getDate().toString(); // YYYY-MM-DD

        // Always HH:mm:ss with zero padding because my time was missing 00 sometimes
        String timeString = "";
        if (record.getTime() != null) {
            timeString = record.getTime()
                    .withNano(0)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        System.out.printf(
                "%-10s  %-30.30s  %-12.20s  %,12.2f  %-6.6s  %-8s%n",
                dateString,
                record.getDescription(),
                record.getVendor(),
                record.getAmount(),            // amount with commas
                record.transactionType(),
                timeString                   // zero-padded time
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
                .sorted(BY_DATETIME_DESC)
                .toList();
    }

}
