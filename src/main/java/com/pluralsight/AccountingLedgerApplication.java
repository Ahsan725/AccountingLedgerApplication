package com.pluralsight;
//imports

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class AccountingLedgerApplication {

    //static comparator
    private static final Comparator<Transaction> BY_DATETIME_DESC =
            Comparator.comparing(Transaction::getDate)
                    .thenComparing(Transaction::getTime)
                    .reversed();
    //Static variables
    static Scanner sc = new Scanner(System.in); //added a static Scanner to avoid passing it to methods repeatedly
    static String fileName = "transactions.csv";//file we will read from and write to
    static ArrayList<Transaction> ledger = new ArrayList<>();

    public static void main(String[] args) {

        //This method displays the options and serves as the main screen
        readFromFileAndAddToLedger(); //load transactions into the program
        showMainMenu(); //displays main menu
    }

    private static void showMainMenu() {
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
                case 'x' -> System.out.println("Exiting...");
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press X to quit");

                }
            }

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
                case 'a' -> printAllLatest();
                case 'd' -> printAllDeposits();
                case 'p' -> printAllPayments();
//                case 'r' ->
                case 'h' -> System.out.println("Exiting...");
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press X to quit");

                }
            }

        }
    }

    private static void printAllPayments() {
        List<Transaction> ledgerCopy = new ArrayList<>(ledger);
        ledgerCopy.sort(BY_DATETIME_DESC); //sort the Ledger Copy to be in sorted format by latest
        for (Transaction record : ledgerCopy) { //for every Transaction in ledger Copy
            if (record.getAmount() < 0) { //this will print all the credits
                printFormatted(record);
            }
        }
    }

    private static void printAllDeposits() {
        List<Transaction> copy = new ArrayList<>(ledger);
        copy.sort(BY_DATETIME_DESC);
        for (Transaction record : copy) {
            if (record.getAmount() > 0) {
                printFormatted(record);
            }
        }
    }

    private static void performTransaction(boolean depositOnly) {
        System.out.println(depositOnly ? "DEPOSIT SCREEN" : "PAYMENT SCREEN");
        // If method called nextInt/nextDouble earlier, clear the leftover newline first:
        if (sc.hasNextLine()) sc.nextLine();
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

        ledger.add(new Transaction(date, time, description, vendor, amount));
        System.out.println(depositOnly ? "Deposit added successfully!" : "Payment added successfully!");

        // for testing
        printAllLatest();
    }

    private static void readFromFileAndAddToLedger() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split on | symbol and also allows extra spaces around pipes
                String[] tokens = line.split("\\|", -1);

                // Skip header lines anywhere (case-insensitive)
                if (tokens.length >= 5
                        && tokens[0].trim().equalsIgnoreCase("date")
                        && tokens[1].trim().equalsIgnoreCase("time")
                        && tokens[2].trim().equalsIgnoreCase("description")
                        && tokens[3].trim().equalsIgnoreCase("vendor")
                        && tokens[4].trim().equalsIgnoreCase("amount")) {
                    continue;
                }

                if (tokens.length != 5) continue; // not a valid transaction line because i need at least 5

                try {
                    LocalDate date = LocalDate.parse(tokens[0].trim());// YYYY-MM-DD
                    LocalTime time = LocalTime.parse(tokens[1].trim());// HH:mm:ss
                    String description = tokens[2].trim();
                    String vendor = tokens[3].trim();
                    double amount = Double.parseDouble(tokens[4].trim());

                    ledger.add(new Transaction(date, time, description, vendor, amount));
                } catch (Exception ignore) {
                    System.out.println("Bad Input: Skipping a row..."); //skips a row and keeps the program running
                }
            }

            printAllLatest(); //only for testing right now
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found: " + fileName);
        } catch (IOException e) {
            System.err.println("I/O error reading " + fileName + ": " + e.getMessage());
        }
    }

    private static void printAllLatest() {
        //could use stream here
        List<Transaction> copy = new ArrayList<>(ledger); // making a copy to not change the order of the original ledger
        copy.sort(BY_DATETIME_DESC);                      // I am using the custom comparator that sorts by latest first
        for (Transaction record : copy) {
            printFormatted(record);
        }
    }

    private static void printFormatted(Transaction record) {
        System.out.printf(
                "type: %s  | %s | %s %s | %s | %.2f%n",
                record.transactionType(),
                record.getDescription(),
                record.getDate(), record.getTime(),
                record.getVendor(),
                record.getAmount()
        );
    }
}