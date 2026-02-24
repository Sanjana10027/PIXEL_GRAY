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

    /**
     * Standard API entry point for Horizontal Flip.
     */
    public ImageMatrixResponse horizontal(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        int[] matrix = LinearMatrixUtil.toLinear(img);
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        // Call the matrix processor to keep logic centralized
        int[] resultMatrix = processHorizontal(matrix, w, h);
        BufferedImage flippedImg = LinearMatrixUtil.fromLinear(resultMatrix, w, h);

        return new ImageMatrixResponse(
            ImageUtil.encode(flippedImg),
            resultMatrix,
            w,
            h
        );
    }

    /**
     * Standard API entry point for Vertical Flip.
     */
    public ImageMatrixResponse vertical(byte[] bytes, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        int w = img.getWidth();
        int h = img.getHeight();

        int[] matrix = LinearMatrixUtil.toLinear(img);
        if (grayscale) {
            matrix = grayscaleService.processMatrix(matrix);
        }

        int[] resultMatrix = processVertical(matrix, w, h);
        BufferedImage flippedImg = LinearMatrixUtil.fromLinear(resultMatrix, w, h);

        return new ImageMatrixResponse(
            ImageUtil.encode(flippedImg),
            resultMatrix,
            w,
            h
        );
    }

    // --- Matrix Pipeline Methods (Used by ImagePipelineService) ---

    

    /**
     * Performs a Horizontal Flip on a linear matrix using the Transformation Engine.
     */
    public int[] processHorizontal(int[] matrix, int w, int h) {
        double[][] flipMatrix = {
            {-1, 0}, 
            { 0, 1}
        };
        BufferedImage img = LinearMatrixUtil.fromLinear(matrix, w, h);
        BufferedImage flipped = engine.applyTransform(img, flipMatrix, w, h);
        return LinearMatrixUtil.toLinear(flipped);
    }

    /**
     * Performs a Vertical Flip on a linear matrix using the Transformation Engine.
     */
    public int[] processVertical(int[] matrix, int w, int h) {
        double[][] flipMatrix = {
            {1,  0}, 
            {0, -1}
        };
        BufferedImage img = LinearMatrixUtil.fromLinear(matrix, w, h);
        BufferedImage flipped = engine.applyTransform(img, flipMatrix, w, h);
        return LinearMatrixUtil.toLinear(flipped);
    }
}