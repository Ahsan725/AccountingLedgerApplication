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
import java.util.Scanner;

public class AccountingLedgerApplication {

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
                case 'd' -> depositsMainMenu();
//                case 'p' -> paymentsMainMenu();
//                case 'l' -> displayLedger();
                case 'x' -> System.out.println("Exiting...");
                default -> {
                    //catch all else
                    System.out.println("Invalid operation... Try again or press X to quit");

                }
            }

        }
    }

    private static void depositsMainMenu() {
        String description;
        String vendor;
        double amount;
        System.out.println("DEPOSIT SCREEN");
//        sc.nextLine();
        System.out.println("Enter the Transaction Description: ");
        description = sc.nextLine();

        System.out.println("Enter the name of the vendor: ");
        vendor = sc.nextLine();

        System.out.println("Enter the amount: ");
        amount = sc.nextDouble();

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0);
        sc.nextLine();

        //create a transaction object
        ledger.add(new Transaction(date, time, description, vendor, amount));
        System.out.println("Deposit added successfully!");

        //for testing
        printLatest();
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

            printLatest(); //only for testing right now
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found: " + fileName);
        } catch (IOException e) {
            System.err.println("I/O error reading " + fileName + ": " + e.getMessage());
        }
    }

    private static void printLatest() {
        //using stream here
        ledger.stream()
                .sorted(Comparator
                        .comparing(Transaction::getDate)
                        .thenComparing(Transaction::getTime)
                        .reversed())
                .forEach(record -> System.out.printf(
                        "type: %s  | %s | %s %s | %s | %.2f%n",
                        record.transactionType(),
                        record.getDescription(),
                        record.getDate(), record.getTime(),
                        record.getVendor(),
                        record.getAmount()
                ));
    }

}