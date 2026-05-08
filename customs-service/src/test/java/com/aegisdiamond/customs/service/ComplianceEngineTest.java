package com.aegisdiamond.customs.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComplianceEngineTest {

    private final ComplianceEngine complianceEngine = new ComplianceEngine();

    @Test
    void testValidateDocumentsUAE() {
        // UAE requires 3+ documents
        List<Long> docs = List.of(1L, 2L, 3L);
        assertTrue(complianceEngine.validateDocuments("India", "UAE", docs));
        
        List<Long> insufficientDocs = List.of(1L, 2L);
        assertFalse(complianceEngine.validateDocuments("India", "UAE", insufficientDocs));
    }

    @Test
    void testValidateDocumentsGeneric() {
        // Other countries require at least 1 document
        List<Long> docs = List.of(1L);
        assertTrue(complianceEngine.validateDocuments("India", "Belgium", docs));
        
        assertFalse(complianceEngine.validateDocuments("India", "Belgium", List.of()));
    }
}
