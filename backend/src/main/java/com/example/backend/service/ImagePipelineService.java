package com.example.backend.service;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.basic.*;
import com.example.backend.service.filters.*;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ImagePipelineService {

    private final GrayscaleService grayscaleService;
    private final BrightnessService brightnessService;
    private final ContrastService contrastService;
    private final BlurService blurService;
    private final SharpenService sharpenService;

    public ImagePipelineService(
            GrayscaleService grayscaleService,
            BrightnessService brightnessService,
            ContrastService contrastService,
            BlurService blurService,
            SharpenService sharpenService) {
        this.grayscaleService = grayscaleService;
        this.brightnessService = brightnessService;
        this.contrastService = contrastService;
        this.blurService = blurService;
        this.sharpenService = sharpenService;
    }

    /**
     * Composite layers SEQUENTIALLY - bottom to top
     * Each layer modifies the accumulated result
     */
    public ImageMatrixResponse compositeLayers(byte[] baseImageBytes, String layersJson) throws Exception {
        BufferedImage baseImage = ImageUtil.decode(baseImageBytes);
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();

        // Parse layers
        List<LayerData> layers = parseLayersJson(layersJson);
        
        System.out.println("=== LAYER COMPOSITE DEBUG ===");
        System.out.println("Total layers received: " + layers.size());
        for (int i = 0; i < layers.size(); i++) {
            LayerData l = layers.get(i);
            System.out.println("Layer " + i + ": type=" + l.type + ", visible=" + l.visible + 
                             ", opacity=" + l.opacity + ", filterType=" + l.filterType);
        }

        // Start with a copy of base image
        BufferedImage currentResult = copyImage(baseImage);

        // Process layers from BOTTOM to TOP (array order)
        for (int i = 0; i < layers.size(); i++) {
            LayerData layer = layers.get(i);
            
            if (!layer.visible) {
                System.out.println("Skipping layer " + i + " (invisible)");
                continue;
            }

            System.out.println("Processing layer " + i + " (type: " + layer.type + ")");
            
            // Apply this layer to the current accumulated result
            currentResult = applyLayer(currentResult, layer, baseImageBytes);
        }

        System.out.println("=== COMPOSITE COMPLETE ===");

        return new ImageMatrixResponse(
            ImageUtil.encode(currentResult),
            LinearMatrixUtil.toLinear(currentResult),
            width,
            height
        );
    }

    /**
     * Apply a single layer to the current accumulated result
     */
    private BufferedImage applyLayer(BufferedImage currentResult, LayerData layer, byte[] originalBase) throws Exception {
        int width = currentResult.getWidth();
        int height = currentResult.getHeight();

        switch (layer.type) {
            case "color":
                return blendColorLayer(currentResult, layer.color, layer.opacity);
            
            case "gradient":
                return blendGradientLayer(currentResult, layer.gradientStart, layer.gradientEnd, 
                                         layer.gradientAngle, layer.opacity);
            
            case "image":
                return blendImageLayer(currentResult, layer.imageData, layer.opacity);
            
            case "filter":
                return applyFilterToCurrentResult(currentResult, layer);
            
            default:
                System.out.println("Unknown layer type: " + layer.type);
                return currentResult;
        }
    }

    /**
     * Blend color layer on top of current result
     */
    private BufferedImage blendColorLayer(BufferedImage base, String colorHex, float opacity) {
        int width = base.getWidth();
        int height = base.getHeight();
        
        BufferedImage colorLayer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = colorLayer.createGraphics();
        g.setColor(hexToColor(colorHex));
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        return blendImages(base, colorLayer, opacity);
    }

    /**
     * Blend gradient layer on top of current result
     * FIX: Proper gradient calculation
     */
    private BufferedImage blendGradientLayer(BufferedImage base, String startHex, String endHex, 
                                            int angle, float opacity) {
        int width = base.getWidth();
        int height = base.getHeight();
        
        BufferedImage gradientLayer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gradientLayer.createGraphics();
        
        // Enable anti-aliasing for smoother gradients
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Convert angle to radians
        double radians = Math.toRadians(angle);
        
        // Calculate gradient vector based on angle
        // We want the gradient to span the entire canvas
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        
        // Calculate maximum distance from center to ensure gradient covers entire image
        double maxDist = Math.sqrt(width * width + height * height) / 2.0;
        
        // Calculate start and end points
        int x1 = (int)(centerX - maxDist * Math.cos(radians));
        int y1 = (int)(centerY - maxDist * Math.sin(radians));
        int x2 = (int)(centerX + maxDist * Math.cos(radians));
        int y2 = (int)(centerY + maxDist * Math.sin(radians));
        
        Color startColor = hexToColor(startHex);
        Color endColor = hexToColor(endHex);
        
        System.out.println("Gradient: " + startHex + " -> " + endHex + " at " + angle + "°");
        System.out.println("Points: (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")");
        
        GradientPaint gradient = new GradientPaint(x1, y1, startColor, x2, y2, endColor);
        g.setPaint(gradient);
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        return blendImages(base, gradientLayer, opacity);
    }

    /**
     * Blend image layer on top of current result
     */
    private BufferedImage blendImageLayer(BufferedImage base, String dataUrl, float opacity) throws Exception {
        int width = base.getWidth();
        int height = base.getHeight();
        
        String base64Data = dataUrl.substring(dataUrl.indexOf(",") + 1);
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        BufferedImage original = ImageUtil.decode(imageBytes);

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        
        return blendImages(base, resized, opacity);
    }

    /**
     * Apply filter to CURRENT RESULT (not original base)
     * This is the key fix for sequential filter application
     */
    private BufferedImage applyFilterToCurrentResult(BufferedImage currentResult, LayerData layer) throws Exception {
        // Convert current result to bytes
        byte[] currentBytes = ImageUtil.encode(currentResult);
        
        BufferedImage filteredResult = null;
        
        switch (layer.filterType) {
            case "brightness":
                int level = layer.getIntParam("level", 0);
                System.out.println("Applying brightness: " + level);
                filteredResult = ImageUtil.decode(brightnessService.apply(currentBytes, level, false).image);
                break;
            
            case "contrast":
                level = layer.getIntParam("level", 0);
                System.out.println("Applying contrast: " + level);
                filteredResult = ImageUtil.decode(contrastService.apply(currentBytes, level, false).image);
                break;
            
            case "blur":
                int intensity = layer.getIntParam("intensity", 0);
                System.out.println("Applying blur: " + intensity);
                filteredResult = ImageUtil.decode(blurService.apply(currentBytes, intensity, false).image);
                break;
            
            case "sharpen":
                intensity = layer.getIntParam("intensity", 0);
                System.out.println("Applying sharpen: " + intensity);
                filteredResult = ImageUtil.decode(sharpenService.apply(currentBytes, intensity, false).image);
                break;
            
            case "grayscale":
                System.out.println("Applying grayscale");
                filteredResult = grayscaleService.process(currentResult);
                break;
            
            default:
                System.out.println("Unknown filter type: " + layer.filterType);
                return currentResult;
        }
        
        if (filteredResult == null) {
            return currentResult;
        }
        
        // Blend filtered result with current using opacity
        return blendImages(currentResult, filteredResult, layer.opacity);
    }

    /**
     * Blend two images with opacity
     * Uses MULTIPLY blend mode for filters to preserve base image
     */
    private BufferedImage blendImages(BufferedImage base, BufferedImage overlay, float opacity) {
        int width = base.getWidth();
        int height = base.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baseRGB = base.getRGB(x, y);
                int overlayRGB = overlay.getRGB(x, y);
                
                // Extract RGBA components from base
                int baseA = (baseRGB >> 24) & 0xFF;
                int baseR = (baseRGB >> 16) & 0xFF;
                int baseG = (baseRGB >> 8) & 0xFF;
                int baseB = baseRGB & 0xFF;
                
                // Extract RGBA components from overlay
                int overlayA = (overlayRGB >> 24) & 0xFF;
                int overlayR = (overlayRGB >> 16) & 0xFF;
                int overlayG = (overlayRGB >> 8) & 0xFF;
                int overlayB = overlayRGB & 0xFF;
                
                // Apply opacity to overlay alpha
                overlayA = (int)(overlayA * opacity);
                
                // Alpha blending formula
                float alpha = overlayA / 255.0f;
                int resultR = (int)(overlayR * alpha + baseR * (1 - alpha));
                int resultG = (int)(overlayG * alpha + baseG * (1 - alpha));
                int resultB = (int)(overlayB * alpha + baseB * (1 - alpha));
                int resultA = Math.max(baseA, overlayA);
                
                int resultRGB = (resultA << 24) | (resultR << 16) | (resultG << 8) | resultB;
                result.setRGB(x, y, resultRGB);
            }
        }
        
        return result;
    }

    /**
     * Create a copy of an image
     */
    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), 
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    /**
     * Convert hex color to Java Color
     */
    private Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new Color(r, g, b);
    }

    /**
     * Enhanced JSON parser with better debugging
     */
    private List<LayerData> parseLayersJson(String json) {
        List<LayerData> layers = new ArrayList<>();
        
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
        json = json.trim();
        
        if (json.isEmpty()) return layers;

        List<String> objects = extractJsonObjects(json);
        
        for (String obj : objects) {
            LayerData layer = parseLayerObject(obj);
            if (layer != null) {
                layers.add(layer);
            }
        }
        
        return layers;
    }

    private List<String> extractJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (braceCount == 0) {
                    start = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && start != -1) {
                    objects.add(json.substring(start + 1, i));
                    start = -1;
                }
            }
        }
        
        return objects;
    }

    private LayerData parseLayerObject(String obj) {
        LayerData layer = new LayerData();
        
        List<String> pairs = splitByComma(obj);
        
        for (String pair : pairs) {
            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) continue;
            
            String key = pair.substring(0, colonIndex).trim().replace("\"", "");
            String value = pair.substring(colonIndex + 1).trim();
            
            parseLayerField(layer, key, value);
        }
        
        return layer;
    }

    private List<String> splitByComma(String str) {
        List<String> parts = new ArrayList<>();
        int braceCount = 0;
        boolean inQuotes = false;
        int start = 0;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == ',' && braceCount == 0) {
                    parts.add(str.substring(start, i));
                    start = i + 1;
                }
            }
        }
        
        if (start < str.length()) {
            parts.add(str.substring(start));
        }
        
        return parts;
    }

    private void parseLayerField(LayerData layer, String key, String value) {
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        
        switch (key) {
            case "type":
                layer.type = value;
                break;
            case "visible":
                layer.visible = Boolean.parseBoolean(value);
                break;
            case "opacity":
                layer.opacity = Float.parseFloat(value);
                break;
            case "color":
                layer.color = value;
                break;
            case "gradientStart":
                layer.gradientStart = value;
                break;
            case "gradientEnd":
                layer.gradientEnd = value;
                break;
            case "gradientAngle":
                try {
                    layer.gradientAngle = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    layer.gradientAngle = 90;
                }
                break;
            case "imageData":
                layer.imageData = value;
                break;
            case "filterType":
                layer.filterType = value;
                break;
            case "params":
                if (value.startsWith("{") && value.endsWith("}")) {
                    parseParams(layer, value);
                }
                break;
        }
    }

    private void parseParams(LayerData layer, String paramsJson) {
        paramsJson = paramsJson.substring(1, paramsJson.length() - 1);
        
        List<String> pairs = splitByComma(paramsJson);
        
        for (String pair : pairs) {
            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) continue;
            
            String key = pair.substring(0, colonIndex).trim().replace("\"", "");
            String value = pair.substring(colonIndex + 1).trim().replace("\"", "");
            
            layer.params.put(key, value);
        }
    }

    private static class LayerData {
        String type;
        boolean visible = true;
        float opacity = 1.0f;
        String color;
        String gradientStart;
        String gradientEnd;
        int gradientAngle = 90;
        String imageData;
        String filterType;
        java.util.Map<String, String> params = new java.util.HashMap<>();

        int getIntParam(String key, int defaultValue) {
            String value = params.get(key);
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
}