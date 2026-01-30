// package com.example.backend.service.geometric;

// import com.example.backend.models.ImageMatrixResponse;
// import com.example.backend.service.core.MatrixTransformEngine;
// import com.example.backend.service.matrix.LinearMatrixUtil;
// import com.example.backend.service.util.ImageUtil;
// import com.example.backend.service.basic.GrayscaleService;
// import org.springframework.stereotype.Service;
// import java.awt.image.BufferedImage;
// import java.io.IOException;

// @Service
// public class ZoomService {
//     private final MatrixTransformEngine engine;
//     private final GrayscaleService grayscaleService;

//     public ZoomService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
//         this.engine = engine;
//         this.grayscaleService = grayscaleService;
//     }

//     public ImageMatrixResponse apply(byte[] bytes, double scale, boolean grayscale) throws IOException {
//         BufferedImage img = ImageUtil.decode(bytes);
        
//         if (grayscale) {
//             img = grayscaleService.process(img);
//         }

//         // Calculate NEW dimensions based on scale
//         int newW = (int) Math.max(1, img.getWidth() * scale);
//         int newH = (int) Math.max(1, img.getHeight() * scale);

//         // Scaling Matrix (Inverse mapping: 1/scale)
//         double[][] matrix = {
//             {1.0 / scale, 0}, 
//             {0, 1.0 / scale}
//         };
        
//         // Transform into the new larger/smaller buffer
//         BufferedImage transformed = engine.applyTransform(img, matrix, newW, newH);
        
//         return new ImageMatrixResponse(
//             ImageUtil.encode(transformed), 
//             LinearMatrixUtil.toLinear(transformed), 
//             transformed.getWidth(), 
//             transformed.getHeight()
//         );
//     }
// }


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
public class ZoomService {
    private final MatrixTransformEngine engine;
    private final GrayscaleService grayscaleService;

    public ZoomService(MatrixTransformEngine engine, GrayscaleService grayscaleService) {
        this.engine = engine;
        this.grayscaleService = grayscaleService;
    }

    public ImageMatrixResponse apply(byte[] bytes, double scale, boolean grayscale) throws IOException {
        BufferedImage img = ImageUtil.decode(bytes);
        
        if (grayscale) {
            img = grayscaleService.process(img);
        }

        // Calculate the target dimensions for the "Data Zoom"
        int newW = (int) Math.max(1, img.getWidth() * scale);
        int newH = (int) Math.max(1, img.getHeight() * scale);

        // Standard scaling matrix
        double[][] matrix = {
            {1.0 / scale, 0}, 
            {0, 1.0 / scale}
        };
        
        // We use the original engine logic but target a new sized buffer
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        
        // Manual coordinate mapping to ensure Nearest Neighbor (crunchy pixels)
        double cx = img.getWidth() / 2.0;
        double cy = img.getHeight() / 2.0;
        double nCx = newW / 2.0;
        double nCy = newH / 2.0;

        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                double dx = x - nCx;
                double dy = y - nCy;

                int srcX = (int) Math.floor(matrix[0][0] * dx + cx);
                int srcY = (int) Math.floor(matrix[1][1] * dy + cy);

                if (srcX >= 0 && srcX < img.getWidth() && srcY >= 0 && srcY < img.getHeight()) {
                    out.setRGB(x, y, img.getRGB(srcX, srcY));
                } else {
                    out.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }
        
        return new ImageMatrixResponse(
            ImageUtil.encode(out), 
            LinearMatrixUtil.toLinear(out), 
            newW, 
            newH
        );
    }
}