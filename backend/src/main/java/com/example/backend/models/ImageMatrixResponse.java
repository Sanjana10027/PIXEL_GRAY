package com.example.backend.models;

public class ImageMatrixResponse {
    public byte[] image;
    public int[] linear;
    public int width;
    public int height;

    public ImageMatrixResponse(byte[] image, int[] linear, int width, int height) {
        this.image = image;
        this.linear = linear;
        this.width = width;
        this.height = height;
    }
}
