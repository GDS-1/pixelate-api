package com.spriteconverter.pixelate_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProcessingResponse {
    private String processedImage;
    private ImageMetadata metadata;

    @Data
    @AllArgsConstructor
    public static class ImageMetadata {
        private String originalSize;
        private String processedSize;
        private Integer colorsUsed;
        private List<String> colors;
    }
}