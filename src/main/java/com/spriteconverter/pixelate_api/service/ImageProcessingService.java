package com.spriteconverter.pixelate_api.service;

import com.spriteconverter.pixelate_api.model.ProcessingRequest;
import com.spriteconverter.pixelate_api.model.ProcessingResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageProcessingService {

    private final PixelationService pixelationService;
    private final ColorQuantizationService colorQuantizationService;
    private final PaletteService paletteService;
    private final BorderService borderService;

    public ImageProcessingService(PixelationService pixelationService, ColorQuantizationService colorQuantizationService, PaletteService paletteService, BorderService borderService) {
        this.pixelationService = pixelationService;
        this.colorQuantizationService = colorQuantizationService;
        this.paletteService = paletteService;
        this.borderService = borderService;
    }

    public ProcessingResponse processImage(MultipartFile file, ProcessingRequest request) throws IOException {

        // Convert MultipartFile to BufferedImage
        BufferedImage image = ImageIO.read(file.getInputStream());
        String originalSize = image.getWidth() + "x" + image.getHeight();

        BufferedImage processedImage = image;

        // Step 1: Pixelation
        if (request.getPixelSize() != null && request.getPixelSize() > 1) {
            processedImage = pixelationService.pixelateByFactor(
                    processedImage,
                    request.getPixelSize(),
                    request.getUpscaleBack()
            );
        } else if (request.getTargetWidth() != null && request.getTargetWidth() > 0) {
            processedImage = pixelationService.pixelateToWidth(
                    processedImage,
                    request.getTargetWidth(),
                    request.getUpscaleBack()
            );
        } else if (request.getTargetHeight() != null && request.getTargetHeight() > 0) {
            processedImage = pixelationService.pixelateToHeight(
                    processedImage,
                    request.getTargetHeight(),
                    request.getUpscaleBack()
            );
        }

        // Step 2: Color Quantization
        if (request.getColorCount() != null && request.getColorCount() > 0) {
            processedImage = colorQuantizationService.quantize(
                    processedImage,
                    request.getColorCount(),
                    request.getQuantizationStrategy()
            );
        }

        // Step 3: Palette Application
        if (request.getPalette() != null && !request.getPalette().isEmpty()) {
            boolean autoMapping = request.getAutoColorMapping() != null ? request.getAutoColorMapping() : true;
            processedImage = paletteService.applyPalette(processedImage, request.getPalette(), autoMapping);
        }

        // Step 4: Border
        if (request.getBorderThickness() != null && request.getBorderColor() != null) {
            processedImage = borderService.addBorder(
                    processedImage,
                    request.getBorderThickness(),
                    request.getBorderColor()
            );
        }

        List<String> colors = extractColors(processedImage);

        String base64Image = convertToBase64(processedImage);
        String processedSize = processedImage.getWidth() + "x" + processedImage.getHeight();

        ProcessingResponse.ImageMetadata metadata = new ProcessingResponse.ImageMetadata(
                originalSize,
                processedSize,
                colors.size(),
                colors
        );

        return new ProcessingResponse(base64Image, metadata);
    }

    private List<String> extractColors(BufferedImage image) {
        Map<String, Integer> colorFrequency = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                if (color.getAlpha() < 128) continue;

                String hex = String.format("#%02x%02x%02x",
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue()
                ).toUpperCase();

                colorFrequency.put(hex, colorFrequency.getOrDefault(hex, 0) + 1);
            }
        }

        return colorFrequency.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}