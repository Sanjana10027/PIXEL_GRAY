package com.example.backend.service.filters;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService; // Import GrayscaleService
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class SharpenService {

    private final GrayscaleService grayscaleService;

    // Inject GrayscaleService to reuse processing logic
    public SharpenService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, int intensity, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);

        // Toggle Fix: Convert to grayscale BEFORE applying the sharpening kernel
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // Sharpening Kernel: The center value increases with intensity
        int[][] k = {
            {0, -1, 0}, 
            {-1, 5 + intensity, -1}, 
            {0, -1, 0}
        };

        // Standard 3x3 Convolution
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int r = 0, g = 0, b = 0;
                
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = img.getRGB(x + kx, y + ky);
                        int wgt = k[ky + 1][kx + 1];
                        
                        r += ((rgb >> 16) & 255) * wgt;
                        g += ((rgb >> 8) & 255) * wgt;
                        b += (rgb & 255) * wgt;
                    }
                }
                
                // Set the sharpened pixel with clamping to prevent color overflow
                out.setRGB(x, y, (0xff << 24) | 
                    (ImageUtil.clamp(r) << 16) | 
                    (ImageUtil.clamp(g) << 8) | 
                    ImageUtil.clamp(b));
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