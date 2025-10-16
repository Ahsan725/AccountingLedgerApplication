package com.pluralsight;


import static com.pluralsight.Utilities.startCliApplication; //imported the entry point method that is designed to call
//other methods as needed. This way the user only need to call just this one method inside just one class.

public class AccountingLedgerApplication {

    public static void main(String[] args) {

        //This method displays the options and serves as the main screen. It also autoloads the
        // transactions from the "transactions.csv" file upon invocation
        //This is the entry point of the entire CLI application.
        startCliApplication();
    }
}