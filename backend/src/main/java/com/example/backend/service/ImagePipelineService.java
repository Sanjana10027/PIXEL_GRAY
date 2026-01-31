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
import java.util.HashMap;
import java.util.Map;

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

    public ImageMatrixResponse compositeLayers(byte[] baseImageBytes, String layersJson) throws Exception {
        BufferedImage baseImage = ImageUtil.decode(baseImageBytes);
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();

        // 1. Parse Layers (Ensures sequential order from JSON array)
        List<LayerData> layers = parseLayersJson(layersJson);
        
        // 2. Start with a clean copy of the base as the "Canvas"
        BufferedImage canvas = copyImage(baseImage);

        // 3. Process each layer strictly in the order received (Bottom -> Top)
        for (int i = 0; i < layers.size(); i++) {
            LayerData layer = layers.get(i);
            if (!layer.visible) continue;

            // Each step OVERWRITES canvas with the new blended result
            canvas = applyLayer(canvas, layer);
        }

        return new ImageMatrixResponse(
            ImageUtil.encode(canvas),
            LinearMatrixUtil.toLinear(canvas),
            width,
            height
        );
    }

    private BufferedImage applyLayer(BufferedImage canvas, LayerData layer) throws Exception {
        switch (layer.type) {
            case "color":
                return blendColorLayer(canvas, layer.color, layer.opacity);
            case "gradient":
                return blendGradientLayer(canvas, layer.gradientStart, layer.gradientEnd, 
                                         layer.gradientAngle, layer.opacity);
            case "image":
                return blendImageLayer(canvas, layer.imageData, layer.opacity);
            case "filter":
                return applyFilterToCanvas(canvas, layer);
            default:
                return canvas;
        }
    }

    private BufferedImage blendColorLayer(BufferedImage base, String colorHex, float opacity) {
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.setColor(hexToColor(colorHex));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return blendImages(base, overlay, opacity);
    }

    private BufferedImage blendGradientLayer(BufferedImage base, String start, String end, int angle, float opacity) {
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        
        double rads = Math.toRadians(angle);
        double dist = Math.sqrt(w * w + h * h) / 2.0;
        int x1 = (int)(w/2.0 - dist * Math.cos(rads));
        int y1 = (int)(h/2.0 - dist * Math.sin(rads));
        int x2 = (int)(w/2.0 + dist * Math.cos(rads));
        int y2 = (int)(h/2.0 + dist * Math.sin(rads));
        
        g.setPaint(new GradientPaint(x1, y1, hexToColor(start), x2, y2, hexToColor(end)));
        g.fillRect(0, 0, w, h);
        g.dispose();
        
        return blendImages(base, overlay, opacity);
    }

    private BufferedImage blendImageLayer(BufferedImage base, String dataUrl, float opacity) throws Exception {
        String b64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(",") + 1) : dataUrl;
        BufferedImage img = ImageUtil.decode(Base64.getDecoder().decode(b64.replaceAll("\\s", "")));
        
        BufferedImage resized = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, base.getWidth(), base.getHeight(), null);
        g.dispose();
        
        return blendImages(base, resized, opacity);
    }

    private BufferedImage applyFilterToCanvas(BufferedImage canvas, LayerData layer) throws Exception {
        byte[] canvasBytes = ImageUtil.encode(canvas);
        BufferedImage filtered = null;
        
        // Filter logic modifies the accumulated stack
        switch (layer.filterType) {
            case "brightness":
                filtered = ImageUtil.decode(brightnessService.apply(canvasBytes, layer.getIntParam("level", 0), false).image);
                break;
            case "contrast":
                filtered = ImageUtil.decode(contrastService.apply(canvasBytes, layer.getIntParam("level", 0), false).image);
                break;
            case "blur":
                filtered = ImageUtil.decode(blurService.apply(canvasBytes, layer.getIntParam("intensity", 0), false).image);
                break;
            case "sharpen":
                filtered = ImageUtil.decode(sharpenService.apply(canvasBytes, layer.getIntParam("intensity", 0), false).image);
                break;
            case "grayscale":
                filtered = grayscaleService.process(canvas);
                break;
        }
        
        return (filtered != null) ? blendImages(canvas, filtered, layer.opacity) : canvas;
    }

    /**
     * CORRECTED SEQUENTIAL BLENDING
     * Uses Standard Alpha Compositing: Result = Foreground * alpha + Background * (1 - alpha)
     */
    private BufferedImage blendImages(BufferedImage base, BufferedImage overlay, float opacity) {
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argbB = base.getRGB(x, y);
                int argbO = overlay.getRGB(x, y);

                float aB = ((argbB >> 24) & 0xFF) / 255.0f;
                float rB = ((argbB >> 16) & 0xFF) / 255.0f;
                float gB = ((argbB >> 8) & 0xFF) / 255.0f;
                float bB = (argbB & 0xFF) / 255.0f;

                // Apply layer-wide opacity to the overlay's intrinsic alpha
                float aO = (((argbO >> 24) & 0xFF) / 255.0f) * opacity;
                float rO = ((argbO >> 16) & 0xFF) / 255.0f;
                float gO = ((argbO >> 8) & 0xFF) / 255.0f;
                float bO = (argbO & 0xFF) / 255.0f;

                // Porter-Duff Source Over Equation
                float outA = aO + aB * (1 - aO);
                float outR = (outA > 0) ? (rO * aO + rB * aB * (1 - aO)) / outA : 0;
                float outG = (outA > 0) ? (gO * aO + gB * aB * (1 - aO)) / outA : 0;
                float outB = (outA > 0) ? (bO * aO + bB * aB * (1 - aO)) / outA : 0;

                res.setRGB(x, y, ((int)(outA * 255) << 24) | ((int)(outR * 255) << 16) | 
                                 ((int)(outG * 255) << 8) | (int)(outB * 255));
            }
        }
        return res;
    }

    private BufferedImage copyImage(BufferedImage s) {
        BufferedImage c = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(s, 0, 0, null);
        g.dispose();
        return c;
    }

    private Color hexToColor(String h) {
        h = h.replace("#", "");
        return new Color(Integer.parseInt(h.substring(0,2),16), 
                         Integer.parseInt(h.substring(2,4),16), 
                         Integer.parseInt(h.substring(4,6),16));
    }

    // --- MANUAL JSON PARSING LOGIC (NO LIBRARIES) ---

    private List<LayerData> parseLayersJson(String json) {
        List<LayerData> list = new ArrayList<>();
        String content = json.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
        
        int depth = 0;
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    list.add(parseObject(content.substring(start + 1, i)));
                }
            }
        }
        return list;
    }

    private LayerData parseObject(String obj) {
        LayerData l = new LayerData();
        int depth = 0;
        boolean inQuotes = false;
        int start = 0;
        for (int i = 0; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\"') inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c == '{') depth++;
                else if (c == '}') depth--;
                else if (c == ',' && depth == 0) {
                    processKV(l, obj.substring(start, i));
                    start = i + 1;
                }
            }
        }
        processKV(l, obj.substring(start));
        return l;
    }

    private void processKV(LayerData l, String kv) {
        int split = kv.indexOf(":");
        if (split == -1) return;
        String k = kv.substring(0, split).trim().replace("\"", "");
        String v = kv.substring(split + 1).trim();
        String rawV = v.startsWith("\"") ? v.substring(1, v.length()-1) : v;

        switch(k) {
            case "type": l.type = rawV; break;
            case "visible": l.visible = Boolean.parseBoolean(rawV); break;
            case "opacity": l.opacity = Float.parseFloat(rawV); break;
            case "color": l.color = rawV; break;
            case "gradientStart": l.gradientStart = rawV; break;
            case "gradientEnd": l.gradientEnd = rawV; break;
            case "gradientAngle": l.gradientAngle = Integer.parseInt(rawV); break;
            case "imageData": l.imageData = rawV; break;
            case "filterType": l.filterType = rawV; break;
            case "params": parseParams(l, v); break;
        }
    }

    private void parseParams(LayerData l, String p) {
        if (!p.contains("{")) return;
        String inner = p.substring(p.indexOf("{") + 1, p.lastIndexOf("}"));
        for (String pair : inner.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) l.params.put(kv[0].trim().replace("\"", ""), kv[1].trim().replace("\"", ""));
        }
    }

    private static class LayerData {
        String type, color, gradientStart, gradientEnd, imageData, filterType;
        boolean visible = true;
        float opacity = 1.0f;
        int gradientAngle = 90;
        Map<String, String> params = new HashMap<>();
        int getIntParam(String k, int d) {
            try { return Integer.parseInt(params.get(k)); } catch(Exception e) { return d; }
        }
    }
}