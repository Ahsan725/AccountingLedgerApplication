package com.pluralsight;

import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.Comparator;

// DTO with date/time as strings (avoids JavaTimeModule)
record TxDto(String date, String time, String description, String vendor, double amount, String type) {
}

public class WebServer {

    private static final Comparator<Transaction> BY_DATETIME_DESC =
            Comparator.comparing(Transaction::getDate)
                    .thenComparing(Transaction::getTime)
                    .reversed();

    private static TxDto toDto(Transaction t) {
        return new TxDto(
                t.getDate() == null ? "" : t.getDate().toString(),
                t.getTime() == null ? "" : t.getTime().toString(),
                t.getDescription(),
                t.getVendor(),
                t.getAmount(),
                t.transactionType()
        );
    }

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
        AccountingLedgerApplication.readFromFileAndAddToLedger();
        System.out.println("Loaded transactions: " + DataStore.ledger.size());

        // This is where I start the server
        Javalin javalinApp = Javalin.create(javalinConfig -> javalinConfig.staticFiles.add("/public")).start(8080);

        javalinApp.get("/api/health", context -> context.result("ok"));

        javalinApp.get("/api/transactions", context
                -> context
                .json(
                        DataStore.ledger.stream()
                                .sorted(BY_DATETIME_DESC)
                                .map(WebServer::toDto)
                                .toList()
                ));

        javalinApp.get("/api/transactions/deposits", context
                -> context
                .json(
                        DataStore.ledger.stream()
                                .filter(t -> t.getAmount() > 0)
                                .sorted(BY_DATETIME_DESC)
                                .map(WebServer::toDto)
                                .toList()
                ));

        javalinApp.get("/api/transactions/payments", context
                -> context
                .json(
                        DataStore.ledger.stream()
                                .filter(t -> t.getAmount() < 0)
                                .sorted(BY_DATETIME_DESC)
                                .map(WebServer::toDto)
                                .toList()
                ));

        //date-range endpoints (JSON + plain text)
        javalinApp.get("/api/transactions/range", context
                -> {
            LocalDate start = parseDate(context
                    .queryParam("start"));
            LocalDate end = parseDate(context
                    .queryParam("end"));
            if (start == null || end == null) {
                context
                        .status(400).result("start and end are required as YYYY-MM-DD");
                return;
            }
            var out = AccountingLedgerApplication.transactionsByDuration(start, end)
                    .stream().map(WebServer::toDto).toList();
            context
                    .json(out);
        });
        //I created this for exporting to pdf and to show text response
        javalinApp.get("/api/transactions/range.txt", context
                -> {
            LocalDate start = parseDate(context
                    .queryParam("start"));
            LocalDate end = parseDate(context
                    .queryParam("end"));
            if (start == null || end == null) {
                context
                        .status(400).result("start and end are required as YYYY-MM-DD");
                return;
            }
            var lines = AccountingLedgerApplication.transactionsByDuration(start, end)
                    .stream().map(AccountingLedgerApplication::formatTransaction).toList();
            context
                    .header("Content-Type", "text/plain; charset=utf-8");
            context
                    .result(String.join("\n", lines));
        });
    }
}
