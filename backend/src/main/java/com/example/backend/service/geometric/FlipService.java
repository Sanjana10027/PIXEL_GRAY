
package com.example.backend.service.geometric;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.core.MatrixTransformEngine;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class FlipService {

    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService;

    public FlipService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse horizontal(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        int w = img.getWidth();
        int h = img.getHeight();

        // Horizontal flip matrix: [[-1, 0], [0, 1]]
        // We pass original w and h as destWidth and destHeight
        BufferedImage flipped = engine.applyTransform(img, new double[][]{{-1, 0}, {0, 1}}, w, h);
        
        return new ImageMatrixResponse(
            ImageUtil.encode(flipped),
            LinearMatrixUtil.toLinear(flipped),
            flipped.getWidth(),
            flipped.getHeight()
        );
    }

    public ImageMatrixResponse vertical(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        int w = img.getWidth();
        int h = img.getHeight();

        // Vertical flip matrix: [[1, 0], [0, -1]]
        BufferedImage flipped = engine.applyTransform(img, new double[][]{{1, 0}, {0, -1}}, w, h);
        
        return new ImageMatrixResponse(
            ImageUtil.encode(flipped),
            LinearMatrixUtil.toLinear(flipped),
            flipped.getWidth(),
            flipped.getHeight()
        );
    }
}