package com.aegisdiamond.customs.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ComplianceEngine {

    public boolean validateDocuments(String origin, String destination, List<Long> documentIds) {
        // High-level mock rules
        if (origin.equalsIgnoreCase("USA") || destination.equalsIgnoreCase("UAE")) {
            // Require at least 3 documents for high-security regions
            return documentIds != null && documentIds.size() >= 3;
        }
        return documentIds != null && !documentIds.isEmpty();
    }

    public String getComplianceRequirement(String country) {
        return switch (country.toUpperCase()) {
            case "UAE" -> "Kimberley Process Certificate, Invoice, Packing List, Security Clearance";
            case "USA" -> "Customs Form 3461, Invoice, Certificate of Origin";
            default -> "Standard Shipping Documents (Invoice, Packing List)";
        };
    }
}
