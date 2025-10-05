package com.spriteconverter.pixelate_api.service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class PixelationService {

    public BufferedImage pixelateByFactor(BufferedImage originalImage, int pixelSize, boolean upscaleBack) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate downscaled dimensions
        int downscaledWidth = originalWidth / pixelSize;
        int downscaledHeight = originalHeight / pixelSize;

        // Ensure we have at least 1x1 pixel
        if (downscaledWidth < 1) downscaledWidth = 1;
        if (downscaledHeight < 1) downscaledHeight = 1;

        return scaleImage(originalImage, downscaledWidth, downscaledHeight, originalWidth, originalHeight, upscaleBack);
    }

    public BufferedImage pixelateToWidth(BufferedImage originalImage, int targetWidth, boolean upscaleBack) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate aspect ratio
        double aspectRatio = (double) originalHeight / originalWidth;

        // Calculate target height maintaining aspect ratio
        int targetHeight = (int) Math.round(targetWidth * aspectRatio);

        // Ensure at least 1 pixel
        if (targetHeight < 1) targetHeight = 1;

        return scaleImage(originalImage, targetWidth, targetHeight, originalWidth, originalHeight, upscaleBack);
    }

    public BufferedImage pixelateToHeight(BufferedImage originalImage, int targetHeight, boolean upscaleBack) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;

        // Calculate target width maintaining aspect ratio
        int targetWidth = (int) Math.round(targetHeight * aspectRatio);

        // Ensure at least 1 pixel
        if (targetWidth < 1) targetWidth = 1;

        return scaleImage(originalImage, targetWidth, targetHeight, originalWidth, originalHeight, upscaleBack);
    }

    private BufferedImage scaleImage(BufferedImage originalImage,
                                     int targetWidth,
                                     int targetHeight,
                                     int originalWidth,
                                     int originalHeight,
                                     boolean upscaleBack) {

        // Downscale using nearest-neighbor (no interpolation)
        BufferedImage downscaled = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = downscaled.createGraphics();
        // CRITICAL: Use nearest-neighbor to avoid blurring
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        // If requested, upscale back to original size (still using nearest-neighbor)
        if (upscaleBack) {
            BufferedImage upscaled = new BufferedImage(
                    originalWidth,
                    originalHeight,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2dUpscale = upscaled.createGraphics();
            g2dUpscale.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2dUpscale.drawImage(downscaled, 0, 0, originalWidth, originalHeight, null);
            g2dUpscale.dispose();

            return upscaled;
        }

        return downscaled;
    }
}