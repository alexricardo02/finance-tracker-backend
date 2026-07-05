package com.example.models;

// Enforces DB integrity and guarantees clean groupings for frontend statistics
public enum PaymentMethod {
    CASH, 
    CREDIT_CARD, 
    DEBIT_CARD, 
    BANK_TRANSFER, 
    OTHER
}