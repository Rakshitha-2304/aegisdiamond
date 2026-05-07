package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.dto.*;
import org.springframework.stereotype.Service;

@Service
public class DiamondStatusManager {

    public String getStatusMessage(DiamondState state) {
        return switch (state) {
            case Registered r -> "Diamond has been registered in the system.";
            case Certified c -> "Diamond is certified with ID: " + c.certificateId();
            case InTransit t -> "Diamond is currently in transit (Shipment: " + t.shipmentId() + ")";
            case InVault v -> "Diamond is safely stored in vault: " + v.vaultId();
            case Sold s -> "Diamond has been sold to owner: " + s.ownerId();
        };
    }
}
