package com.example.backend.service.geometric;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.core.MatrixTransformEngine;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.basic.GrayscaleService; // Added
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class FlipService {

    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService; // Added

    public FlipService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse horizontal(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        // Apply grayscale if toggle is active
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        // Horizontal flip matrix: [[-1, 0], [0, 1]]
        BufferedImage flipped = engine.applyTransform(img, new double[][]{{-1, 0}, {0, 1}});
        
        return new ImageMatrixResponse(
            ImageUtil.encode(flipped),
            LinearMatrixUtil.toLinear(flipped),
            flipped.getWidth(),
            flipped.getHeight()
        );
    }

    public ImageMatrixResponse vertical(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        // Apply grayscale if toggle is active
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        // Vertical flip matrix: [[1, 0], [0, -1]]
        BufferedImage flipped = engine.applyTransform(img, new double[][]{{1, 0}, {0, -1}});
        
        return new ImageMatrixResponse(
            ImageUtil.encode(flipped),
            LinearMatrixUtil.toLinear(flipped),
            flipped.getWidth(),
            flipped.getHeight()
        );
    }
}