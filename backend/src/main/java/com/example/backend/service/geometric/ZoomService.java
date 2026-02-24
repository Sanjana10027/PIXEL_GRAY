
package com.example.backend.service.geometric;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class ZoomService {
    private final GrayscaleService grayscaleService;

    public ZoomService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    /**
     * Entry point for Zoom. 
     * Returns a response where the pixel matrix matches the new scaled dimensions.
     */
    public ImageMatrixResponse apply(byte[] bytes, double scale, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int srcW = img.getWidth();
        int srcH = img.getHeight();

        // 1. Convert to matrix and handle grayscale toggle
        int[] srcMatrix = LinearMatrixUtil.toLinear(img);
        if (grayscale) {
            srcMatrix = grayscaleService.processMatrix(srcMatrix);
        }

        // 2. Calculate target dimensions
        int newW = (int) Math.max(1, srcW * scale);
        int newH = (int) Math.max(1, srcH * scale);

        // 3. Process the scaling on the matrix
        int[] resultMatrix = processMatrix(srcMatrix, srcW, srcH, newW, newH, scale);

        // 4. Return response with the newly sized matrix for the frontend 5x5 grid
        return new ImageMatrixResponse(
            ImageUtil.encode(LinearMatrixUtil.fromLinear(resultMatrix, newW, newH)), 
            resultMatrix, 
            newW, 
            newH
        );
    }

    /**
     * Core Scaling Logic (Nearest Neighbor)
     * Maps destination coordinates back to source coordinates.
     */
    public int[] processMatrix(int[] srcMatrix, int srcW, int srcH, int newW, int newH, double scale) {
        int[] result = new int[newW * newH];
        
        // Inverse of the scale factor
        double invScale = 1.0 / scale;
        
        // Centers for coordinate mapping
        double cx = srcW / 2.0;
        double cy = srcH / 2.0;
        double nCx = newW / 2.0;
        double nCy = newH / 2.0;

        

        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                // Map current (x, y) back to source coordinates relative to center
                double dx = x - nCx;
                double dy = y - nCy;

                int srcX = (int) Math.floor(invScale * dx + cx);
                int srcY = (int) Math.floor(invScale * dy + cy);

                if (srcX >= 0 && srcX < srcW && srcY >= 0 && srcY < srcH) {
                    result[y * newW + x] = srcMatrix[srcY * srcW + srcX];
                } else {
                    // Padding color (White/Transparent)
                    result[y * newW + x] = 0xFFFFFFFF; 
                }
            }
        }
        return result;
    }
}