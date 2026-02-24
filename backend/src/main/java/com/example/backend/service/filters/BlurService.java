package com.example.backend.service.filters;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class BlurService {

    private final GrayscaleService grayscaleService;

    public BlurService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    /**
     * Updated apply method: Preserves previous functionality (grayscale toggle, intensity)
     * but uses updated matrix-based processing for the pixel matrix.
     */
    public ImageMatrixResponse apply(byte[] bytes, int intensity, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        // 1. Convert to linear matrix
        int[] matrix = LinearMatrixUtil.toLinear(img);

        // 2. Functional Preservation: Grayscale first if toggled
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // 3. Process the blur using the matrix logic
        int[] resultMatrix = processMatrix(matrix, w, h, intensity);

        // 4. Return the response with the pixel matrix intact for the frontend
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, w, h)), 
            resultMatrix, 
            w, 
            h
        );
    }

    /**
     * Updated logic: Neighborhood convolution performed directly on the int[] matrix.
     * This ensures the 5x5 grid in the frontend shows accurately blurred ARGB values.
     */
    public int[] processMatrix(int[] matrix, int w, int h, int intensity) {
        if (intensity <= 0) return matrix;

        int[] result = new int[matrix.length];
        int size = Math.max(3, intensity * 2 + 1);
        int off = size / 2;

        

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                long sr = 0, sg = 0, sb = 0, sa = 0;
                int count = 0;

                for (int ky = -off; ky <= off; ky++) {
                    for (int kx = -off; kx <= off; kx++) {
                        int px = x + kx;
                        int py = y + ky;

                        if (px >= 0 && px < w && py >= 0 && py < h) {
                            int argb = matrix[py * w + px];
                            sa += (argb >> 24) & 0xff;
                            sr += (argb >> 16) & 0xff;
                            sg += (argb >> 8) & 0xff;
                            sb += argb & 0xff;
                            count++;
                        }
                    }
                }

                // Pack the averaged ARGB values back into the result matrix
                result[y * w + x] = ((int)(sa / count) << 24) | 
                                    ((int)(sr / count) << 16) | 
                                    ((int)(sg / count) << 8) | 
                                    (int)(sb / count);
            }
        }
        return result;
    }
}