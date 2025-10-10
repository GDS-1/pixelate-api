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
        // Extract unique colors from the image
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

        // Create optimal one-to-one mapping using greedy approach
        Map<Integer, int[]> colorMapping = createOptimalMapping(uniqueColors, palette);

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

    private Map<Integer, int[]> createOptimalMapping(List<Integer> imageColors, int[][] palette) {
        Map<Integer, int[]> mapping = new HashMap<>();

        // Compute brightness for image colors
        List<ColorBrightness> imageWithBrightness = new ArrayList<>();
        for (int rgb : imageColors) {
            Color c = new Color(rgb, true);
            double brightness = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
            imageWithBrightness.add(new ColorBrightness(rgb, brightness));
        }

        // Compute brightness for palette
        List<PaletteBrightness> paletteWithBrightness = new ArrayList<>();
        for (int[] rgb : palette) {
            double brightness = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
            paletteWithBrightness.add(new PaletteBrightness(rgb, brightness));
        }

        // Sort both by brightness (dark → light)
        imageWithBrightness.sort(Comparator.comparingDouble(cb -> cb.brightness));
        paletteWithBrightness.sort(Comparator.comparingDouble(pb -> pb.brightness));

        // Map one-to-one in order
        int count = Math.min(imageWithBrightness.size(), paletteWithBrightness.size());
        for (int i = 0; i < count; i++) {
            mapping.put(imageWithBrightness.get(i).rgb, paletteWithBrightness.get(i).rgb);
        }

        // Map any remaining image colors to the nearest palette color by distance
        if (imageWithBrightness.size() > paletteWithBrightness.size()) {
            int[][] paletteArray = paletteWithBrightness.stream().map(pb -> pb.rgb).toArray(int[][]::new);
            for (int i = count; i < imageWithBrightness.size(); i++) {
                int[] nearest = findNearestColor(
                        rgbToArray(imageWithBrightness.get(i).rgb),
                        paletteArray
                );
                mapping.put(imageWithBrightness.get(i).rgb, nearest);
            }
        }

        return mapping;
    }

    private static class ColorBrightness {
        int rgb;
        double brightness;

        ColorBrightness(int rgb, double brightness) {
            this.rgb = rgb;
            this.brightness = brightness;
        }
    }

    private static class PaletteBrightness {
        int[] rgb;
        double brightness;

        PaletteBrightness(int[] rgb, double brightness) {
            this.rgb = rgb;
            this.brightness = brightness;
        }
    }

    private int[] rgbToArray(int rgb) {
        Color c = new Color(rgb, true);
        return new int[]{c.getRed(), c.getGreen(), c.getBlue()};
    }


    private static class ColorPairing {
        int imageColorIndex;
        int paletteIndex;
        double distance;
        int imageColorRgb;

        ColorPairing(int imageColorIndex, int paletteIndex, double distance, int imageColorRgb) {
            this.imageColorIndex = imageColorIndex;
            this.paletteIndex = paletteIndex;
            this.distance = distance;
            this.imageColorRgb = imageColorRgb;
        }
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