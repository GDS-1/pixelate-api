package com.spriteconverter.pixelate_api.service;

import com.spriteconverter.pixelate_api.model.ProcessingRequest;
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
    private final PaletteService paletteService;

    public ImageProcessingService(PixelationService pixelationService, ColorQuantizationService colorQuantizationService, PaletteService paletteService) {
        this.pixelationService = pixelationService;
        this.colorQuantizationService = colorQuantizationService;
        this.paletteService = paletteService;
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

        // Step 4: Border (TODO)
        // if (request.getBorderThickness() != null && request.getBorderColor() != null) {
        //     processedImage = borderService.addBorder(...);
        // }

        // Convert to response...
        String base64Image = convertToBase64(processedImage);
        String processedSize = processedImage.getWidth() + "x" + processedImage.getHeight();

        ProcessingResponse.ImageMetadata metadata = new ProcessingResponse.ImageMetadata(
                originalSize,
                processedSize,
                null
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