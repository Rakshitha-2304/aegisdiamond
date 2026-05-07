package com.aegisdiamond.shipping.dto;

public sealed interface ShipmentState permits Created, Verified, Sealed, InTransit, Customs, Delivered, Closed {
    String status();
}
