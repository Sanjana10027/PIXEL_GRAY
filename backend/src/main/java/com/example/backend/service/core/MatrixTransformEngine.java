package com.example.backend.service.core;

import java.awt.image.BufferedImage;
import org.springframework.stereotype.Component;

@Component
public class MatrixTransformEngine {

    public BufferedImage applyTransform(BufferedImage original, double[][] m) {

        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        double cx = width / 2.0;
        double cy = height / 2.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double dx = x - cx;
                double dy = y - cy;

                double srcX = m[0][0] * dx + m[0][1] * dy + cx;
                double srcY = m[1][0] * dx + m[1][1] * dy + cy;

                int ix = (int) Math.round(srcX);
                int iy = (int) Math.round(srcY);

                if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
                    out.setRGB(x, y, original.getRGB(ix, iy));
                } else {
                    out.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }
        return out;
    }
}
