package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class SubMatrixService {

    /**
     * Extracts a submatrix from a 1D ARGB pixel array.
     * Optimized using System.arraycopy for high-speed row-based memory transfer.
     */
    public int[] extractSubMatrix(int[] pixels, int width, int height, int x, int y, int w, int h) {
        // Validation to prevent ArrayIndexOutOfBounds
        int actualW = Math.min(w, width - x);
        int actualH = Math.min(h, height - y);
        
        int[] result = new int[actualW * actualH];

        

        for (int row = 0; row < actualH; row++) {
            // Calculate starting positions for the source and destination
            int srcPos = (y + row) * width + x;
            int destPos = row * actualW;

            // Move the entire row of pixels in one native operation
            System.arraycopy(pixels, srcPos, result, destPos, actualW);
        }
        
        return result;
    }
}