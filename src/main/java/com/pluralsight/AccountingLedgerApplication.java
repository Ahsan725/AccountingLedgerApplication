package com.pluralsight;
//imports
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class AccountingLedgerApplication {
    //added a static Scanner to avoid passing it to methods repeatedly
    static Scanner sc = new Scanner(System.in);
    static String fileName = "transactions.csv";

    public static void main(String[] args) {

        //file we will read from and write to


        //This method displays the options and serves as the main screen
        readFromFileAndAdd();
        showMainMenu();
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
//                case 'd' -> depositsMainMenu();
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

    private static void readFromFileAndAdd() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;


            while ((line = br.readLine()) != null) {
                Transaction transaction1 = parseAndCreateTransaction(line);
                System.out.println("type: " + transaction1.getTransactionType()+ ", " + transaction1.getDescription());
                System.out.println("date: " + transaction1.getDate()+ ", " + transaction1.getTime());
            }
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: The file was not found at the specified path. Check your file name and location.");
        } catch (IOException e) {
            System.err.println("A general I/O error occurred while reading the file: " + e.getMessage());
        }

    }

}