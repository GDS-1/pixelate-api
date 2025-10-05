package com.spriteconverter.pixelate_api.model;

public enum QuantizationStrategy {
    MEDIAN_CUT,      // Original - good for general use
    POPULARITY,      // Best for low color counts (2-16)
    WEIGHTED,        // Balanced - considers frequency
    K_MEANS,         //Slower, Higher quality
}
