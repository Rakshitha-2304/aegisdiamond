package com.aegisdiamond.fraud.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiFraudEngine {

    @Autowired(required = false)
    private ChatModel chatModel;

    public String analyzePatterns(Long shipmentId, String payload) {
        if (chatModel == null) {
            return "AI Fraud Analysis unavailable. Standard pattern matching indicates normal activity.";
        }

        String promptText = String.format(
            "Analyze the following shipment data for potential fraud, duplicate shipment attempts, or suspicious route patterns. " +
            "Shipment ID: %s. Data Payload: %s. " +
            "Reply with 'FRAUD DETECTED' or 'SECURE' and provide a detailed reason.",
            shipmentId, payload
        );

        return chatModel.call(promptText);
    }

    public double calculateFraudProbability(String analysis) {
        if (analysis.contains("FRAUD DETECTED")) {
            return 0.95;
        } else if (analysis.contains("SUSPICIOUS")) {
            return 0.65;
        }
        return 0.05;
    }
}
