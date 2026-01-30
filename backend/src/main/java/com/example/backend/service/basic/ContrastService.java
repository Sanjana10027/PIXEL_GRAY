package com.example.backend.service.basic;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class ContrastService {

    private final GrayscaleService grayscaleService;

    // Inject GrayscaleService to reuse its processing logic
    public ContrastService(GrayscaleService grayscaleService) {
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, int level, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);

        // If the toggle is active, convert the image to grayscale first
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        double factor = (259.0 * (level + 255)) / (255.0 * (259 - level));

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                
                // Extract channels and apply contrast factor
                int r = ImageUtil.clamp((int)(factor * (((rgb >> 16) & 255) - 128) + 128));
                int g = ImageUtil.clamp((int)(factor * (((rgb >> 8) & 255) - 128) + 128));
                int b = ImageUtil.clamp((int)(factor * ((rgb & 255) - 128) + 128));
                
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