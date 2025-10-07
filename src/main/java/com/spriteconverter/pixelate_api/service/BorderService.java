package com.spriteconverter.pixelate_api.service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class BorderService {

    public BufferedImage addBorder(BufferedImage image, int thickness, String hexColor) {
        if (thickness <= 0) {
            return image;
        }

        int borderColor = hexToRgb(hexColor);

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        boolean[][] needsBorder = new boolean[image.getWidth()][image.getHeight()];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);

                if (pixel.getAlpha() >= 128) {
                    if (hasTransparentNeighbor(image, x, y, thickness)) {
                        needsBorder[x][y] = true;
                    }
                }
            }
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color originalPixel = new Color(image.getRGB(x, y), true);

                if (originalPixel.getAlpha() >= 128) {
                    result.setRGB(x, y, image.getRGB(x, y));
                } else {
                    if (shouldDrawBorder(needsBorder, x, y, thickness)) {
                        result.setRGB(x, y, borderColor);
                    } else {
                        result.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                    }
                }
            }
        }

        return result;
    }

    private boolean hasTransparentNeighbor(BufferedImage image, int x, int y, int thickness) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int dy = -thickness; dy <= thickness; dy++) {
            for (int dx = -thickness; dx <= thickness; dx++) {
                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    Color neighbor = new Color(image.getRGB(nx, ny), true);
                    if (neighbor.getAlpha() < 128) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldDrawBorder(boolean[][] needsBorder, int x, int y, int thickness) {
        int width = needsBorder.length;
        int height = needsBorder[0].length;

        for (int dy = -thickness; dy <= thickness; dy++) {
            for (int dx = -thickness; dx <= thickness; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (needsBorder[nx][ny]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int hexToRgb(String hex) {
        hex = hex.replace("#", "");

        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return new Color(r, g, b, 255).getRGB();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color format: " + hex, e);
        }
    }
}