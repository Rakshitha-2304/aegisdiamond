package com.aegisdiamond.risk.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiRiskEngine {

    @Autowired(required = false)
    private ChatModel chatModel;

    private static final double W1 = 0.4; // Value Factor Weight
    private static final double W2 = 0.3; // Route Risk Weight
    private static final double W3 = 0.3; // Historical Risk Weight

    public double calculateFormulaicRisk(double value, double routeRisk, double historicalRisk) {
        // Value Factor = Value / 1,000,000 (capped at 1.0)
        double valueFactor = Math.min(value / 1000000.0, 1.0);
        return (W1 * valueFactor) + (W2 * routeRisk) + (W3 * historicalRisk);
    }

    public String generateAiInsights(long shipmentId, double value, String route, double baseRisk) {
        if (chatModel == null) {
            return "AI Insights currently unavailable. Base risk analysis suggests focus on value-to-route ratio.";
        }

        String promptText = String.format(
            "Analyze the shipment risk for Shipment ID: %d. Value: $%.2f. Route: %s. Base Risk Score: %.2f. " +
            "Provide concise security insights and recommendations.",
            shipmentId, value, route, baseRisk
        );

        return chatModel.call(promptText);
    }

    public String detectAnomalies(long shipmentId, String currentData) {
        if (chatModel == null) {
            return "Anomaly detection service running in manual mode. No immediate deviations detected.";
        }

        String promptText = String.format(
            "Analyze the following real-time data for Shipment %d for anomalies or suspicious patterns: %s. " +
            "Reply with 'ANOMALY DETECTED' or 'NORMAL' and a brief explanation.",
            shipmentId, currentData
        );

        return chatModel.call(promptText);
    }
}
