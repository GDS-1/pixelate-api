package com.spriteconverter.pixelate_api.controller;

import com.spriteconverter.pixelate_api.model.ProcessingRequest;
import com.spriteconverter.pixelate_api.model.ProcessingResponse;
import com.spriteconverter.pixelate_api.service.ImageProcessingService;
import jakarta.validation.Valid;
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
            @ModelAttribute @Valid ProcessingRequest request
    ) throws IOException {

        ProcessingResponse response = imageProcessingService.processImage(image, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProcessingResponse>> processBatchImages(
            @RequestParam("images") List<MultipartFile> images,
            @ModelAttribute @Valid ProcessingRequest request
    ) throws IOException {

        List<ProcessingResponse> responses = new ArrayList<>();

        for (MultipartFile image : images) {
            ProcessingResponse response = imageProcessingService.processImage(image, request);
            responses.add(response);
        }

        return ResponseEntity.ok(responses);
    }
}

