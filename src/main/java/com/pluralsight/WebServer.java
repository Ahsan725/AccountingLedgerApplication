package com.pluralsight;

import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.Comparator;

//DTOs are simple containers for a set of data. They typically have no business logic just fields,
// constructors, and getters and setters

//I took date and time as strings to avoid needing extra JSON serializer modules for Java time that I was having trouble with.

//Records are a shorthand way of creating a class that stores data and its members are immutable.
//Records auto-generate constructor, accessors, equals, hashCode, toString
record TransactionDto(String date, String time, String description, String vendor, double amount, String type) {
}

public class WebServer {
//Entry point into the Web UI that will launch and configure the HTTP server.

    //a comparator to sort Transaction objects by date, then time, and then reverse it so newest appear first.
    private static final Comparator<Transaction> BY_DATETIME_DESCENDING = Comparator.comparing(Transaction::getDate)
            .thenComparing(Transaction::getTime).reversed();//Transaction::getDate and Transaction::getTime
    // are method references used in the comparator chain.


    // Helper that converts a domain Transaction to a TransactionDto.
// Safely handles null date/time by emitting empty strings otherwise uses toString().
// Copies description, vendor, amount, and computed type.
    private static TransactionDto toDto(Transaction record) {
        return new TransactionDto(record.getDate() == null ? "" : record.getDate().toString(), record.getTime() == null ? "" : record.getTime().toString(), record.getDescription(), record.getVendor(), record.getAmount(), record.transactionType());
    }

    //takes in a date string to parse as a LocalDate
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        // Load data
        Utilities.readFromFileAndAddToLedger();
        System.out.println("Loaded transactions: " + DataStore.ledger.size());

        // This is where I start the server. The javalin server is configured here to display static files like html css from this directory
        Javalin javalinApp = Javalin.create(javalinConfig -> javalinConfig.staticFiles.add("/public")).start(8080);

        //creating the GET api endpoints
        javalinApp.get("/api/health", context -> context.result("ok"));

        javalinApp.get("/api/transactions", context -> context.json(DataStore.ledger.stream().sorted(BY_DATETIME_DESCENDING).map(WebServer::toDto).toList()));

        javalinApp.get("/api/transactions/deposits", context -> context.json(DataStore.ledger.stream().filter(t -> t.getAmount() > 0).sorted(BY_DATETIME_DESCENDING).map(WebServer::toDto).toList()));

        javalinApp.get("/api/transactions/payments", context -> context.json(DataStore.ledger.stream().filter(t -> t.getAmount() < 0).sorted(BY_DATETIME_DESCENDING).map(WebServer::toDto).toList()));

        //date-range endpoints (JSON + plain text)
        javalinApp.get("/api/transactions/range", context -> {
            LocalDate start = parseDate(context.queryParam("start"));
            LocalDate end = parseDate(context.queryParam("end"));
            if (start == null || end == null) {
                context.status(400).result("start and end are required as YYYY-MM-DD");
                return;
            }
            var out = Utilities.transactionsByDuration(start, end).stream().map(WebServer::toDto).toList();
            context.json(out); //using one of the functions I created already in the CLI app to get time range data
        });
        //I created this for exporting to pdf and to show text response
        javalinApp.get("/api/transactions/range.txt", context -> {
            LocalDate start = parseDate(context.queryParam("start"));
            LocalDate end = parseDate(context.queryParam("end"));
            if (start == null || end == null) {
                context.status(400).result("start and end are required as YYYY-MM-DD");
                return;
            }
            var lines = Utilities.transactionsByDuration(start, end).stream().map(Utilities::formatTransaction).toList();
            context.header("Content-Type", "text/plain; charset=utf-8");
            context.result(String.join("\n", lines));
        });

        javalinApp.get("/api/transactions/user/{userId}", context -> {
            try {
                // Get the userId from the path parameter
                int userId = Integer.parseInt(context.pathParam("userId"));

                // Filter the ledger by the userId
                var userTransactions = DataStore.ledger.stream().filter(t -> t.getUserId() == userId).sorted(BY_DATETIME_DESCENDING).map(WebServer::toDto).toList();

                // Respond with the list of transactions
                if (userTransactions.isEmpty()) {
                    // Return 404 if no transactions are found for the user ID
                    context.status(404).result("No transactions found for user ID: " + userId);
                } else {
                    context.json(userTransactions);
                }

            } catch (NumberFormatException e) {
                // Return 400 if the path parameter is not a valid integer
                context.status(400).result("Invalid user ID format.");
            }
        });
    }
}
