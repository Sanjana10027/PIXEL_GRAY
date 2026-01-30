package com.example.backend.service;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.util.ImageUtil;
import com.example.backend.service.matrix.LinearMatrixUtil;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

@Service
public class LayerCompositeService {

    public ImageMatrixResponse composeLayers(byte[] originalBytes, String layersJson) throws Exception {
        BufferedImage baseImage = ImageUtil.decode(originalBytes);
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();
        
        // Start with the original image as base
        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = composite.createGraphics();
        g.drawImage(baseImage, 0, 0, null);
        
        // Parse layers manually
        List<Layer> layers = parseLayersJson(layersJson);
        
        // Apply each visible layer from bottom to top (array order)
        for (Layer layer : layers) {
            if (!layer.visible) {
                continue;
            }
            
            // Set composite with opacity
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.opacity);
            g.setComposite(alphaComposite);
            
            if ("color".equals(layer.type)) {
                // Solid color layer
                Color color = parseColor(layer.color);
                g.setColor(color);
                g.fillRect(0, 0, width, height);
                
            } else if ("gradient".equals(layer.type)) {
                // Gradient layer
                Color startColor = parseColor(layer.gradientStart);
                Color endColor = parseColor(layer.gradientEnd);
                int angle = layer.gradientAngle;
                
                // Calculate gradient coordinates based on angle
                double radians = Math.toRadians(angle);
                int x1 = (int) (width / 2 - Math.cos(radians) * width);
                int y1 = (int) (height / 2 - Math.sin(radians) * height);
                int x2 = (int) (width / 2 + Math.cos(radians) * width);
                int y2 = (int) (height / 2 + Math.sin(radians) * height);
                
                GradientPaint gradient = new GradientPaint(x1, y1, startColor, x2, y2, endColor);
                g.setPaint(gradient);
                g.fillRect(0, 0, width, height);
                
            } else if ("filter".equals(layer.type)) {
                // Filter layer - decode the base64 image data
                String imageData = layer.imageData;
                if (imageData != null && imageData.startsWith("data:image/png;base64,")) {
                    imageData = imageData.substring("data:image/png;base64,".length());
                }
                
                if (imageData != null && !imageData.isEmpty()) {
                    byte[] filterImageBytes = Base64.getDecoder().decode(imageData);
                    BufferedImage filterImage = ImageUtil.decode(filterImageBytes);
                    
                    // Overlay the filter image
                    g.drawImage(filterImage, 0, 0, width, height, null);
                }
            } else if ("image".equals(layer.type)) {
                // Image layer for mixing - decode the base64 image data
                String imageData = layer.imageData;
                if (imageData != null && imageData.startsWith("data:image/")) {
                    int base64Start = imageData.indexOf("base64,");
                    if (base64Start != -1) {
                        imageData = imageData.substring(base64Start + 7);
                    }
                }
                
                if (imageData != null && !imageData.isEmpty()) {
                    byte[] mixImageBytes = Base64.getDecoder().decode(imageData);
                    BufferedImage mixImage = ImageUtil.decode(mixImageBytes);
                    
                    // Scale the mix image to match the base image dimensions
                    g.drawImage(mixImage, 0, 0, width, height, null);
                }
            }
            
            // Reset composite to full opacity for next layer
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        g.dispose();
        
        return new ImageMatrixResponse(
            ImageUtil.encode(composite),
            LinearMatrixUtil.toLinear(composite),
            width,
            height
        );
    }
    
    // Manual JSON parser for layers array
    private List<Layer> parseLayersJson(String json) {
        List<Layer> layers = new ArrayList<>();
        
        // Remove outer brackets and whitespace
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }
        
        // Split by objects (looking for },{ pattern)
        List<String> objectStrings = splitJsonObjects(json);
        
        for (String objStr : objectStrings) {
            Layer layer = parseLayerObject(objStr);
            if (layer != null) {
                layers.add(layer);
            }
        }
        
        return layers;
    }
    
    // Split JSON array into individual object strings
    private List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int startIndex = 0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (braceCount == 0) {
                    startIndex = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(json.substring(startIndex, i + 1));
                }
            }
        }
        
        return objects;
    }
    
    // Parse a single layer object
    private Layer parseLayerObject(String objStr) {
        Layer layer = new Layer();
        
        // Remove outer braces
        objStr = objStr.trim();
        if (objStr.startsWith("{")) {
            objStr = objStr.substring(1);
        }
        if (objStr.endsWith("}")) {
            objStr = objStr.substring(0, objStr.length() - 1);
        }
        
        // Parse key-value pairs
        List<KeyValue> pairs = parseKeyValuePairs(objStr);
        
        for (KeyValue kv : pairs) {
            switch (kv.key) {
                case "id":
                    layer.id = parseInteger(kv.value);
                    break;
                case "type":
                    layer.type = removeQuotes(kv.value);
                    break;
                case "visible":
                    layer.visible = parseBoolean(kv.value);
                    break;
                case "opacity":
                    layer.opacity = parseFloat(kv.value);
                    break;
                case "color":
                    layer.color = removeQuotes(kv.value);
                    break;
                case "gradientStart":
                    layer.gradientStart = removeQuotes(kv.value);
                    break;
                case "gradientEnd":
                    layer.gradientEnd = removeQuotes(kv.value);
                    break;
                case "gradientAngle":
                    layer.gradientAngle = parseInteger(kv.value);
                    break;
                case "imageData":
                    layer.imageData = removeQuotes(kv.value);
                    break;
                case "filterType":
                    layer.filterType = removeQuotes(kv.value);
                    break;
                case "name":
                    layer.name = removeQuotes(kv.value);
                    break;
                case "fileName":
                    layer.fileName = removeQuotes(kv.value);
                    break;
            }
        }
        
        return layer;
    }
    
    // Parse key-value pairs from object string
    private List<KeyValue> parseKeyValuePairs(String objStr) {
        List<KeyValue> pairs = new ArrayList<>();
        
        int i = 0;
        while (i < objStr.length()) {
            // Skip whitespace and commas
            while (i < objStr.length() && (objStr.charAt(i) == ' ' || objStr.charAt(i) == ',' || objStr.charAt(i) == '\n' || objStr.charAt(i) == '\r' || objStr.charAt(i) == '\t')) {
                i++;
            }
            
            if (i >= objStr.length()) break;
            
            // Find key (between quotes)
            if (objStr.charAt(i) == '"') {
                i++; // Skip opening quote
                int keyStart = i;
                while (i < objStr.length() && objStr.charAt(i) != '"') {
                    i++;
                }
                String key = objStr.substring(keyStart, i);
                i++; // Skip closing quote
                
                // Skip whitespace and colon
                while (i < objStr.length() && (objStr.charAt(i) == ' ' || objStr.charAt(i) == ':')) {
                    i++;
                }
                
                // Find value
                String value = extractValue(objStr, i);
                i += value.length();
                
                // Clean up value (might have trailing comma)
                if (value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1);
                }
                value = value.trim();
                
                pairs.add(new KeyValue(key, value));
            } else {
                i++;
            }
        }
        
        return pairs;
    }
    
    // Extract value from current position
    private String extractValue(String str, int startIndex) {
        StringBuilder value = new StringBuilder();
        int i = startIndex;
        
        // Skip leading whitespace
        while (i < str.length() && str.charAt(i) == ' ') {
            i++;
        }
        
        if (i >= str.length()) return "";
        
        // Check if value is quoted string
        if (str.charAt(i) == '"') {
            i++; // Skip opening quote
            value.append('"');
            while (i < str.length()) {
                char c = str.charAt(i);
                value.append(c);
                if (c == '"' && (i == startIndex + 1 || str.charAt(i - 1) != '\\')) {
                    i++;
                    break;
                }
                i++;
            }
        } else if (str.charAt(i) == '{') {
            // Nested object
            int braceCount = 0;
            while (i < str.length()) {
                char c = str.charAt(i);
                value.append(c);
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                i++;
                if (braceCount == 0) break;
            }
        } else {
            // Simple value (number, boolean, etc.)
            while (i < str.length()) {
                char c = str.charAt(i);
                if (c == ',' || c == '}') {
                    break;
                }
                value.append(c);
                i++;
            }
        }
        
        return value.toString();
    }
    
    // Helper to remove quotes from string
    private String removeQuotes(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    // Parse integer from string
    private int parseInteger(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // Parse float from string
    private float parseFloat(String str) {
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
    
    // Parse boolean from string
    private boolean parseBoolean(String str) {
        return "true".equalsIgnoreCase(str.trim());
    }
    
    // Parse hex color to Color object
    private Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.BLACK;
        }
        
        // Remove # if present
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
    
    // Inner class to represent a layer
    private static class Layer {
        int id;
        String type;
        boolean visible;
        float opacity;
        String color;
        String gradientStart;
        String gradientEnd;
        int gradientAngle;
        String imageData;
        String filterType;
        String name;
        String fileName;
        
        Layer() {
            this.visible = true;
            this.opacity = 1.0f;
            this.gradientAngle = 0;
        }
    }
    
    // Inner class for key-value pairs
    private static class KeyValue {
        String key;
        String value;
        
        KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}