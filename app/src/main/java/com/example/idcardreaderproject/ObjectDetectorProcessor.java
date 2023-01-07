package com.example.idcardreaderproject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.util.List;

/** A processor to run object detector. */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

  private static final String TAG = "ObjectDetectorProcessor";

  private final ObjectDetector detector;
  private CroppedImageListener croppedImageListener;

  public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options) {
    super(context);
    detector = ObjectDetection.getClient(options);
  }

  public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options,CroppedImageListener croppedImageListener) {
    super(context);
    detector = ObjectDetection.getClient(options);
    this.croppedImageListener = croppedImageListener;
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
  protected Task<List<DetectedObject>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay, Bitmap originalCameraImage) {
    for (DetectedObject object : results) {
      for(int i=0;i< object.getLabels().size();i++){
        if(object.getLabels().get(i).getConfidence() >0.8f && object.getLabels().get(i).getIndex() == 510){
          Bitmap bitmap = cropDetectedBitmap(originalCameraImage,object);
          if(croppedImageListener!=null) croppedImageListener.croppedBitmap(bitmap);
          graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
        }
      }
//      graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Object detection failed!", e);
  }

  private Bitmap cropDetectedBitmap(Bitmap bitmap, DetectedObject detectedObject) {
    Log.i("tag_halil","cropDetectedFace: $bitmap");
    Rect rect = detectedObject.getBoundingBox();
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
