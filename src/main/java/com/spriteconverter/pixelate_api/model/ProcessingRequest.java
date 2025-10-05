package com.spriteconverter.pixelate_api.model;

import lombok.Data;

@Data
public class ProcessingRequest {
    private Integer pixelSize;
    private Integer colorCount;
    private String paletteId;
    private BorderConfig border;

    @Data
    public static class BorderConfig {
        private Integer thickness;
        private String color;
    }
}