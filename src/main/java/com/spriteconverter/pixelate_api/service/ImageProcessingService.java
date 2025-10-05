package com.spriteconverter.pixelate_api.service;

import com.spriteconverter.pixelate_api.model.ProcessingResponse;
import com.spriteconverter.pixelate_api.model.QuantizationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class ImageProcessingService {

    private final PixelationService pixelationService;
    private final ColorQuantizationService colorQuantizationService;

    public ImageProcessingService(PixelationService pixelationService, ColorQuantizationService colorQuantizationService) {
        this.pixelationService = pixelationService;
        this.colorQuantizationService = colorQuantizationService;
    }

    public ProcessingResponse processImage(
            MultipartFile file,
            Integer pixelSize,
            Integer targetWidth,
            Integer targetHeight,
            Boolean upscaleBack,
            Integer colorCount,
            QuantizationStrategy quantizationStrategy,
            String paletteId,
            Integer borderThickness,
            String borderColor
    ) throws IOException {

        BufferedImage image = ImageIO.read(file.getInputStream());
        String originalSize = image.getWidth() + "x" + image.getHeight();

        BufferedImage processedImage = image;

        // Step 1: Pixelation
        if (pixelSize != null && pixelSize > 1) {
            boolean shouldUpscale = upscaleBack != null ? upscaleBack : true;
            processedImage = pixelationService.pixelateByFactor(processedImage, pixelSize, shouldUpscale);
        } else if (targetWidth != null && targetWidth > 0) {
            boolean shouldUpscale = upscaleBack != null ? upscaleBack : true;
            processedImage = pixelationService.pixelateToWidth(processedImage, targetWidth, shouldUpscale);

        } else if (targetHeight != null && targetHeight > 0) {
            boolean shouldUpscale = upscaleBack != null ? upscaleBack : true;
            processedImage = pixelationService.pixelateToHeight(processedImage, targetHeight, shouldUpscale);
        }

        // Step 2: Color Quantization (TODO)
        if (colorCount != null && colorCount > 0 && colorCount < 257) {
            processedImage = colorQuantizationService.quantize(
                    processedImage,
                    colorCount,
                    quantizationStrategy
            );
        }

        // Step 3: Palette Application (TODO)
        // if (paletteId != null) {
        //     processedImage = paletteService.applyPalette(processedImage, paletteId);
        // }

        // Step 4: Border (TODO)
        // if (borderThickness != null && borderColor != null) {
        //     processedImage = borderService.addBorder(processedImage, borderThickness, borderColor);
        // }

        // Convert processed image to base64
        String base64Image = convertToBase64(processedImage);
        String processedSize = processedImage.getWidth() + "x" + processedImage.getHeight();

        // Create metadata
        ProcessingResponse.ImageMetadata metadata = new ProcessingResponse.ImageMetadata(
                originalSize,
                processedSize,
                null // TODO: Calculate colors used
        );

        return new ProcessingResponse(base64Image, metadata);
    }

    private String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}