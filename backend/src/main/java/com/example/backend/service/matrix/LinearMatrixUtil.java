package com.example.backend.service.matrix;

import java.awt.image.BufferedImage;

public class LinearMatrixUtil {

    // Preserve full RGB integer instead of just the Blue channel
    public static int[] toLinear(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[] linear = new int[w * h];
        int i = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Get the full 32-bit ARGB value
                linear[i++] = img.getRGB(x, y); 
            }
        }
        return linear;
    }
}

