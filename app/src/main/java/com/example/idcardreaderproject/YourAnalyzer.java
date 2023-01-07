package com.example.idcardreaderproject;

import android.media.Image;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;

@androidx.camera.core.ExperimentalGetImage
public class YourAnalyzer implements ImageAnalysis.Analyzer {

    @Override
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            // Pass image to an ML Kit Vision API
            // ...
        }
    }
}
