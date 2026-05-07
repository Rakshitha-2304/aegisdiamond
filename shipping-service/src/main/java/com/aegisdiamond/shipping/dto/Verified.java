package com.aegisdiamond.shipping.dto;

public record Verified() implements ShipmentState {
    @Override public String status() { return "VERIFIED"; }
}
