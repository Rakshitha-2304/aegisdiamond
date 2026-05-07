package com.aegisdiamond.diamond.dto;

public record InTransit(Long shipmentId) implements DiamondState {
    public String status() { return "IN_TRANSIT"; }
}
