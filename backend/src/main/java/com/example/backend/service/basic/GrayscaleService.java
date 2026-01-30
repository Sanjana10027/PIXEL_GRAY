package com.example.backend.service.basic;

import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;
import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;

@Service
public class GrayscaleService {

    // Main entry point for the /grayscale endpoint
    public ImageMatrixResponse apply(byte[] bytes) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        BufferedImage grayImg = process(img);

        return new ImageMatrixResponse(
                ImageUtil.encode(grayImg),
                LinearMatrixUtil.toLinear(grayImg),
                grayImg.getWidth(),
                grayImg.getHeight()
        );
    }

    // Reusable logic for other services
    public BufferedImage process(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;
                
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                result.setRGB(x, y, (0xff << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
        return result;
    }
}