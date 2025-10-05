package com.spriteconverter.pixelate_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class Palette {
    private String id;
    private String name;
    private List<String> colors; // Hex colors
}