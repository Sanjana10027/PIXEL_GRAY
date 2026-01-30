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
public class RotateService {

    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService; // Added

    public RotateService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, double angle, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        // Toggle Fix: Convert to grayscale BEFORE rotating
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        double rad = Math.toRadians(angle);
        
        // Rotation Matrix logic remains the same
        double[][] matrix = {
            { Math.cos(rad), -Math.sin(rad) },
            { Math.sin(rad),  Math.cos(rad) }
        };

        BufferedImage rotatedImg = engine.applyTransform(img, matrix);
        
        return new ImageMatrixResponse(
            ImageUtil.encode(rotatedImg),
            LinearMatrixUtil.toLinear(rotatedImg),
            rotatedImg.getWidth(),
            rotatedImg.getHeight()
        );
    }
}