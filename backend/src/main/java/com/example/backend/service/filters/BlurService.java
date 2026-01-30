package com.example.backend.service.filters;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService; // Import GrayscaleService
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class BlurService {

    private final GrayscaleService grayscaleService;

    // Injecting GrayscaleService to reuse processing logic
    public BlurService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, int intensity, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);

        // If the toggle is active, convert the source image to grayscale first
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // Kernel size calculation based on intensity
        int size = Math.max(3, intensity * 2 + 1);
        int off = size / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sr = 0, sg = 0, sb = 0, count = 0;
                
                // Neighborhood convolution
                for (int ky = -off; ky <= off; ky++) {
                    for (int kx = -off; kx <= off; kx++) {
                        int px = x + kx, py = y + ky;
                        if (px >= 0 && px < w && py >= 0 && py < h) {
                            int rgb = img.getRGB(px, py);
                            sr += (rgb >> 16) & 255;
                            sg += (rgb >> 8) & 255;
                            sb += rgb & 255;
                            count++;
                        }
                    }
                }
                
                // Set the averaged (blurred) pixel
                out.setRGB(x, y, (0xff << 24) | ((sr / count) << 16) | ((sg / count) << 8) | (sb / count));
            }
        }
        
        return new ImageMatrixResponse(
            ImageUtil.encode(out), 
            LinearMatrixUtil.toLinear(out), 
            w, 
            h
        );
    }
}