package com.aegisdiamond.shipping.dto;

public record InTransit() implements ShipmentState {
    @Override public String status() { return "IN_TRANSIT"; }
}
