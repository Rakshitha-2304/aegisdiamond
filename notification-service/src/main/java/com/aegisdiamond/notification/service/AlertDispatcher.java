package com.aegisdiamond.notification.service;

import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class AlertDispatcher {
    private static final Logger logger = Logger.getLogger(AlertDispatcher.class.getName());

    public void dispatch(String type, String message, String severity) {
        // In a real system, this would integrate with Twilio, SendGrid, or Firebase
        logger.info(String.format("[%s] DISPATCHED: %s (Severity: %s)", type.toUpperCase(), message, severity));
    }
}
