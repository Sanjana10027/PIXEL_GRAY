package com.example.backend.service.matrix;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class LinearMatrixUtil {

    /**
     * Converts the entire image into a 1D array of ARGB integers.
     * Each integer contains [Alpha, Red, Green, Blue].
     */
    public static int[] toLinear(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // If image is already in the correct format, grab the internal buffer for speed
        if (img.getType() == BufferedImage.TYPE_INT_ARGB || img.getType() == BufferedImage.TYPE_INT_RGB) {
            return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        }

        int[] linear = new int[w * h];
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Returns the full 32-bit integer (AARRGGBB)
                linear[i++] = img.getRGB(x, y); 
            }
        }
        return linear;
    }

    /**
     * Reconstructs the image from the ARGB matrix.
     */
    public static BufferedImage fromLinear(int[] matrix, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] pixelData = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        System.arraycopy(matrix, 0, pixelData, 0, Math.min(matrix.length, pixelData.length));
        return img;
    }
}
