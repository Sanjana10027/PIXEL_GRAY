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
public class ZoomService {
    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService; // Added

    public ZoomService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, double scale, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        // Toggle Fix: Convert to grayscale BEFORE zooming
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        // Scaling Matrix: The inverse (1/scale) is used for backward mapping in the engine
        double[][] matrix = {{1.0 / scale, 0}, {0, 1.0 / scale}};
        
        
        
        BufferedImage transformed = engine.applyTransform(img, matrix);
        
        return new ImageMatrixResponse(
            ImageUtil.encode(transformed), 
            LinearMatrixUtil.toLinear(transformed), 
            transformed.getWidth(), 
            transformed.getHeight()
        );
    }
}