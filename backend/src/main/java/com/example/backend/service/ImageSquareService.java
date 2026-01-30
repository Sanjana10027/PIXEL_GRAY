package com.example.backend.service;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.backend.service.util.ImageUtil;
 
@Service
public class ImageSquareService {
 
    public boolean isSquare(byte[] bytes) throws IOException {
        BufferedImage image = ImageUtil.decode(bytes);
        if (image == null) {
            throw new IOException("Could not decode image data.");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        return width == height;
    }
}
