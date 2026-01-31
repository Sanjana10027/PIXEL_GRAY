package com.example.backend.controller;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.*;
import com.example.backend.service.basic.*;
import com.example.backend.service.filters.*;
import com.example.backend.service.geometric.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private final GrayscaleService grayscaleService;
    private final BrightnessService brightnessService;
    private final ContrastService contrastService;
    private final RotateService rotateService;
    private final ZoomService zoomService;
    private final FlipService flipService;
    private final BlurService blurService;
    private final SharpenService sharpenService;
    private final CropService cropService;
    private final ImageSquareService imageSquareService;
    private final ImagePipelineService imagePipelineService;
    private final BackgroundRemovalService backgroundRemovalService;

    public ImageController(
            GrayscaleService grayscaleService, 
            BrightnessService brightnessService,
            ContrastService contrastService, 
            RotateService rotateService,
            ZoomService zoomService, 
            FlipService flipService,
            BlurService blurService, 
            SharpenService sharpenService,
            CropService cropService, 
            ImageSquareService imageSquareService,
            ImagePipelineService imagePipelineService,
            BackgroundRemovalService backgroundRemovalService) {
        this.grayscaleService = grayscaleService;
        this.brightnessService = brightnessService;
        this.contrastService = contrastService;
        this.rotateService = rotateService;
        this.zoomService = zoomService;
        this.flipService = flipService;
        this.blurService = blurService;
        this.sharpenService = sharpenService;
        this.cropService = cropService;
        this.imageSquareService = imageSquareService;
        this.imagePipelineService = imagePipelineService;
        this.backgroundRemovalService = backgroundRemovalService;
    }

    @PostMapping("/is-square")
    public boolean checkIfSquare(@RequestParam("image") MultipartFile file) throws IOException {
        return imageSquareService.isSquare(file.getBytes());
    }

    @PostMapping("/crop")
    public ImageMatrixResponse crop(
            @RequestParam("image") MultipartFile file,
            @RequestParam("x") int x,
            @RequestParam("y") int y,
            @RequestParam("w") int w,
            @RequestParam("h") int h,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return cropService.applyCrop(file.getBytes(), x, y, w, h, grayscale);
    }

    @PostMapping("/grayscale")
    public ImageMatrixResponse grayscale(@RequestParam("image") MultipartFile file) throws Exception {
        return grayscaleService.apply(file.getBytes());
    }

    @PostMapping("/brightness")
    public ImageMatrixResponse brightness(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("level") int level,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return brightnessService.apply(file.getBytes(), level, grayscale);
    }

    @PostMapping("/contrast")
    public ImageMatrixResponse contrast(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("level") int level,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return contrastService.apply(file.getBytes(), level, grayscale);
    }

    @PostMapping("/rotate")
    public ImageMatrixResponse rotate(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("angle") double angle,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return rotateService.apply(file.getBytes(), angle, grayscale);
    }

    @PostMapping("/flip/horizontal")
    public ImageMatrixResponse flipHorizontal(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return flipService.horizontal(file.getBytes(), grayscale);
    }

    @PostMapping("/flip/vertical")
    public ImageMatrixResponse flipVertical(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return flipService.vertical(file.getBytes(), grayscale);
    }

    @PostMapping("/blur")
    public ImageMatrixResponse blur(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("intensity") int intensity,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return blurService.apply(file.getBytes(), intensity, grayscale);
    }

    @PostMapping("/sharpen")
    public ImageMatrixResponse sharpen(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("intensity") int intensity,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return sharpenService.apply(file.getBytes(), intensity, grayscale);
    }

    @PostMapping("/zoom")
    public ImageMatrixResponse zoom(
            @RequestParam("image") MultipartFile file, 
            @RequestParam("scale") double scale,
            @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
        return zoomService.apply(file.getBytes(), scale, grayscale);
    }

    @PostMapping("/composite-layers")
    public ImageMatrixResponse compositeLayers(
            @RequestParam("image") MultipartFile file,
            @RequestParam("layers") String layersJson) throws Exception {
        return imagePipelineService.compositeLayers(file.getBytes(), layersJson);
    }

    /**
     * NEW: Background removal endpoint
     */
//     @PostMapping("/remove-background")
//     public ImageMatrixResponse removeBackground(@RequestParam("image") MultipartFile file) throws Exception {
//         return backgroundRemovalService.apply(file.getBytes());
//     }
// }
@PostMapping("/remove-background")
    public ImageMatrixResponse removeBackground(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "ai") String mode,
            @RequestParam(value = "sensitivity", defaultValue = "30") int sensitivity) throws Exception {
        
        return backgroundRemovalService.apply(file.getBytes(), mode, sensitivity);
    }
}