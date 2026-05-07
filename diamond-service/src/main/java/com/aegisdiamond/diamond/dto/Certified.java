package com.aegisdiamond.diamond.dto;

public record Certified(String certificateId) implements DiamondState {
    public String status() { return "CERTIFIED"; }
}
