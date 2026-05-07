package com.aegisdiamond.shipping.dto;

public record Closed() implements ShipmentState {
    @Override public String status() { return "CLOSED"; }
}
