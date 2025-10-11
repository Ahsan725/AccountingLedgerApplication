package com.pluralsight;
//imports
import java.util.Scanner;

public class AccountingLedgerApplication {
    //added a static Scanner to avoid passing it to methods repeatedly
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) {

        //file we will read from and write to
        String fileName = "transactions.csv";

        //This method displays the options and serves as the main screen
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
}