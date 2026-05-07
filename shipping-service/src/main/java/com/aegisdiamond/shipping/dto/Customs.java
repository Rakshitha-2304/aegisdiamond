package com.aegisdiamond.shipping.dto;

public record Customs() implements ShipmentState {
    @Override public String status() { return "CUSTOMS"; }
}
