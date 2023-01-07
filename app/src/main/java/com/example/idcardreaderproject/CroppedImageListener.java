package com.example.idcardreaderproject;

import android.graphics.Bitmap;

public interface CroppedImageListener {
    void croppedBitmap(Bitmap bitmap);
    void isLookingRightSide(Boolean rightSideIsOkay);
    void isLookingLeftSide(Boolean leftSideIsOkay);
    void isLookingFrontSide(Boolean frontSideIsOkay);
}
