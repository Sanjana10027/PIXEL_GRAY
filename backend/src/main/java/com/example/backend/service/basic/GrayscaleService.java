package com.example.backend.service.basic;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class GrayscaleService {

    /**
     * Entry point for Controller (MultipartFile -> Response)
     * Keeps previous functionality but uses updated matrix logic.
     */
    public ImageMatrixResponse apply(byte[] bytes) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        // Use the updated matrix processor
        int[] resultMatrix = processMatrix(LinearMatrixUtil.toLinear(img));

        return new ImageMatrixResponse(
                ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, w, h)),
                resultMatrix,
                w,
                h
        );
    }

    /**
     * Entry point for other Services (BufferedImage -> BufferedImage)
     * Keeps previous functionality for service-to-service calls.
     */
    public BufferedImage process(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        
        int[] matrix = LinearMatrixUtil.toLinear(img);
        int[] resultMatrix = processMatrix(matrix);
        
        return LinearMatrixUtil.fromLinear(resultMatrix, w, h);
    }

    /**
     * Core Logic: Process raw ARGB matrix.
     * This is the "Updated Code" that drives everything else.
     */
    public int[] processMatrix(int[] matrix) {
        int[] result = new int[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            int argb = matrix[i];

            // Extract channels
            int a = (argb >> 24) & 0xff;
            int r = (argb >> 16) & 0xff;
            int g = (argb >> 8) & 0xff;
            int b = argb & 0xff;

            // Updated Grayscale Math (Luminosity)
            int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            gray = ImageUtil.clamp(gray);

            // Pack back to ARGB - This ensures the 5x5 grid in frontend is correct
            result[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }
        return result;
    }
}