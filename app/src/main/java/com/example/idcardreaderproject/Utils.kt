package com.example.idcardreaderproject

import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService

class Utils {

//    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
//    private lateinit var binding: ActivityBarcodeScanningBinding
//    private lateinit var cameraExecutor: ExecutorService
//
//    private fun initPreview() {
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraExecutor = Executors.newSingleThreadExecutor()
//
//        cameraProviderFuture.addListener(Runnable {
//            val cameraProvider = cameraProviderFuture.get()
//            bindPreview(cameraProvider)
//        }, ContextCompat.getMainExecutor(this))
//
//        binding.barcodeOverlay.post {
//            binding.barcodeOverlay.setViewFinder()
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
}