// // package com.example.backend.service;

// // import com.example.backend.models.ImageMatrixResponse;
// // import com.example.backend.service.matrix.LinearMatrixUtil;
// // import com.example.backend.service.util.ImageUtil;
// // import org.springframework.stereotype.Service;
// // import java.awt.image.BufferedImage;
// // import java.io.IOException;

// // @Service
// // public class BackgroundRemovalService {

// //     public ImageMatrixResponse apply(byte[] bytes) throws IOException {
// //         BufferedImage img = ImageUtil.decode(bytes);
// //         int width = img.getWidth();
// //         int height = img.getHeight();
        
// //         // Create output image with alpha channel
// //         BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
// //         // Step 1: Detect background color (use corner pixels as reference)
// //         int bgColor = detectBackgroundColor(img);
        
// //         // Step 2: Calculate color similarity threshold
// //         int threshold = 40; // Adjust this value for sensitivity (lower = stricter)
        
// //         // Step 3: Process each pixel
// //         for (int y = 0; y < height; y++) {
// //             for (int x = 0; x < width; x++) {
// //                 int pixel = img.getRGB(x, y);
                
// //                 // Check if pixel is similar to background color
// //                 if (isColorSimilar(pixel, bgColor, threshold)) {
// //                     // Make pixel transparent
// //                     result.setRGB(x, y, 0x00000000); // Fully transparent
// //                 } else {
// //                     // Keep pixel as is (with full alpha)
// //                     int r = (pixel >> 16) & 0xFF;
// //                     int g = (pixel >> 8) & 0xFF;
// //                     int b = pixel & 0xFF;
// //                     result.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
// //                 }
// //             }
// //         }
        
// //         return new ImageMatrixResponse(
// //             ImageUtil.encode(result),
// //             LinearMatrixUtil.toLinear(result),
// //             width,
// //             height
// //         );
// //     }
    
// //     /**
// //      * Detect background color by sampling corner pixels
// //      */
// //     private int detectBackgroundColor(BufferedImage img) {
// //         int width = img.getWidth();
// //         int height = img.getHeight();
        
// //         // Sample corners and edges
// //         int[] samples = new int[8];
// //         samples[0] = img.getRGB(0, 0); // Top-left corner
// //         samples[1] = img.getRGB(width - 1, 0); // Top-right corner
// //         samples[2] = img.getRGB(0, height - 1); // Bottom-left corner
// //         samples[3] = img.getRGB(width - 1, height - 1); // Bottom-right corner
// //         samples[4] = img.getRGB(width / 2, 0); // Top center
// //         samples[5] = img.getRGB(width / 2, height - 1); // Bottom center
// //         samples[6] = img.getRGB(0, height / 2); // Left center
// //         samples[7] = img.getRGB(width - 1, height / 2); // Right center
        
// //         // Find most common color among samples
// //         return findMostCommonColor(samples);
// //     }
    
// //     /**
// //      * Find the most common color in an array of RGB values
// //      */
// //     private int findMostCommonColor(int[] colors) {
// //         int maxCount = 0;
// //         int mostCommon = colors[0];
        
// //         for (int i = 0; i < colors.length; i++) {
// //             int count = 0;
// //             for (int j = 0; j < colors.length; j++) {
// //                 if (isColorSimilar(colors[i], colors[j], 30)) {
// //                     count++;
// //                 }
// //             }
// //             if (count > maxCount) {
// //                 maxCount = count;
// //                 mostCommon = colors[i];
// //             }
// //         }
        
// //         return mostCommon;
// //     }
    
// //     /**
// //      * Check if two colors are similar within a threshold
// //      */
// //     private boolean isColorSimilar(int color1, int color2, int threshold) {
// //         int r1 = (color1 >> 16) & 0xFF;
// //         int g1 = (color1 >> 8) & 0xFF;
// //         int b1 = color1 & 0xFF;
        
// //         int r2 = (color2 >> 16) & 0xFF;
// //         int g2 = (color2 >> 8) & 0xFF;
// //         int b2 = color2 & 0xFF;
        
// //         // Calculate Euclidean distance in RGB space
// //         int rDiff = r1 - r2;
// //         int gDiff = g1 - g2;
// //         int bDiff = b1 - b2;
        
// //         double distance = Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
        
// //         return distance <= threshold;
// //     }
// // }









// package com.example.backend.service;

// import com.example.backend.models.ImageMatrixResponse;
// import com.example.backend.service.matrix.LinearMatrixUtil;
// import com.example.backend.service.util.ImageUtil;
// import org.springframework.stereotype.Service;
// import java.awt.image.BufferedImage;
// import java.io.IOException;

// @Service
// public class BackgroundRemovalService {

//     public ImageMatrixResponse apply(byte[] bytes) throws IOException {
//         BufferedImage img = ImageUtil.decode(bytes);
//         BufferedImage result = processImage(img);
        
//         return new ImageMatrixResponse(
//             ImageUtil.encode(result),
//             LinearMatrixUtil.toLinear(result),
//             result.getWidth(),
//             result.getHeight()
//         );
//     }

//     /**
//      * Process a BufferedImage directly (for pipeline use)
//      */
//     public BufferedImage processImage(BufferedImage img) {
//         int width = img.getWidth();
//         int height = img.getHeight();
        
//         // Create output image with alpha channel
//         BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
//         // Step 1: Detect background color (use corner pixels as reference)
//         int bgColor = detectBackgroundColor(img);
        
//         // Step 2: Calculate color similarity threshold
//         int threshold = 40; // Adjust this value for sensitivity (lower = stricter)
        
//         // Step 3: Process each pixel
//         for (int y = 0; y < height; y++) {
//             for (int x = 0; x < width; x++) {
//                 int pixel = img.getRGB(x, y);
                
//                 // Check if pixel is similar to background color
//                 if (isColorSimilar(pixel, bgColor, threshold)) {
//                     // Make pixel transparent
//                     result.setRGB(x, y, 0x00000000); // Fully transparent
//                 } else {
//                     // Keep pixel as is (with full alpha)
//                     int r = (pixel >> 16) & 0xFF;
//                     int g = (pixel >> 8) & 0xFF;
//                     int b = pixel & 0xFF;
//                     result.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
//                 }
//             }
//         }
        
//         return result;
//     }
    
//     /**
//      * Detect background color by sampling corner pixels
//      */
//     private int detectBackgroundColor(BufferedImage img) {
//         int width = img.getWidth();
//         int height = img.getHeight();
        
//         // Sample corners and edges
//         int[] samples = new int[8];
//         samples[0] = img.getRGB(0, 0); // Top-left corner
//         samples[1] = img.getRGB(width - 1, 0); // Top-right corner
//         samples[2] = img.getRGB(0, height - 1); // Bottom-left corner
//         samples[3] = img.getRGB(width - 1, height - 1); // Bottom-right corner
//         samples[4] = img.getRGB(width / 2, 0); // Top center
//         samples[5] = img.getRGB(width / 2, height - 1); // Bottom center
//         samples[6] = img.getRGB(0, height / 2); // Left center
//         samples[7] = img.getRGB(width - 1, height / 2); // Right center
        
//         // Find most common color among samples
//         return findMostCommonColor(samples);
//     }
    
//     /**
//      * Find the most common color in an array of RGB values
//      */
//     private int findMostCommonColor(int[] colors) {
//         int maxCount = 0;
//         int mostCommon = colors[0];
        
//         for (int i = 0; i < colors.length; i++) {
//             int count = 0;
//             for (int j = 0; j < colors.length; j++) {
//                 if (isColorSimilar(colors[i], colors[j], 30)) {
//                     count++;
//                 }
//             }
//             if (count > maxCount) {
//                 maxCount = count;
//                 mostCommon = colors[i];
//             }
//         }
        
//         return mostCommon;
//     }
    
//     /**
//      * Check if two colors are similar within a threshold
//      */
//     private boolean isColorSimilar(int color1, int color2, int threshold) {
//         int r1 = (color1 >> 16) & 0xFF;
//         int g1 = (color1 >> 8) & 0xFF;
//         int b1 = color1 & 0xFF;
        
//         int r2 = (color2 >> 16) & 0xFF;
//         int g2 = (color2 >> 8) & 0xFF;
//         int b2 = color2 & 0xFF;
        
//         // Calculate Euclidean distance in RGB space
//         int rDiff = r1 - r2;
//         int gDiff = g1 - g2;
//         int bDiff = b1 - b2;
        
//         double distance = Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
        
//         return distance <= threshold;
//     }
// }





package com.example.backend.service;

import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;

/**
 * Simple background removal using color-based segmentation
 * No external libraries - pure Java implementation
 */
@Service
public class BackgroundRemovalService {

    private static final int DEFAULT_THRESHOLD = 30;
    private static final int EDGE_SAMPLES = 20;

    /**
     * Remove background by detecting dominant edge color
     */
    public BufferedImage processImage(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Detect background color from edges
        int backgroundColor = detectBackgroundColor(input);

        // Create output with transparency
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = input.getRGB(x, y);
                
                if (isSimilarColor(pixel, backgroundColor, DEFAULT_THRESHOLD)) {
                    // Make background transparent
                    output.setRGB(x, y, 0x00000000);
                } else {
                    // Keep foreground pixel
                    output.setRGB(x, y, pixel | 0xFF000000);
                }
            }
        }

        return output;
    }

    /**
     * Detect most common color from image edges
     */
    private int detectBackgroundColor(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        
        java.util.Map<Integer, Integer> colorCount = new java.util.HashMap<>();

        // Sample top edge
        for (int x = 0; x < width; x += Math.max(1, width / EDGE_SAMPLES)) {
            countColor(img.getRGB(x, 0), colorCount);
        }

        // Sample bottom edge
        for (int x = 0; x < width; x += Math.max(1, width / EDGE_SAMPLES)) {
            countColor(img.getRGB(x, height - 1), colorCount);
        }

        // Sample left edge
        for (int y = 0; y < height; y += Math.max(1, height / EDGE_SAMPLES)) {
            countColor(img.getRGB(0, y), colorCount);
        }

        // Sample right edge
        for (int y = 0; y < height; y += Math.max(1, height / EDGE_SAMPLES)) {
            countColor(img.getRGB(width - 1, y), colorCount);
        }

        // Find most common color
        int maxCount = 0;
        int dominantColor = 0xFFFFFFFF;

        for (java.util.Map.Entry<Integer, Integer> entry : colorCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantColor = entry.getKey();
            }
        }

        return dominantColor;
    }

    /**
     * Increment color count in map
     */
    private void countColor(int rgb, java.util.Map<Integer, Integer> colorCount) {
        // Quantize color to reduce noise (group similar colors)
        int quantized = quantizeColor(rgb);
        colorCount.put(quantized, colorCount.getOrDefault(quantized, 0) + 1);
    }

    /**
     * Quantize color to nearest 32-step value
     */
    private int quantizeColor(int rgb) {
        int r = ((rgb >> 16) & 0xFF) / 32 * 32;
        int g = ((rgb >> 8) & 0xFF) / 32 * 32;
        int b = (rgb & 0xFF) / 32 * 32;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Check if two colors are similar within threshold
     */
    private boolean isSimilarColor(int color1, int color2, int threshold) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
        return diff < threshold;
    }
}