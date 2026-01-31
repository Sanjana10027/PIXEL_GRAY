package com.example.backend.service;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.matrix.LinearMatrixUtil;
import com.example.backend.service.util.ImageUtil;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@Service
public class BackgroundRemovalService {

    private static String PYTHON_PATH = null;
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "pixellab_ai";
    private static final int DEFAULT_SENSITIVITY = 30;

    static {
        detectPython();
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private static void detectPython() {
        String[] commands = {"python3", "python", "py"};
        for (String cmd : commands) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                if (p.waitFor() == 0) {
                    PYTHON_PATH = cmd;
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Main entry point compatible with your BrightnessService style.
     */
    public ImageMatrixResponse apply(byte[] bytes, String mode, int sensitivity) throws Exception {
        BufferedImage input = ImageUtil.decode(bytes);
        BufferedImage output;

        // Use requested sensitivity or default
        int finalSensitivity = (sensitivity <= 0) ? DEFAULT_SENSITIVITY : sensitivity;

        // Try AI first if requested
        if (("ai".equalsIgnoreCase(mode) || "auto".equalsIgnoreCase(mode)) && isRembgAvailable()) {
            try {
                output = removeBackgroundWithAI(input);
            } catch (Exception e) {
                // Fallback to manual if Python script fails
                output = processManual(input, finalSensitivity);
            }
        } else {
            output = processManual(input, finalSensitivity);
        }

        return new ImageMatrixResponse(
                ImageUtil.encode(output),
                LinearMatrixUtil.toLinear(output),
                output.getWidth(),
                output.getHeight()
        );
    }

    // --- AI LOGIC (REMBG BRIDGE) ---

    private BufferedImage removeBackgroundWithAI(BufferedImage image) throws Exception {
        String id = UUID.randomUUID().toString();
        File inputFile = new File(TEMP_DIR, "in_" + id + ".png");
        File outputFile = new File(TEMP_DIR, "out_" + id + ".png");
        File scriptFile = new File(TEMP_DIR, "script_" + id + ".py");

        try {
            ImageIO.write(image, "PNG", inputFile);

            String script = String.format(
                "import sys\n" +
                "try:\n" +
                "    from rembg import remove\n" +
                "    from PIL import Image\n" +
                "    input_img = Image.open(r'%s').convert('RGBA')\n" +
                "    output_img = remove(input_img)\n" +
                "    output_img.save(r'%s')\n" +
                "    sys.exit(0)\n" +
                "except Exception as e:\n" +
                "    print(e)\n" +
                "    sys.exit(1)", 
                inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

            Files.write(scriptFile.toPath(), script.getBytes());

            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, scriptFile.getAbsolutePath());
            Process p = pb.start();
            p.waitFor();

            if (!outputFile.exists()) throw new Exception("AI failed");
            return ImageIO.read(outputFile);
        } finally {
            inputFile.delete();
            outputFile.delete();
            scriptFile.delete();
        }
    }

    private boolean isRembgAvailable() {
        if (PYTHON_PATH == null) return false;
        try {
            Process p = new ProcessBuilder(PYTHON_PATH, "-c", "import rembg").start();
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    // --- IMPROVED MANUAL ALGORITHM ---

    public BufferedImage processManual(BufferedImage input, int sensitivity) {
        int w = input.getWidth();
        int h = input.getHeight();

        Color bg = sampleEdges(input);

        // Create mask
        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double dist = Math.sqrt(Math.pow(r - bg.getRed(), 2) + 
                                        Math.pow(g - bg.getGreen(), 2) + 
                                        Math.pow(b - bg.getBlue(), 2));
                
                mask[y][x] = dist > (sensitivity + 20); 
            }
        }

        mask = keepLargestComponent(mask, w, h);

        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask[y][x]) {
                    output.setRGB(x, y, input.getRGB(x, y));
                } else {
                    output.setRGB(x, y, 0x00000000); // Transparent
                }
            }
        }
        return output;
    }

    private Color sampleEdges(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        // Sample top and bottom edges
        for (int x = 0; x < w; x += 5) {
            int rgbT = img.getRGB(x, 0);
            int rgbB = img.getRGB(x, h - 1);
            rSum += ((rgbT >> 16) & 0xFF) + ((rgbB >> 16) & 0xFF);
            gSum += ((rgbT >> 8) & 0xFF) + ((rgbB >> 8) & 0xFF);
            bSum += (rgbT & 0xFF) + (rgbB & 0xFF);
            count += 2;
        }

        if (count == 0) return Color.WHITE;
        return new Color((int)(rSum/count), (int)(gSum/count), (int)(bSum/count));
    }

    private boolean[][] keepLargestComponent(boolean[][] mask, int w, int h) {
        boolean[][] visited = new boolean[h][w];
        int maxArea = 0;
        List<int[]> largestComponent = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask[y][x] && !visited[y][x]) {
                    List<int[]> current = new ArrayList<>();
                    Queue<int[]> q = new LinkedList<>();
                    q.add(new int[]{x, y});
                    visited[y][x] = true;

                    while(!q.isEmpty()){
                        int[] p = q.poll();
                        current.add(p);
                        int[][] neighbors = {{p[0]+1,p[1]}, {p[0]-1,p[1]}, {p[0],p[1]+1}, {p[0],p[1]-1}};
                        for(int[] n : neighbors){
                            if(n[0]>=0 && n[0]<w && n[1]>=0 && n[1]<h && mask[n[1]][n[0]] && !visited[n[1]][n[0]]){
                                visited[n[1]][n[0]] = true;
                                q.add(n);
                            }
                        }
                    }
                    if(current.size() > maxArea){
                        maxArea = current.size();
                        largestComponent = current;
                    }
                }
            }
        }

        boolean[][] result = new boolean[h][w];
        for(int[] p : largestComponent) result[p[1]][p[0]] = true;
        return result;
    }
}