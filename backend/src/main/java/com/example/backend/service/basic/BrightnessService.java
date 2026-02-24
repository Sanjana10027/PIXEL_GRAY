package com.example.backend.service.basic;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class BrightnessService {

    private final GrayscaleService grayscaleService;

    public BrightnessService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    /**
     * Updated apply method that maintains original functionality 
     * but uses the high-performance matrix pipeline.
     */
    public ImageMatrixResponse apply(byte[] bytes, int level, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        // 1. Convert to initial linear matrix
        int[] matrix = LinearMatrixUtil.toLinear(img);

        // 2. Maintain original functionality: Apply grayscale if toggled
        // We call the matrix version of grayscale to keep the data flow clean
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // 3. Apply Brightness logic on the matrix
        int[] resultMatrix = processMatrix(matrix, level);

        // 4. Return the response including the final pixel matrix for your 5x5 frontend view
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, w, h)), 
            resultMatrix, 
            w, 
            h
        );
    }

    /**
     * Core matrix logic that can be reused by the Pipeline or other Services.
     */
    public int[] processMatrix(int[] matrix, int level) {
        int[] result = new int[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            int argb = matrix[i];

            int a = (argb >> 24) & 0xff;
            int r = (argb >> 16) & 0xff;
            int g = (argb >> 8) & 0xff;
            int b = argb & 0xff;

            // Apply level and clamp using ImageUtil
            r = ImageUtil.clamp(r + level);
            g = ImageUtil.clamp(g + level);
            b = ImageUtil.clamp(b + level);

            // Reassemble pixel
            result[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return result;
    }
}