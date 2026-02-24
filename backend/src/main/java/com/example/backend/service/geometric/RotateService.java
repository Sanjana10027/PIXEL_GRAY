
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
public class RotateService {

    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService;

    public RotateService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    /**
     * Applies rotation and returns the updated Pixel Matrix.
     * The matrix returned reflects the new dimensions (newW, newH).
     */
    public ImageMatrixResponse apply(byte[] bytes, double angle, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);

        // 1. Maintain functionality: Pre-process grayscale if toggled
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        double rad = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));

        int w = img.getWidth();
        int h = img.getHeight();

        // 2. Calculate new bounding box dimensions to prevent clipping
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        

        // 3. Rotation Matrix for the Engine
        // This maps the output coordinate back to the input coordinate (Inverse Mapping)
        double[][] matrix = {
            { Math.cos(rad), Math.sin(rad) },
            { -Math.sin(rad), Math.cos(rad) }
        };

        // 4. Perform transformation
        BufferedImage rotatedImg = engine.applyTransform(img, matrix, newW, newH);

        // 5. Extract the Linear Matrix of the RESULTING image
        // This ensures the 5x5 grid in the frontend maps to the rotated pixels
        int[] resultMatrix = LinearMatrixUtil.toLinear(rotatedImg);

        return new ImageMatrixResponse(
            ImageUtil.encode(rotatedImg),
            resultMatrix,
            rotatedImg.getWidth(),
            rotatedImg.getHeight()
        );
    }
}