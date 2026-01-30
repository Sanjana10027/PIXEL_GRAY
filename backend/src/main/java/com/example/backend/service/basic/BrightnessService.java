package com.example.backend.service.basic;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class BrightnessService {

    private final GrayscaleService grayscaleService;

    // Dependency Injection
    public BrightnessService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, int level, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);

        // Use the GrayscaleService logic if toggle is active
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        // Apply Brightness
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                
                int r = ImageUtil.clamp(((rgb >> 16) & 255) + level);
                int g = ImageUtil.clamp(((rgb >> 8) & 255) + level);
                int b = ImageUtil.clamp((rgb & 255) + level);
                
                img.setRGB(x, y, (0xff << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return new ImageMatrixResponse(
            ImageUtil.encode(img), 
            LinearMatrixUtil.toLinear(img), 
            img.getWidth(), 
            img.getHeight()
        );
    }
}