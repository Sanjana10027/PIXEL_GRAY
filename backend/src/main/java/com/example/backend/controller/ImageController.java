// package com.example.backend.controller;

// import com.example.backend.models.ImageMatrixResponse;
// import com.example.backend.service.CropService;
// import com.example.backend.service.ImageSquareService;
// import com.example.backend.service.basic.*;
// import com.example.backend.service.filters.*;
// import com.example.backend.service.geometric.*;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;

// @RestController
// @RequestMapping("/api/image")
// public class ImageController {

//     private final GrayscaleService grayscaleService;
//     private final BrightnessService brightnessService;
//     private final ContrastService contrastService;
//     private final RotateService rotateService;
//     private final ZoomService zoomService;
//     private final FlipService flipService;
//     private final BlurService blurService;
//     private final SharpenService sharpenService;
//     private final CropService cropService;
//     private final ImageSquareService imageSquareService;

//     public ImageController(
//             GrayscaleService grayscaleService, BrightnessService brightnessService,
//             ContrastService contrastService, RotateService rotateService,
//             ZoomService zoomService, FlipService flipService,
//             BlurService blurService, SharpenService sharpenService,
//             CropService cropService, ImageSquareService imageSquareService) {
//         this.grayscaleService = grayscaleService;
//         this.brightnessService = brightnessService;
//         this.contrastService = contrastService;
//         this.rotateService = rotateService;
//         this.zoomService = zoomService;
//         this.flipService = flipService;
//         this.blurService = blurService;
//         this.sharpenService = sharpenService;
//         this.cropService = cropService;
//         this.imageSquareService = imageSquareService;
//     }

//     @PostMapping("/is-square")
//     public boolean checkIfSquare(@RequestParam("image") MultipartFile file) throws IOException {
//         return imageSquareService.isSquare(file.getBytes());
//     }

//     @PostMapping("/crop")
//     public ImageMatrixResponse crop(
//             @RequestParam("image") MultipartFile file,
//             @RequestParam("x") int x,
//             @RequestParam("y") int y,
//             @RequestParam("w") int w,
//             @RequestParam("h") int h,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         // Pass the grayscale toggle to the crop service
//         return cropService.applyCrop(file.getBytes(), x, y, w, h, grayscale);
//     }

//     @PostMapping("/grayscale")
//     public ImageMatrixResponse grayscale(@RequestParam("image") MultipartFile file) throws Exception {
//         return grayscaleService.apply(file.getBytes());
//     }

//     @PostMapping("/brightness")
//     public ImageMatrixResponse brightness(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("level") int level,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         // Now passing the grayscale boolean to the service
//         return brightnessService.apply(file.getBytes(), level, grayscale);
//     }

//     @PostMapping("/contrast")
//     public ImageMatrixResponse contrast(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("level") int level,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return contrastService.apply(file.getBytes(), level, grayscale);
//     }

//     @PostMapping("/rotate")
//     public ImageMatrixResponse rotate(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("angle") double angle,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return rotateService.apply(file.getBytes(), angle, grayscale);
//     }

//     @PostMapping("/flip/horizontal")
//     public ImageMatrixResponse flipHorizontal(
//             @RequestParam("image") MultipartFile file,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return flipService.horizontal(file.getBytes(), grayscale);
//     }

//     @PostMapping("/flip/vertical")
//     public ImageMatrixResponse flipVertical(
//             @RequestParam("image") MultipartFile file,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return flipService.vertical(file.getBytes(), grayscale);
//     }

//     @PostMapping("/blur")
//     public ImageMatrixResponse blur(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("intensity") int intensity,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return blurService.apply(file.getBytes(), intensity, grayscale);
//     }

//     @PostMapping("/sharpen")
//     public ImageMatrixResponse sharpen(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("intensity") int intensity,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return sharpenService.apply(file.getBytes(), intensity, grayscale);
//     }

//     @PostMapping("/zoom")
//     public ImageMatrixResponse zoom(
//             @RequestParam("image") MultipartFile file, 
//             @RequestParam("scale") double scale,
//             @RequestParam(value = "grayscale", defaultValue = "false") boolean grayscale) throws Exception {
//         return zoomService.apply(file.getBytes(), scale, grayscale);
//     }
// }



package com.example.backend.controller;

import com.example.backend.models.ImageMatrixResponse;
import com.example.backend.service.CropService;
import com.example.backend.service.ImageSquareService;
import com.example.backend.service.LayerCompositeService;
import com.example.backend.service.basic.*;
import com.example.backend.service.filters.*;
import com.example.backend.service.geometric.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// No Jackson dependencies needed - all JSON parsing is done manually

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
    private final LayerCompositeService layerCompositeService;

    public ImageController(
            GrayscaleService grayscaleService, BrightnessService brightnessService,
            ContrastService contrastService, RotateService rotateService,
            ZoomService zoomService, FlipService flipService,
            BlurService blurService, SharpenService sharpenService,
            CropService cropService, ImageSquareService imageSquareService,
            LayerCompositeService layerCompositeService) {
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
        this.layerCompositeService = layerCompositeService;
    }

    @PostMapping("/is-square")
    public boolean checkIfSquare(@RequestParam("image") MultipartFile file) throws IOException {
        return imageSquareService.isSquare(file.getBytes());
    }

    @PostMapping("/composite-layers")
    public ImageMatrixResponse compositeLayers(
            @RequestParam("image") MultipartFile file,
            @RequestParam("layers") String layersJson) throws Exception {
        return layerCompositeService.composeLayers(file.getBytes(), layersJson);
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
}
