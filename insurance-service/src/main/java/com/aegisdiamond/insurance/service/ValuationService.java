package com.aegisdiamond.insurance.service;

import org.springframework.stereotype.Service;

@Service
public class ValuationService {

    public double calculateDiamondValue(double basePrice, double carat, double qualityMultiplier) {
        // Formula: Diamond Value = Base Price × Carat × Quality Multiplier
        return basePrice * carat * qualityMultiplier;
    }
}
