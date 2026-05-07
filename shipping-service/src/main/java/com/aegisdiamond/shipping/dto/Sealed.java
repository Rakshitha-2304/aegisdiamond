package com.aegisdiamond.shipping.dto;

public record Sealed() implements ShipmentState {
    @Override public String status() { return "SEALED"; }
}
