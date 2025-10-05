package com.spriteconverter.pixelate_api.controller;

import com.spriteconverter.pixelate_api.model.ProcessingResponse;
import com.spriteconverter.pixelate_api.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/process")
public class ImageProcessingController {

    private final ImageProcessingService imageProcessingService;

    public ImageProcessingController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    @PostMapping
    public ResponseEntity<ProcessingResponse> processSingleImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false) Integer pixelSize,
            @RequestParam(required = false) Integer colorCount,
            @RequestParam(required = false) String paletteId,
            @RequestParam(required = false) Integer borderThickness,
            @RequestParam(required = false) String borderColor,
            @RequestParam(required = false, defaultValue = "true") Boolean upscaleBack
    ) throws IOException {

        ProcessingResponse response = imageProcessingService.processImage(
                image, pixelSize, colorCount, paletteId, borderThickness, borderColor, upscaleBack
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProcessingResponse>> processBatchImages(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(required = false) Integer pixelSize,
            @RequestParam(required = false) Integer colorCount,
            @RequestParam(required = false) String paletteId,
            @RequestParam(required = false) Integer borderThickness,
            @RequestParam(required = false) String borderColor,
            @RequestParam(required = false, defaultValue = "true") Boolean upscaleBack
    ) throws IOException {

        List<ProcessingResponse> responses = new ArrayList<>();

        for (MultipartFile image : images) {
            ProcessingResponse response = imageProcessingService.processImage(
                    image, pixelSize, colorCount, paletteId, borderThickness, borderColor, upscaleBack
            );
            responses.add(response);
        }

        return ResponseEntity.ok(responses);
    }
}
