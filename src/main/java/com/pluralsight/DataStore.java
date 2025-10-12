package com.pluralsight;

import java.util.ArrayList;
import java.util.List;

public final class DataStore {
    public static final List<Transaction> ledger = new ArrayList<>();
    private DataStore() {}
}
