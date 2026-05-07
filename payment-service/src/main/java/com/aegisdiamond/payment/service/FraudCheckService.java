package com.aegisdiamond.payment.service;

import org.springframework.stereotype.Service;

@Service
public class FraudCheckService {

    public boolean isTransactionSafe(Long transactionId, double amount) {
        // High-level mock rules
        if (amount > 5000000.0) {
            // Very high value transactions need extra scrutiny
            return false; // Requires manual review or fails in this mock
        }
        return true;
    }
}
