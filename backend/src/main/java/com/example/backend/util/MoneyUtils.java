package com.example.backend.util;

public final class MoneyUtils {

    private MoneyUtils() {}

    public static String format(long amount, String currency) {
        return amount + " " + currency;
    }
}
