/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.idcardreaderproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectorProcessor";

  private final FaceDetector detector;
  private CroppedImageListener croppedImageListener;
  private Context context;

  public FaceDetectorProcessor(Context context) {
    super(context);
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
    this.context = context;
  }

  public FaceDetectorProcessor(Context context,CroppedImageListener croppedImageListener) {
    super(context);
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
    this.croppedImageListener = croppedImageListener;
    this.context = context;
  }

  @Override
  public void processImageProxy(ImageProxy image) throws MlKitException {

  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<Face>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay, Bitmap originalCameraImage) {
    for (Face face : faces) {
      if(faces.size()==0){
        Toast.makeText(context, "Yüz Algılanmadı", Toast.LENGTH_SHORT).show();
      }else if(faces.size()>1){
        Toast.makeText(context, "Birden fazla yüz algılandı. HATA !", Toast.LENGTH_SHORT).show();
      }else{
        checkSides(face,originalCameraImage);
        graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
      }
      logExtrasForTesting(face);
    }
  }

  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // All landmarks
      int[] landMarkTypes =
          new int[] {
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.NOSE_BASE
          };
      String[] landMarkTypesStrings =
          new String[] {
            "MOUTH_BOTTOM",
            "MOUTH_RIGHT",
            "MOUTH_LEFT",
            "RIGHT_EYE",
            "LEFT_EYE",
            "RIGHT_EAR",
            "LEFT_EAR",
            "RIGHT_CHEEK",
            "LEFT_CHEEK",
            "NOSE_BASE"
          };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: "
                  + landMarkTypesStrings[i]
                  + " is :"
                  + landmarkPositionStr);
        }
      }
      Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }

  private void checkSides(Face face, Bitmap latestBitmap ){
    if(face!=null){
      Integer tolerance = 20;

        if((face.getHeadEulerAngleY()-tolerance) > 0) croppedImageListener.isLookingLeftSide(true);
        else croppedImageListener.isLookingLeftSide(false);


        if((face.getHeadEulerAngleY()+tolerance) < 0) croppedImageListener.isLookingRightSide(true);
        else croppedImageListener.isLookingRightSide(false);


        if(face.getHeadEulerAngleY() < 2.5 && face.getHeadEulerAngleY() > -2.5){
          croppedImageListener.croppedBitmap(cropDetectedBitmap(latestBitmap,face));
          croppedImageListener.isLookingFrontSide(true);
        }
        else croppedImageListener.isLookingFrontSide(false);

    }
  }

  private Bitmap cropDetectedBitmap(Bitmap bitmap, Face face) {
    Log.i("tag_halil","cropDetectedFace: $bitmap");
    Rect rect = face.getBoundingBox();
    Integer x = Math.max(rect.left,0);
    Integer y = Math.max(rect.top,0);

    Integer width = rect.width();
    Integer height = rect.height();
    Integer useHeight = 0;
    if((y+height) > bitmap.getHeight()){
      useHeight = bitmap.getHeight() -y;
    }  else useHeight =height;

    Integer useWith =0;
    if((x+width) > bitmap.getWidth()) {
      useWith= bitmap.getWidth()-x ;
    } else useWith=width;
//    Integer originX =0;
//    if (x + width > bitmap.width){
//      bitmap.width - width;
//    } else x
//    Integer originY = if (y + height > bitmap.height) (bitmap.height - height) else y
    Log.i("tag_halil","bitmap.width:${bitmap.width} bitmap.height:${bitmap.height} width:$width height:$height x:$x y:$y $useHeight $useWith $originY $originX");
    Bitmap croppedBitmap = Bitmap.createBitmap(
            bitmap,
            x,
            y,
            useWith,
            useHeight

    );
    return croppedBitmap;
  }
}
