package com.example.backend.service;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService;
import com.example.backend.service.SubMatrixService;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class CropService {
    private final SubMatrixService subMatrixService;
    private final GrayscaleService grayscaleService;

    public CropService(SubMatrixService subMatrixService, GrayscaleService grayscaleService) {
        this.subMatrixService = subMatrixService;
        this.grayscaleService = grayscaleService;
    }

    /**
     * Updated applyCrop: Maintains original functionality (Grayscale toggle + Bounds check)
     * but operates on the matrix to ensure the 5x5 grid in the frontend is accurate.
     */
    public ImageMatrixResponse applyCrop(byte[] bytes, int x, int y, int w, int h, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int srcW = img.getWidth();
        int srcH = img.getHeight();

        // 1. Convert source to matrix
        int[] matrix = LinearMatrixUtil.toLinear(img);

        // 2. Functional Preservation: Grayscale before cropping if toggled
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // 3. Prevent out-of-bounds errors (Logic remains the same)
        int actualW = Math.min(w, srcW - x);
        int actualH = Math.min(h, srcH - y);
        
        // Ensure coordinates are not negative
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);

        // 4. Extract cropped matrix using SubMatrixService
        int[] croppedPixels = subMatrixService.extractSubMatrix(matrix, srcW, srcH, startX, startY, actualW, actualH);

        // 5. Build response using the processed matrix
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(croppedPixels, actualW, actualH)), 
            croppedPixels, 
            actualW, 
            actualH
        );
    }

    /**
     * Matrix-only processing for the ImagePipelineService.
     */
    public int[] processMatrix(int[] matrix, int srcW, int srcH, int x, int y, int w, int h) {
        // Ensure we don't start outside the image
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        
        // Ensure we don't end outside the image
        int actualW = Math.min(w, srcW - startX);
        int actualH = Math.min(h, srcH - startY);
        
        // Handle edge case where w or h might result in zero/negative size
        if (actualW <= 0 || actualH <= 0) {
            return new int[0];
        }

        return subMatrixService.extractSubMatrix(matrix, srcW, srcH, startX, startY, actualW, actualH);
    }
}