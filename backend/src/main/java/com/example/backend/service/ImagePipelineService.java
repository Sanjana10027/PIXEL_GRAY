// package com.example.backend.service;

// import com.example.backend.models.ImageMatrixResponse;
// import com.example.backend.service.basic.*;
// import com.example.backend.service.filters.*;
// import com.example.backend.service.matrix.LinearMatrixUtil;
// import com.example.backend.service.util.ImageUtil;
// import org.springframework.stereotype.Service;

// import java.awt.*;
// import java.awt.image.BufferedImage;
// import java.io.IOException;
// import java.util.*;
// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Service
// public class ImagePipelineService {

//     private final GrayscaleService grayscaleService;
//     private final BrightnessService brightnessService;
//     private final ContrastService contrastService;
//     private final BlurService blurService;
//     private final SharpenService sharpenService;
//     private final BackgroundRemovalService backgroundRemovalService;

//     public ImagePipelineService(
//             GrayscaleService grayscaleService,
//             BrightnessService brightnessService,
//             ContrastService contrastService,
//             BlurService blurService,
//             SharpenService sharpenService,
//             BackgroundRemovalService backgroundRemovalService) {

//         this.grayscaleService = grayscaleService;
//         this.brightnessService = brightnessService;
//         this.contrastService = contrastService;
//         this.blurService = blurService;
//         this.sharpenService = sharpenService;
//         this.backgroundRemovalService = backgroundRemovalService;
//     }

//     public ImageMatrixResponse compositeLayers(byte[] baseImageBytes, String layersJson) throws Exception {

//         BufferedImage baseImage = ImageUtil.decode(baseImageBytes);
//         if (baseImage == null) throw new IOException("Base image is null");

//         int width = baseImage.getWidth();
//         int height = baseImage.getHeight();

//         int[] canvasMatrix = LinearMatrixUtil.toLinear(baseImage);
//         List<LayerData> layers = parseLayersJson(layersJson);

//         for (LayerData layer : layers) {
//             // Fix: Respect visibility toggle correctly
//             if (!layer.visible) continue;
//             canvasMatrix = applyLayerToMatrix(canvasMatrix, layer, width, height);
//         }

//         BufferedImage finalImage = LinearMatrixUtil.fromLinear(canvasMatrix, width, height);

//         return new ImageMatrixResponse(
//                 ImageUtil.encode(finalImage),
//                 canvasMatrix,
//                 width,
//                 height
//         );
//     }

//     private int[] applyLayerToMatrix(int[] canvas, LayerData layer, int w, int h) throws Exception {

//         switch (layer.type.toLowerCase()) {

//             case "color": {
//                 int[] colorOverlay = generateColorMatrix(w, h, layer.color);
//                 return blendMatrices(canvas, colorOverlay, layer.opacity);
//             }

//             case "gradient": {
//                 int[] gradientOverlay = generateGradientMatrix(w, h, layer);
//                 return blendMatrices(canvas, gradientOverlay, layer.opacity);
//             }

//             case "image": {
//                 int[] imageOverlay = decodeImageToMatrix(layer.imageData, w, h);

//                 // Apply filters to the image layer itself
//                 imageOverlay = applyFilterToImageLayer(imageOverlay, layer, w, h);

//                 return blendMatrices(canvas, imageOverlay, layer.opacity);
//             }

//             case "filter": {
//                 int[] filteredCanvas = applyFilterMatrix(canvas, layer, w, h);
//                 return blendMatrices(canvas, filteredCanvas, layer.opacity);
//             }

//             default:
//                 return canvas;
//         }
//     }

//     // =========================
//     // IMAGE LAYER FILTER LOGIC
//     // =========================
//     private int[] applyFilterToImageLayer(int[] image, LayerData layer, int w, int h) throws Exception {

//         if (layer.filterType == null) return image;

//         switch (layer.filterType.toLowerCase()) {

//             case "brightness":
//                 return brightnessService.processMatrix(
//                         image,
//                         layer.getIntParam("level", 0)
//                 );

//             case "contrast":
//                 return contrastService.processMatrix(
//                         image,
//                         layer.getIntParam("level", 0)
//                 );

//             case "blur":
//                 return blurService.processMatrix(
//                         image,
//                         w,
//                         h,
//                         layer.getIntParam("intensity", 0)
//                 );

//             case "sharpen":
//                 return sharpenService.processMatrix(
//                         image,
//                         w,
//                         h,
//                         layer.getIntParam("intensity", 0)
//                 );

//             case "grayscale":
//                 return grayscaleService.processMatrix(image);

//             default:
//                 return image;
//         }
//     }

//     // =========================
//     // ADJUSTMENT FILTER LOGIC
//     // =========================
//     private int[] applyFilterMatrix(int[] canvas, LayerData layer, int w, int h) throws Exception {

//         if (layer.filterType == null) return canvas;

//         switch (layer.filterType.toLowerCase()) {

//             case "brightness":
//                 return brightnessService.processMatrix(
//                         canvas,
//                         layer.getIntParam("level", 0)
//                 );

//             case "contrast":
//                 return contrastService.processMatrix(
//                         canvas,
//                         layer.getIntParam("level", 0)
//                 );

//             case "blur":
//                 return blurService.processMatrix(
//                         canvas,
//                         w,
//                         h,
//                         layer.getIntParam("intensity", 0)
//                 );

//             case "sharpen":
//                 return sharpenService.processMatrix(
//                         canvas,
//                         w,
//                         h,
//                         layer.getIntParam("intensity", 0)
//                 );

//             case "grayscale":
//                 return grayscaleService.processMatrix(canvas);

//             case "background-removal":
//             case "remove-bg":
//                 return backgroundRemovalService.processManualMatrix(
//                         canvas,
//                         w,
//                         h,
//                         layer.getIntParam("sensitivity", 30)
//                 );

//             default:
//                 return canvas;
//         }
//     }

//     // =========================
//     // BLENDING
//     // =========================
//     private int[] blendMatrices(int[] base, int[] overlay, float opacity) {

//         int[] result = new int[base.length];

//         for (int i = 0; i < base.length; i++) {

//             int b = base[i];
//             int o = overlay[i];

//             float aB = ((b >> 24) & 255) / 255f;
//             float aO = (((o >> 24) & 255) / 255f) * opacity;

//             float aOut = aO + aB * (1 - aO);

//             if (aOut == 0) {
//                 result[i] = 0;
//                 continue;
//             }

//             int r = (int) (
//                     (((o >> 16) & 255) * aO +
//                      ((b >> 16) & 255) * aB * (1 - aO)) / aOut
//             );

//             int g = (int) (
//                     (((o >> 8) & 255) * aO +
//                      ((b >> 8) & 255) * aB * (1 - aO)) / aOut
//             );

//             int bl = (int) (
//                     ((o & 255) * aO +
//                      (b & 255) * aB * (1 - aO)) / aOut
//             );

//             result[i] =
//                     (clamp((int)(aOut * 255)) << 24) |
//                     (clamp(r) << 16) |
//                     (clamp(g) << 8) |
//                     clamp(bl);
//         }

//         return result;
//     }

//     private int clamp(int v) {
//         return v < 0 ? 0 : Math.min(v, 255);
//     }

//     // =========================
//     // GENERATORS
//     // =========================
//     private int[] generateColorMatrix(int w, int h, String hex) {

//         if (hex == null || hex.isEmpty()) hex = "#FFFFFF";
//         int color = (255 << 24) | Integer.parseInt(hex.replace("#", ""), 16);

//         int[] m = new int[w * h];
//         Arrays.fill(m, color);
//         return m;
//     }

//     private int[] generateGradientMatrix(int w, int h, LayerData layer) {

//         BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//         Graphics2D g = img.createGraphics();

//         Color c1 = Color.decode(layer.params.getOrDefault("gradientStart", "#FFFFFF"));
//         Color c2 = Color.decode(layer.params.getOrDefault("gradientEnd", "#000000"));
//         int angle = layer.getIntParam("gradientAngle", 0);

//         double rad = Math.toRadians(angle);
//         float cx = w / 2f;
//         float cy = h / 2f;
//         float d = (float)Math.sqrt(cx * cx + cy * cy);

//         float x1 = cx - (float)Math.cos(rad) * d;
//         float y1 = cy + (float)Math.sin(rad) * d;
//         float x2 = cx + (float)Math.cos(rad) * d;
//         float y2 = cy - (float)Math.sin(rad) * d;

//         g.setPaint(new GradientPaint(x1, y1, c1, x2, y2, c2));
//         g.fillRect(0, 0, w, h);
//         g.dispose();

//         return LinearMatrixUtil.toLinear(img);
//     }

//     private int[] decodeImageToMatrix(String dataUrl, int w, int h) throws Exception {

//         String b64 = dataUrl.contains(",")
//                 ? dataUrl.substring(dataUrl.indexOf(",") + 1)
//                 : dataUrl;

//         BufferedImage img = ImageUtil.decode(
//                 Base64.getDecoder().decode(b64.replaceAll("\\s", ""))
//         );

//         BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//         Graphics2D g = out.createGraphics();
//         g.drawImage(img, 0, 0, w, h, null);
//         g.dispose();

//         return LinearMatrixUtil.toLinear(out);
//     }

//     // =========================
//     // GENERIC JSON PARSER (FIXED)
//     // =========================
//     private List<LayerData> parseLayersJson(String json) {
//         List<LayerData> list = new ArrayList<>();
//         // Pattern to find objects: { ... }
//         Pattern p = Pattern.compile("\\{[^{}]*(\\{[^{}]*\\})*[^{}]*\\}");
//         Matcher m = p.matcher(json);

//         while (m.find()) {
//             list.add(parseObject(m.group()));
//         }
//         return list;
//     }

//     private LayerData parseObject(String obj) {
//         LayerData l = new LayerData();
        
//         // Regex to find "key": value pairs
//         // Supports: "key": "string", "key": number, "key": boolean
//         Pattern p = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}]*))");
//         Matcher m = p.matcher(obj);

//         while (m.find()) {
//             String key = m.group(1);
//             // Group 2 is for string values (inside quotes)
//             // Group 3 is for raw values (numbers, booleans)
//             String val = (m.group(2) != null) ? m.group(2) : m.group(3).trim();

//             if (val == null) continue;

//             switch (key) {
//                 // Known system fields
//                 case "type": l.type = val; break;
//                 case "visible": l.visible = Boolean.parseBoolean(val); break;
//                 case "opacity": l.opacity = Float.parseFloat(val); break;
//                 case "color": l.color = val; break;
//                 case "imageData": l.imageData = val; break;
//                 case "filterType": l.filterType = val; break;
                
//                 // FIX: Any other field (like 'level', 'intensity', etc.) goes into params map
//                 default:
//                     l.params.put(key, val);
//                     break;
//             }
//         }
//         return l;
//     }

//     private static class LayerData {

//         String type, color, imageData, filterType;
//         boolean visible = true;
//         float opacity = 1f;
//         Map<String, String> params = new HashMap<>();

//         int getIntParam(String k, int d) {
//             try {
//                 return params.containsKey(k)
//                         ? (int)Float.parseFloat(params.get(k))
//                         : d;
//             } catch (Exception e) {
//                 return d;
//             }
//         }
//     }
// }






















package com.example.backend.service;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.basic.*;
import com.example.backend.service.filters.*;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImagePipelineService {

    private final GrayscaleService grayscaleService;
    private final BrightnessService brightnessService;
    private final ContrastService contrastService;
    private final BlurService blurService;
    private final SharpenService sharpenService;
    private final BackgroundRemovalService backgroundRemovalService;

    public ImagePipelineService(
            GrayscaleService grayscaleService,
            BrightnessService brightnessService,
            ContrastService contrastService,
            BlurService blurService,
            SharpenService sharpenService,
            BackgroundRemovalService backgroundRemovalService) {

        this.grayscaleService = grayscaleService;
        this.brightnessService = brightnessService;
        this.contrastService = contrastService;
        this.blurService = blurService;
        this.sharpenService = sharpenService;
        this.backgroundRemovalService = backgroundRemovalService;
    }

    public ImageMatrixResponse compositeLayers(byte[] baseImageBytes, String layersJson) throws Exception {

        BufferedImage baseImage = ImageUtil.decode(baseImageBytes);
        if (baseImage == null) throw new IOException("Base image is null");

        int width = baseImage.getWidth();
        int height = baseImage.getHeight();

        int[] canvasMatrix = LinearMatrixUtil.toLinear(baseImage);
        List<LayerData> layers = parseLayersJson(layersJson);

        for (LayerData layer : layers) {
            // Fix: Respect visibility toggle correctly
            if (!layer.visible) continue;
            canvasMatrix = applyLayerToMatrix(canvasMatrix, layer, width, height);
        }

        BufferedImage finalImage = LinearMatrixUtil.fromLinear(canvasMatrix, width, height);

        return new ImageMatrixResponse(
                ImageUtil.encode(finalImage),
                canvasMatrix,
                width,
                height
        );
    }

    private int[] applyLayerToMatrix(int[] canvas, LayerData layer, int w, int h) throws Exception {

        switch (layer.type.toLowerCase()) {

            case "color": {
                int[] colorOverlay = generateColorMatrix(w, h, layer.color);
                return blendMatrices(canvas, colorOverlay, layer.opacity);
            }

            case "gradient": {
                int[] gradientOverlay = generateGradientMatrix(w, h, layer);
                return blendMatrices(canvas, gradientOverlay, layer.opacity);
            }

            case "image": {
                int[] imageOverlay = decodeImageToMatrix(layer.imageData, w, h);

                // Apply filters to the image layer itself
                imageOverlay = applyFilterToImageLayer(imageOverlay, layer, w, h);

                return blendMatrices(canvas, imageOverlay, layer.opacity);
            }

            case "filter": {
                int[] filteredCanvas = applyFilterMatrix(canvas, layer, w, h);
                return blendMatrices(canvas, filteredCanvas, layer.opacity);
            }

            default:
                return canvas;
        }
    }

    // =========================
    // IMAGE LAYER FILTER LOGIC
    // =========================
    private int[] applyFilterToImageLayer(int[] image, LayerData layer, int w, int h) throws Exception {

        if (layer.filterType == null) return image;

        switch (layer.filterType.toLowerCase()) {

            case "brightness":
                return brightnessService.processMatrix(
                        image,
                        layer.getIntParam("level", 0)
                );

            case "contrast":
                return contrastService.processMatrix(
                        image,
                        layer.getIntParam("level", 0)
                );

            case "blur":
                return blurService.processMatrix(
                        image,
                        w,
                        h,
                        layer.getIntParam("intensity", 0)
                );

            case "sharpen":
                return sharpenService.processMatrix(
                        image,
                        w,
                        h,
                        layer.getIntParam("intensity", 0)
                );

            case "grayscale":
                return grayscaleService.processMatrix(image);

            default:
                return image;
        }
    }

    // =========================
    // ADJUSTMENT FILTER LOGIC
    // =========================
    private int[] applyFilterMatrix(int[] canvas, LayerData layer, int w, int h) throws Exception {

        if (layer.filterType == null) return canvas;

        switch (layer.filterType.toLowerCase()) {

            case "brightness":
                return brightnessService.processMatrix(
                        canvas,
                        layer.getIntParam("level", 0)
                );

            case "contrast":
                return contrastService.processMatrix(
                        canvas,
                        layer.getIntParam("level", 0)
                );

            case "blur":
                return blurService.processMatrix(
                        canvas,
                        w,
                        h,
                        layer.getIntParam("intensity", 0)
                );

            case "sharpen":
                return sharpenService.processMatrix(
                        canvas,
                        w,
                        h,
                        layer.getIntParam("intensity", 0)
                );

            case "grayscale":
                return grayscaleService.processMatrix(canvas);

            case "background-removal":
            case "remove-bg":
                return backgroundRemovalService.processManualMatrix(
                        canvas,
                        w,
                        h,
                        layer.getIntParam("sensitivity", 30)
                );

            default:
                return canvas;
        }
    }

    // =========================
    // BLENDING
    // =========================
    private int[] blendMatrices(int[] base, int[] overlay, float opacity) {

        int[] result = new int[base.length];

        for (int i = 0; i < base.length; i++) {

            int b = base[i];
            int o = overlay[i];

            float aB = ((b >> 24) & 255) / 255f;
            float aO = (((o >> 24) & 255) / 255f) * opacity;

            float aOut = aO + aB * (1 - aO);

            if (aOut == 0) {
                result[i] = 0;
                continue;
            }

            int r = (int) (
                    (((o >> 16) & 255) * aO +
                     ((b >> 16) & 255) * aB * (1 - aO)) / aOut
            );

            int g = (int) (
                    (((o >> 8) & 255) * aO +
                     ((b >> 8) & 255) * aB * (1 - aO)) / aOut
            );

            int bl = (int) (
                    ((o & 255) * aO +
                     (b & 255) * aB * (1 - aO)) / aOut
            );

            result[i] =
                    (clamp((int)(aOut * 255)) << 24) |
                    (clamp(r) << 16) |
                    (clamp(g) << 8) |
                    clamp(bl);
        }

        return result;
    }

    private int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    // =========================
    // GENERATORS
    // =========================
    private int[] generateColorMatrix(int w, int h, String hex) {

        if (hex == null || hex.isEmpty()) hex = "#FFFFFF";
        int color = (255 << 24) | Integer.parseInt(hex.replace("#", ""), 16);

        int[] m = new int[w * h];
        Arrays.fill(m, color);
        return m;
    }

    private int[] generateGradientMatrix(int w, int h, LayerData layer) {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color c1 = Color.decode(layer.params.getOrDefault("gradientStart", "#FFFFFF"));
        Color c2 = Color.decode(layer.params.getOrDefault("gradientEnd", "#000000"));
        int angle = layer.getIntParam("gradientAngle", 0);

        double rad = Math.toRadians(angle);
        float cx = w / 2f;
        float cy = h / 2f;
        float d = (float)Math.sqrt(cx * cx + cy * cy);

        float x1 = cx - (float)Math.cos(rad) * d;
        float y1 = cy + (float)Math.sin(rad) * d;
        float x2 = cx + (float)Math.cos(rad) * d;
        float y2 = cy - (float)Math.sin(rad) * d;

        g.setPaint(new GradientPaint(x1, y1, c1, x2, y2, c2));
        g.fillRect(0, 0, w, h);
        g.dispose();

        return LinearMatrixUtil.toLinear(img);
    }

    private int[] decodeImageToMatrix(String dataUrl, int w, int h) throws Exception {

        String b64 = dataUrl.contains(",")
                ? dataUrl.substring(dataUrl.indexOf(",") + 1)
                : dataUrl;

        BufferedImage img = ImageUtil.decode(
                Base64.getDecoder().decode(b64.replaceAll("\\s", ""))
        );

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();

        return LinearMatrixUtil.toLinear(out);
    }

    // =========================
    // GENERIC JSON PARSER (FIXED)
    // =========================
        // ... (Keep the rest of your service methods unchanged) ...

    // =========================
    // MANUAL JSON PARSER (FIXED)
    // =========================
    private List<LayerData> parseLayersJson(String json) {
        List<LayerData> list = new ArrayList<>();
        Pattern p = Pattern.compile("\\{[^{}]*(\\{[^{}]*\\})*[^{}]*\\}");
        Matcher m = p.matcher(json);

        while (m.find()) {
            list.add(parseObject(m.group()));
        }
        return list;
    }

    private LayerData parseObject(String obj) {

        LayerData l = new LayerData();
        // Regex to capture "key": "value" OR "key": number/boolean
        Pattern p = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}]*))");
        Matcher m = p.matcher(obj);

        while (m.find()) {

            String key = m.group(1);
            // Group 2 is string value (e.g., "brightness"), Group 3 is raw value (e.g., 50)
            String val = (m.group(2) != null) ? m.group(2) : m.group(3).trim();

            if (val == null) continue;

            switch (key) {
                case "type": l.type = val; break;
                case "visible": l.visible = Boolean.parseBoolean(val); break;
                case "opacity": l.opacity = Float.parseFloat(val); break;
                case "color": l.color = val; break;
                case "imageData": l.imageData = val; break;
                case "filterType": l.filterType = val; break;

                // FIX: Explicitly capture known filter parameters and put them in the map
                case "level": l.params.put("level", val); break;
                case "intensity": l.params.put("intensity", val); break;
                case "sensitivity": l.params.put("sensitivity", val); break;
                
                // Handle gradient params if present
                case "gradientStart": l.params.put("gradientStart", val); break;
                case "gradientEnd": l.params.put("gradientEnd", val); break;
                case "gradientAngle": l.params.put("gradientAngle", val); break;
            }
        }
        return l;
    }

    private static class LayerData {

        String type, color, imageData, filterType;
        boolean visible = true;
        float opacity = 1f;
        Map<String, String> params = new HashMap<>();

        int getIntParam(String k, int d) {
            try {
                return params.containsKey(k)
                        ? (int)Float.parseFloat(params.get(k))
                        : d;
            } catch (Exception e) {
                return d;
            }
        }
    }
}