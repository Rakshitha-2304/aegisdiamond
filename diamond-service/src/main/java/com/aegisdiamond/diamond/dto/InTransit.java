package com.aegisdiamond.diamond.dto;

public record InTransit(String shipmentId) implements DiamondState {
    public String status() { return "IN_TRANSIT"; }
}
