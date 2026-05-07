package com.aegisdiamond.diamond.dto;

public sealed interface DiamondState permits Registered, Certified, InTransit, InVault, Sold {
    String status();
}
