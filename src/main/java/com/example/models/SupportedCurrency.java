package com.example.models;

// Single source of truth for currencies: the frontend offers exactly these, and FX lookups are
// only built for them.
public enum SupportedCurrency {
    USD, EUR, GBP, ARS, JPY;

    // Exact, case-sensitive match: valueOf would throw, and lenient casing would let "usd" reach
    // the database and Micrometer tags as a separate value.
    public static boolean isSupported(String code) {
        if (code == null) {
            return false;
        }
        for (SupportedCurrency currency : values()) {
            if (currency.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
