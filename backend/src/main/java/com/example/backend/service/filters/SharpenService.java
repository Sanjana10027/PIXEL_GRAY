package com.example.backend.service.filters;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class SharpenService {

    private final GrayscaleService grayscaleService;

    // Keep original Dependency Injection
    public SharpenService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    /**
     * Updated apply: Keeps functionality but uses matrix-based processing.
     * Ensures the resultMatrix is returned to the frontend for the 5x5 grid.
     */
    public ImageMatrixResponse apply(byte[] bytes, int intensity, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        // 1. Convert source to matrix
        int[] matrix = LinearMatrixUtil.toLinear(img);

        // 2. Functional Preservation: Toggle Grayscale BEFORE sharpening
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // 3. Apply sharpening kernel on the matrix
        int[] resultMatrix = processMatrix(matrix, w, h, intensity);

        // 4. Return result with updated pixel matrix
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, w, h)),
            resultMatrix,
            w,
            h
        );
    }

    /**
     * Core matrix logic. Performs a 3x3 convolution directly on the int[] array.
     */
    public int[] processMatrix(int[] matrix, int w, int h, int intensity) {
        int[] result = new int[matrix.length];
        
        // Define Kernel: Center value increases with intensity
        int center = 5 + intensity;
        int[][] kernel = {
            {0, -1, 0}, 
            {-1, center, -1}, 
            {0, -1, 0}
        };

        

        // Loop through pixels (skipping boundaries to avoid out-of-bounds)
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int r = 0, g = 0, b = 0;
                
                // Preserve original alpha
                int alpha = (matrix[y * w + x] >> 24) & 0xff;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int argb = matrix[(y + ky) * w + (x + kx)];
                        int weight = kernel[ky + 1][kx + 1];

                        r += ((argb >> 16) & 255) * weight;
                        g += ((argb >> 8) & 255) * weight;
                        b += (argb & 255) * weight;
                    }
                }

                // Pack sharpened pixel with clamping
                result[y * w + x] = (alpha << 24) | 
                                    (ImageUtil.clamp(r) << 16) | 
                                    (ImageUtil.clamp(g) << 8) | 
                                    ImageUtil.clamp(b);
            }
        }
        
        // Fill boundaries to prevent black edges (copies from source)
        fillBoundaries(matrix, result, w, h);
        
        return result;
    }

    private void fillBoundaries(int[] src, int[] dst, int w, int h) {
        for (int x = 0; x < w; x++) {
            dst[x] = src[x]; // Top row
            dst[(h - 1) * w + x] = src[(h - 1) * w + x]; // Bottom row
        }
        for (int y = 0; y < h; y++) {
            dst[y * w] = src[y * w]; // Left col
            dst[y * w + (w - 1)] = src[y * w + (w - 1)]; // Right col
        }
    }
}