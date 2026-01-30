// package com.example.backend.service.core;

// import java.awt.image.BufferedImage;
// import org.springframework.stereotype.Component;

// @Component
// public class MatrixTransformEngine {

//     public BufferedImage applyTransform(BufferedImage original, double[][] m) {

//         int width = original.getWidth();
//         int height = original.getHeight();

//         BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

//         double cx = width / 2.0;
//         double cy = height / 2.0;

//         for (int y = 0; y < height; y++) {
//             for (int x = 0; x < width; x++) {

//                 double dx = x - cx;
//                 double dy = y - cy;

//                 double srcX = m[0][0] * dx + m[0][1] * dy + cx;
//                 double srcY = m[1][0] * dx + m[1][1] * dy + cy;

//                 int ix = (int) Math.round(srcX);
//                 int iy = (int) Math.round(srcY);

//                 if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
//                     out.setRGB(x, y, original.getRGB(ix, iy));
//                 } else {
//                     out.setRGB(x, y, 0xFFFFFFFF);
//                 }
//             }
//         }
//         return out;
//     }
// }



package com.example.backend.service.core;

import java.awt.image.BufferedImage;
import org.springframework.stereotype.Component;

@Component
public class MatrixTransformEngine {

    public BufferedImage applyTransform(BufferedImage original, double[][] m, int destWidth, int destHeight) {
        BufferedImage out = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_ARGB);

        // Center points for rotation/scaling relative to their respective sizes
        double srcCx = original.getWidth() / 2.0;
        double srcCy = original.getHeight() / 2.0;
        double destCx = destWidth / 2.0;
        double destCy = destHeight / 2.0;

        for (int y = 0; y < destHeight; y++) {
            for (int x = 0; x < destWidth; x++) {

                // Translate to origin (relative to destination center)
                double dx = x - destCx;
                double dy = y - destCy;

                // Apply inverse transformation to find source coordinates
                double srcX = m[0][0] * dx + m[0][1] * dy + srcCx;
                double srcY = m[1][0] * dx + m[1][1] * dy + srcCy;

                // Nearest Neighbor Interpolation (using floor to get exact pixel blocks)
                int ix = (int) Math.floor(srcX);
                int iy = (int) Math.floor(srcY);

                if (ix >= 0 && ix < original.getWidth() && iy >= 0 && iy < original.getHeight()) {
                    out.setRGB(x, y, original.getRGB(ix, iy));
                } else {
                    // Background color for out-of-bounds (e.g., after rotation)
                    out.setRGB(x, y, 0xFFFFFFFF); 
                }
            }
        }
        return out;
    }
}