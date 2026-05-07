package com.aegisdiamond.diamond.dto;

public record InVault(Long vaultId) implements DiamondState {
    public String status() { return "IN_VAULT"; }
}
