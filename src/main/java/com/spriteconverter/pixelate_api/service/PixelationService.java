package com.spriteconverter.pixelate_api.service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class PixelationService {

    public BufferedImage pixelate(BufferedImage originalImage, int pixelSize, boolean upscaleBack) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate downscaled dimensions
        int downscaledWidth = originalWidth / pixelSize;
        int downscaledHeight = originalHeight / pixelSize;

        // Ensure we have at least 1x1 pixel
        if (downscaledWidth < 1) downscaledWidth = 1;
        if (downscaledHeight < 1) downscaledHeight = 1;

        // Downscale using nearest-neighbor (no interpolation)
        BufferedImage downscaled = new BufferedImage(
                downscaledWidth,
                downscaledHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = downscaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(originalImage, 0, 0, downscaledWidth, downscaledHeight, null);
        g2d.dispose();

        // If requested, upscale back to original size
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

    // Overload for convenience - defaults to upscaling back for preview
    public BufferedImage pixelate(BufferedImage originalImage, int pixelSize) {
        return pixelate(originalImage, pixelSize, true);
    }
}
