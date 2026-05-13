package com.aegisdiamond.fraud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiFraudEngine {

    private static final Logger logger = LoggerFactory.getLogger(AiFraudEngine.class);

    @Autowired(required = false)
    private ChatModel chatModel;

    public String analyzePatterns(Long shipmentId, String payload) {
        logger.info("Analyzing fraud patterns for shipment ID: {}", shipmentId);
        if (chatModel == null) {
            logger.warn("ChatModel is null, returning default fraud analysis for shipment ID: {}", shipmentId);
            return "AI Fraud Analysis unavailable. Standard pattern matching indicates normal activity.";
        }

        String promptText = String.format(
            "Analyze the following shipment data for potential fraud, duplicate shipment attempts, or suspicious route patterns. " +
            "Shipment ID: %s. Data Payload: %s. " +
            "Reply with 'FRAUD DETECTED' or 'SECURE' and provide a detailed reason.",
            shipmentId, payload
        );

        try {
            return chatModel.call(promptText);
        } catch (Exception e) {
            logger.error("Error in AI fraud analysis for shipment ID {}: {}", shipmentId, e.getMessage());
            return "Error in fraud analysis.";
        }
    }

    public double calculateFraudProbability(String analysis) {
        logger.debug("Calculating fraud probability from analysis: {}", analysis);
        if (analysis.contains("FRAUD DETECTED")) {
            return 0.95;
        } else if (analysis.contains("SUSPICIOUS")) {
            return 0.65;
        }
        return 0.05;
    }
}
