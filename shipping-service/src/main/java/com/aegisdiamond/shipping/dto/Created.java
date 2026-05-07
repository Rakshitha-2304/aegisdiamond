package com.aegisdiamond.shipping.dto;

public record Created() implements ShipmentState {
    @Override public String status() { return "CREATED"; }
}
