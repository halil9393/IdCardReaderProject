package com.example.idcardreaderproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private static final String OBJECT_DETECTION = "Object Detection";
    private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
    private static final String FACE_DETECTION = "Face Detection";
    private static final String STATE_SELECTED_MODEL = "selected_model";

    private static final String TASK_ID_CARD = "task_id_card";
    private static final String TASK_RIGHT_SIDE = "task_right_side";
    private static final String TASK_LEFT_SIDE = "task_left_side";
    private static final String TASK_FRONT_SIDE = "task_front_side";


    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;

    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable private Preview previewUseCase;
    @Nullable private ImageAnalysis analysisUseCase;
    @Nullable private VisionImageProcessor imageProcessor;
    private boolean needUpdateGraphicOverlayImageSourceInfo;

    private String selectedModel = OBJECT_DETECTION_CUSTOM;
    private String currentTask = TASK_ID_CARD;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int lensFacingFront = CameraSelector.LENS_FACING_FRONT;
    private CameraSelector cameraSelector;
    private ToggleButton facingSwitch;
    private TextView tvTaskDescription;

    private MutableLiveData<Boolean> rightSide = new MutableLiveData();
    private MutableLiveData<Boolean> leftSide = new MutableLiveData();
    private MutableLiveData<Boolean> frontSide = new MutableLiveData();
    private MutableLiveData<String> currentTaskString = new MutableLiveData();

    public Integer taskNumber = 0;
    public Bitmap latestBitmap;
    public Bitmap idCardBitmap;
    public Bitmap frontSideBitmapOne;
    public Bitmap frontSideBitmapTwo;
    public Bitmap frontSideBitmapThree;
    public Bitmap frontSideBitmapFour;
    public Bitmap frontSideBitmapFive;

    private String encodedIdCardBitmap;
    private String encodedFrontSideBitmapOne;
    private String encodedFrontSideBitmapTwo;
    private String encodedFrontSideBitmapThree;
    private String encodedFrontSideBitmapFour;
    private String encodedFrontSideBitmapFive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TAG", "onCreate");
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION);
        }
        checkCameraPermission();


    }

    public void continueFlow(){

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            Log.d("TAG", "previewView is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d("TAG", "graphicOverlay is null");
        }

        initObservers();

        tvTaskDescription = findViewById(R.id.tvTaskDescription);
        tvTaskDescription.setText(R.string.task_descripton_id_card);

        facingSwitch = findViewById(R.id.facing_switch);
        facingSwitch.setOnCheckedChangeListener(this);

        new ViewModelProvider(this, (ViewModelProvider.Factory) AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            bindAllCameraUseCases();
                        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTED_MODEL, selectedModel);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
// An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent.getItemAtPosition(position).toString();
        Log.d("TAG", "Selected model: " + selectedModel);
        bindAnalysisUseCase();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (cameraProvider == null) {
            return;
        }
        int newLensFacing =
                lensFacing == CameraSelector.LENS_FACING_FRONT
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;
        CameraSelector newCameraSelector =
                new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
        try {
            if (cameraProvider.hasCamera(newCameraSelector)) {
                Log.d("TAG", "Set facing to " + newLensFacing);
                lensFacing = newLensFacing;
                cameraSelector = newCameraSelector;
                bindAllCameraUseCases();
                return;
            }
        } catch (CameraInfoUnavailableException e) {
            // Falls through
        }
        Toast.makeText(
                        getApplicationContext(),
                        "This device does not have lens with facing: " + newLensFacing,
                        Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void initObservers(){
        rightSide.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(currentTaskString.getValue().equals(TASK_RIGHT_SIDE)){
                    if(aBoolean){
                        currentTaskString.setValue(TASK_FRONT_SIDE);
                        taskNumber += 1;
                        showStatusOnScreen(true);
                    }
                }
                else if(!currentTaskString.getValue().equals(TASK_FRONT_SIDE)){
                    if(aBoolean){
                        //yanlıs tarafa donmus demekdır tasklar sıfırlanır
                        taskNumber = 0;
                        currentTaskString.setValue(TASK_FRONT_SIDE);
                        showStatusOnScreen(false);
                    }
                }
            }
        });
        leftSide.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {

                if(currentTaskString.getValue().equals(TASK_LEFT_SIDE)){
                    if(aBoolean){
                        currentTaskString.setValue(TASK_FRONT_SIDE);
                        taskNumber += 1;
                        Log.i("tag_halilib","left başarılı");
                        showStatusOnScreen(true);
                    }else{
                        Log.i("tag_halilib","left başarısız");
                    }
                }
                else if(!currentTaskString.getValue().equals(TASK_FRONT_SIDE)){
                    if (aBoolean){
                        //yanlıs tarafa donmus demekdır tasklar sıfırlanır
                        taskNumber = 0;
                        currentTaskString.setValue(TASK_FRONT_SIDE);
                        showStatusOnScreen(false);
                    }
                }

            }
        });
        frontSide.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(currentTaskString.getValue().equals(TASK_FRONT_SIDE)){
                    if(aBoolean){
                        switch (taskNumber){
                            case 0:
                            case 1:
                                frontSideBitmapOne = latestBitmap;
                                encodedFrontSideBitmapOne = bitmapToBase64(frontSideBitmapOne);
                                break;
                            case 2:
                            case 3:
                                frontSideBitmapTwo = latestBitmap;
                                encodedFrontSideBitmapTwo = bitmapToBase64(frontSideBitmapTwo);
                                break;
                            case 4:
                            case 5:
                                frontSideBitmapThree = latestBitmap;
                                encodedFrontSideBitmapThree = bitmapToBase64(frontSideBitmapThree);
                                break;
                            case 6:
                            case 7:
                                frontSideBitmapFour = latestBitmap;
                                encodedFrontSideBitmapFour = bitmapToBase64(frontSideBitmapFour);
                                break;
                            case 8:
                            case 9:
                                frontSideBitmapFive = latestBitmap;
                                encodedFrontSideBitmapFive = bitmapToBase64(frontSideBitmapFive);
                                break;
                        }
                        if(taskNumber<7){
                            Random random = new Random();
                            if(random.nextBoolean()){
                                currentTaskString.setValue(TASK_LEFT_SIDE);
                            }else{
                                currentTaskString.setValue(TASK_RIGHT_SIDE);
                            }
                        }
                        else{
                            Toast.makeText(MainActivity.this, "CANLILIK TESTİ BAŞARILI", Toast.LENGTH_LONG).show();
                            tvTaskDescription.setText("CANLILIK TESTİ BAŞARILI");
                            currentTaskString.setValue("");
                            finishedPictures();
                            analysisUseCase.clearAnalyzer();
                            cameraProvider.unbindAll();
                        }
                        taskNumber += 1;
                        Log.i("tag_halilib","front başarılı");
                        showStatusOnScreen(true);
                    }else{
                        Log.i("tag_halilib","front başarısız");
                    }
                }
            }
        });
        currentTaskString.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String currentTaskString) {

                if(currentTaskString.equals(TASK_LEFT_SIDE)){
                    Toast.makeText(MainActivity.this, "SOLA BAKINIZ TASK="+taskNumber, Toast.LENGTH_LONG).show();
                    tvTaskDescription.setText("SOLA BAKINIZ");
                }else if(currentTaskString.equals(TASK_RIGHT_SIDE)){
                    Toast.makeText(MainActivity.this, "SAĞA BAKINIZ TASK="+taskNumber, Toast.LENGTH_LONG).show();
                    tvTaskDescription.setText("SAĞA BAKINIZ");
                }else if(currentTaskString.equals(TASK_FRONT_SIDE)){
                    Toast.makeText(MainActivity.this, "ORTAYA BAKINIZ TASK="+taskNumber, Toast.LENGTH_LONG).show();
                    tvTaskDescription.setText("ORTAYA BAKINIZ");
                }

            }
        });
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {
            switch (selectedModel) {
                case OBJECT_DETECTION:
                    Log.i("TAG", "Using Object Detector Processor");
                    ObjectDetectorOptions objectDetectorOptions =
                            PreferenceUtils.getObjectDetectorOptionsForLivePreview(this);
                    imageProcessor = new ObjectDetectorProcessor(this, objectDetectorOptions);
                    break;
                case OBJECT_DETECTION_CUSTOM:
                    Log.i("TAG", "Using Custom Object Detector Processor");
                    LocalModel localModel =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/object_labeler.tflite")
                                    .build();
                    CustomObjectDetectorOptions customObjectDetectorOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
                    imageProcessor = new ObjectDetectorProcessor(this, customObjectDetectorOptions, new CroppedImageListener() {
                        @Override
                        public void croppedBitmap(Bitmap bitmap) {

                            idCardBitmap = bitmap;
                            encodedIdCardBitmap = bitmapToBase64(bitmap);
                            //2. asamaya geciyoruz.(text recognize etmedim simdilik,daha doğru veri için  kimlik kartı textlerininde kontrolü gerekli)
                            selectedModel = FACE_DETECTION;
                            facingSwitch.setChecked(true);
                            currentTaskString.setValue(TASK_FRONT_SIDE);

                        }

                        @Override
                        public void isLookingRightSide(Boolean rightSideIsOkay) {
                            //for face
                        }

                        @Override
                        public void isLookingLeftSide(Boolean leftSideIsOkay) {
                            //for face
                        }

                        @Override
                        public void isLookingFrontSide(Boolean frontSideIsOkay) {
                            //for face
                        }
                    });
                    break;
               case FACE_DETECTION:
                    Log.i("TAG", "Using Face Detector Processor");
                    imageProcessor = new FaceDetectorProcessor(this, new CroppedImageListener() {
                        @Override
                        public void croppedBitmap(Bitmap bitmap) {
                            latestBitmap = bitmap;
                        }

                        @Override
                        public void isLookingRightSide(Boolean rightSideIsOkay) {
                            rightSide.setValue(rightSideIsOkay);
                        }

                        @Override
                        public void isLookingLeftSide(Boolean leftSideIsOkay) {
                            leftSide.setValue(leftSideIsOkay);
                        }

                        @Override
                        public void isLookingFrontSide(Boolean frontSideIsOkay) {
                            frontSide.setValue(frontSideIsOkay);
                        }
                    });
                    break;
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e("TAG", "Can not create image processor: " + selectedModel, e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                    } catch (MlKitException e) {
                        Log.e("TAG", "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }

    private void finishedPictures(){
        ImageView ivZero = findViewById(R.id.ivZero);//finishedConstraintLayout
        ImageView ivOne = findViewById(R.id.ivOne);
        ImageView ivTwo = findViewById(R.id.ivTwo);
        ImageView ivThree = findViewById(R.id.ivThree);
        ImageView ivFour = findViewById(R.id.ivFour);
        ImageView ivFive = findViewById(R.id.ivFive);
        ConstraintLayout finishedConstraintLayout = findViewById(R.id.finishedConstraintLayout);
        ivZero.setImageBitmap(idCardBitmap);
        ivOne.setImageBitmap(frontSideBitmapOne);
        ivTwo.setImageBitmap(frontSideBitmapTwo);
        ivThree.setImageBitmap(frontSideBitmapThree);
        ivFour.setImageBitmap(frontSideBitmapFour);
        ivFive.setImageBitmap(frontSideBitmapFive);
        finishedConstraintLayout.setVisibility(View.VISIBLE);
    }

    private void showStatusOnScreen(boolean statusIsSuccess){
        ImageView imageView = findViewById(R.id.ivStatus);
        if(statusIsSuccess){
            imageView.setImageResource(R.drawable.ic__check_circle);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.teal_700), android.graphics.PorterDuff.Mode.SRC_IN);
        }else{
            imageView.setImageResource(R.drawable.ic_round_cancel);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        imageView.setVisibility(View.VISIBLE);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                imageView.setVisibility(View.GONE);
            }
        }, 1500);
    }

    private String bitmapToBase64(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return encoded;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Dexter.withContext(this)
                    .withPermission(Manifest.permission.CAMERA)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            continueFlow();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(getApplicationContext(),"Kamera izni vermeden program  çalışmayacaktır. Ayarlardan lütfen kamera izni veriniz!",Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            Toast.makeText(getApplicationContext(),"Kamera izni vermeden program  çalışmayacaktır. Ayarlardan lütfen kamera izni veriniz!",Toast.LENGTH_LONG).show();
                            token.continuePermissionRequest();
                        }
                    }).check();
        }else{
            continueFlow();
        }

    }
}

