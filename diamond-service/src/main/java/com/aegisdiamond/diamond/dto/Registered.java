package com.aegisdiamond.diamond.dto;

public record Registered() implements DiamondState {
    public String status() { return "REGISTERED"; }
}
