package com.aegisdiamond.diamond.dto;

public record Sold(String ownerId) implements DiamondState {
    public String status() { return "SOLD"; }
}
