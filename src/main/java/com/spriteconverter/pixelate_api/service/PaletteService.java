package com.spriteconverter.pixelate_api.service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

@Service
public class PaletteService {


    public BufferedImage applyPalette(BufferedImage image, List<String> hexColors, boolean autoColorMapping) {
        if (hexColors == null || hexColors.isEmpty()) {
            return image;
        }

        int[][] palette = hexColors.stream()
                .map(this::hexToRgb)
                .toArray(int[][]::new);

        if (autoColorMapping) {
            return mapToPaletteAuto(image, palette);
        } else {
            return mapToPaletteManual(image, palette);
        }
    }

    private BufferedImage mapToPaletteAuto(BufferedImage original, int[][] palette) {
        BufferedImage result = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color originalColor = new Color(original.getRGB(x, y), true);

                if (originalColor.getAlpha() < 128) {
                    result.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                    continue;
                }

                int[] rgb = new int[]{
                        originalColor.getRed(),
                        originalColor.getGreen(),
                        originalColor.getBlue()
                };

                int[] nearestColor = findNearestColor(rgb, palette);
                Color newColor = new Color(nearestColor[0], nearestColor[1], nearestColor[2], 255);
                result.setRGB(x, y, newColor.getRGB());
            }
        }

        return result;
    }

    private BufferedImage mapToPaletteManual(BufferedImage original, int[][] palette) {
        // Extract unique colors from the image (excluding transparent)
        Set<Integer> uniqueColorsSet = new LinkedHashSet<>();

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color color = new Color(original.getRGB(x, y), true);

                if (color.getAlpha() >= 128) {
                    int rgb = new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB();
                    uniqueColorsSet.add(rgb);
                }
            }
        }

        List<Integer> uniqueColors = new ArrayList<>(uniqueColorsSet);

        // Create mapping: image color index → palette color
        Map<Integer, int[]> colorMapping = new HashMap<>();
        for (int i = 0; i < uniqueColors.size() && i < palette.length; i++) {
            colorMapping.put(uniqueColors.get(i), palette[i]);
        }

        // If there are more unique colors than palette colors, map extras to last palette color
        if (uniqueColors.size() > palette.length) {
            int[] lastPaletteColor = palette[palette.length - 1];
            for (int i = palette.length; i < uniqueColors.size(); i++) {
                colorMapping.put(uniqueColors.get(i), lastPaletteColor);
            }
        }

        // Apply the mapping
        BufferedImage result = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color originalColor = new Color(original.getRGB(x, y), true);

                if (originalColor.getAlpha() < 128) {
                    result.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                    continue;
                }

                int rgb = new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue()).getRGB();
                int[] mappedColor = colorMapping.get(rgb);

                if (mappedColor != null) {
                    Color newColor = new Color(mappedColor[0], mappedColor[1], mappedColor[2], 255);
                    result.setRGB(x, y, newColor.getRGB());
                }
            }
        }

        return result;
    }

    private int[] findNearestColor(int[] pixel, int[][] palette) {
        int[] nearest = palette[0];
        double minDistance = colorDistance(pixel, nearest);

        for (int[] paletteColor : palette) {
            double distance = colorDistance(pixel, paletteColor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = paletteColor;
            }
        }

        return nearest;
    }

    private double colorDistance(int[] c1, int[] c2) {
        int dr = c1[0] - c2[0];
        int dg = c1[1] - c2[1];
        int db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");

        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color format: " + hex, e);
        }
    }
}