package com.example.backend.service.basic;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class ContrastService {

    private final GrayscaleService grayscaleService;

    // Maintain original Dependency Injection
    public ContrastService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    /**
     * Updated apply: Keeps functional flow but uses optimized matrix processing.
     * Ensures the resultMatrix is returned to the frontend for the 5x5 pixel view.
     */
    public ImageMatrixResponse apply(byte[] bytes, int level, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        // 1. Convert source to matrix
        int[] matrix = LinearMatrixUtil.toLinear(img);

        // 2. Functional Preservation: Apply grayscale if toggled
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // 3. Apply contrast logic on the matrix
        int[] resultMatrix = processMatrix(matrix, level);

        // 4. Return response with preview image and the full processed matrix
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, w, h)),
            resultMatrix,
            w,
            h
        );
    }

    /**
     * Core matrix logic. Performs contrast adjustment directly on the int[] array.
     * This method can be reused by the ImagePipelineService.
     */
    public int[] processMatrix(int[] matrix, int level) {
        int[] result = new int[matrix.length];
        
        // Calculate contrast factor once
        double factor = (259.0 * (level + 255)) / (255.0 * (259 - level));

        

        for (int i = 0; i < matrix.length; i++) {
            int argb = matrix[i];

            // Preserve original alpha
            int a = (argb >> 24) & 0xff;
            int r = (argb >> 16) & 0xff;
            int g = (argb >> 8) & 0xff;
            int b = argb & 0xff;

            // Apply contrast factor and clamp
            r = ImageUtil.clamp((int)(factor * (r - 128) + 128));
            g = ImageUtil.clamp((int)(factor * (g - 128) + 128));
            b = ImageUtil.clamp((int)(factor * (b - 128) + 128));

            // Pack back into ARGB
            result[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return result;
    }
}