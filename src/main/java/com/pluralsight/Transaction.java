package com.pluralsight;

import java.time.LocalDate;
import java.time.LocalTime;

public class Transaction {
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String vendor;
    private double amount;
    private String transactionType;
    private int userId;

    public Transaction(LocalDate date, LocalTime time, String description, String vendor, double amount, int userId) {
        this.date = date;
        this.time = time;
        this.description = description;
        this.vendor = vendor;
        this.amount = amount;
        this.transactionType = transactionType();
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String transactionType() {

        if (amount < 0) {
            this.transactionType = "credit";
            return "credit";
        } else {
            this.transactionType = "debit";
            return "debit";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;

        // Compare amount to the cent to avoid double not matching issue i was facing
        long thisCents = Math.round(this.getAmount() * 100.0);
        long thatCents = Math.round(that.getAmount() * 100.0);

        return this.getUserId() == that.getUserId()
                && thisCents == thatCents
                && java.util.Objects.equals(this.getDate(), that.getDate())
                && java.util.Objects.equals(this.getTime(), that.getTime())
                && java.util.Objects.equals(this.getDescription(), that.getDescription())
                && java.util.Objects.equals(this.getVendor(), that.getVendor());
    }

    @Override
    public int hashCode() {
        long cents = Math.round(this.getAmount() * 100.0);
        int result = java.util.Objects.hash(
                getDate(), getTime(), getDescription(), getVendor(), getUserId()
        );
        // mix cents into hash
        result = 31 * result + Long.hashCode(cents);
        return result;
    }

}
