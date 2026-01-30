package com.example.backend.service;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.springframework.stereotype.Service;
import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService; // Added

@Service
public class CropService {
    private final SubMatrixService subMatrixService;
    private final GrayscaleService grayscaleService; // Added

    public CropService(SubMatrixService subMatrixService, GrayscaleService grayscaleService) {
        this.subMatrixService = subMatrixService;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse applyCrop(byte[] bytes, int x, int y, int w, int h, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        // Toggle Fix: Convert to grayscale BEFORE cropping 
        // This ensures the cropped pixels returned in the response are grayscale values
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        int srcW = img.getWidth();
        int srcH = img.getHeight();

        // Prevent out-of-bounds errors
        int actualW = Math.min(w, srcW - x);
        int actualH = Math.min(h, srcH - y);

        // Extracting raw pixel data from the (potentially grayscaled) image
        int[] pixels = img.getRGB(0, 0, srcW, srcH, null, 0, srcW);
        int[] croppedPixels = subMatrixService.extractSubMatrix(pixels, srcW, srcH, x, y, actualW, actualH);

        

        BufferedImage resultImg = new BufferedImage(actualW, actualH, BufferedImage.TYPE_INT_ARGB);
        resultImg.setRGB(0, 0, actualW, actualH, croppedPixels, 0, actualW);

        return new ImageMatrixResponse(
            ImageUtil.encode(resultImg), 
            croppedPixels, 
            actualW, 
            actualH
        );
    }
}