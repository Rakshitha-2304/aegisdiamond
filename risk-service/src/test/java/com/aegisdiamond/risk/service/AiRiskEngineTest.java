package com.aegisdiamond.risk.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiRiskEngineTest {

    private final AiRiskEngine riskEngine = new AiRiskEngine();

    @Test
    void testCalculateFormulaicRiskHighValue() {
        // $2M value (capped at 1.0), 0.5 route risk, 0.5 historical risk
        double score = riskEngine.calculateFormulaicRisk(2000000.0, 0.5, 0.5);
        
        // (0.4 * 1.0) + (0.3 * 0.5) + (0.3 * 0.5) = 0.4 + 0.15 + 0.15 = 0.7
        assertEquals(0.7, score, 0.001);
    }

    @Test
    void testCalculateFormulaicRiskLowValue() {
        // $100k value (0.1 factor), 0.1 route risk, 0.1 historical risk
        double score = riskEngine.calculateFormulaicRisk(100000.0, 0.1, 0.1);
        
        // (0.4 * 0.1) + (0.3 * 0.1) + (0.3 * 0.1) = 0.04 + 0.03 + 0.03 = 0.1
        assertEquals(0.1, score, 0.001);
    }
}
