package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.dto.*;
import org.springframework.stereotype.Service;

@Service
public class ShipmentStatusManager {

    public String getStatusMessage(ShipmentState state) {
        return switch (state) {
            case Created c -> "Shipment has been created and is awaiting verification.";
            case Verified v -> "Shipment has been verified and is ready for sealing.";
            case Sealed s -> "Shipment is sealed and ready for transit.";
            case InTransit t -> "Shipment is currently in transit.";
            case Customs cu -> "Shipment is undergoing customs clearance.";
            case Delivered d -> "Shipment has been delivered successfully.";
            case Closed cl -> "Shipment is closed and archived.";
        };
    }

    public ShipmentState fromString(String status) {
        return switch (status) {
            case "CREATED" -> new Created();
            case "VERIFIED" -> new Verified();
            case "SEALED" -> new Sealed();
            case "IN_TRANSIT" -> new InTransit();
            case "CUSTOMS" -> new Customs();
            case "DELIVERED" -> new Delivered();
            case "CLOSED" -> new Closed();
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
}
