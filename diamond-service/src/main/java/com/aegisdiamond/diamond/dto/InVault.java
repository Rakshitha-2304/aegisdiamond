package com.aegisdiamond.diamond.dto;

public record InVault(String vaultId) implements DiamondState {
    public String status() { return "IN_VAULT"; }
}
