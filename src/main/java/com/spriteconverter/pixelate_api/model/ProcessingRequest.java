package com.spriteconverter.pixelate_api.model;

import lombok.Data;
import java.util.List;

@Data
public class ProcessingRequest {
    private Integer pixelSize;
    private Integer targetWidth;
    private Integer targetHeight;
    private Boolean upscaleBack = true;
    private Integer colorCount;
    private Boolean autoColorMapping = true;
    private QuantizationStrategy quantizationStrategy;
    private List<String> palette;
    private Integer borderThickness;
    private String borderColor;
}