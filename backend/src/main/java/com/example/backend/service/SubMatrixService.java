package com.example.backend.service;
import org.springframework.stereotype.Service;

@Service
public class SubMatrixService {
 
    // Extract submatrix from a pixel matrix (1D array)
    public int[] extractSubMatrix(int[] pixels, int width, int height, int x, int y, int w, int h) {
        int[] result = new int[w * h];
 
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int srcIndex = (y + row) * width + (x + col);
                int destIndex = row * w + col;
                result[destIndex] = pixels[srcIndex];
            }
        }
        return result;
    }
}
 