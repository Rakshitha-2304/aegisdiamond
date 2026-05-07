package com.aegisdiamond.shipping.dto;

public record Delivered() implements ShipmentState {
    @Override public String status() { return "DELIVERED"; }
}
