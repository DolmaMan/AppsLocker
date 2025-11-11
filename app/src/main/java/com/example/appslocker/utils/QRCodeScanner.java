package com.example.appslocker.utils;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeScanner {
    private static final String TAG = "QRCodeScanner";

    public interface QRScanListener {
        void onQRCodeScanned(String result);
        void onScanError(String error);
    }

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private boolean isScanning = false;
    private QRScanListener listener;


    public void startScanning(Context context, PreviewView previewView, LifecycleOwner lifecycleOwner, QRScanListener listener) {
        this.listener = listener;
        try {
            cameraExecutor = Executors.newSingleThreadExecutor();
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases(context, previewView, lifecycleOwner);
                    isScanning = true;
                } catch (Exception e) {
                    listener.onScanError("Ошибка инициализации камеры: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(context));

        } catch (Exception e) {
            listener.onScanError("Ошибка запуска сканера: " + e.getMessage());
        }
    }

    public void stopScanning() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        isScanning = false;
    }

    public boolean isScanning() {
        return isScanning;
    }

    private void bindCameraUseCases(Context context, PreviewView previewView, LifecycleOwner lifecycleOwner) {
        try {
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();

            BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

            imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                try {
                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees()
                    );

                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String rawValue = barcode.getRawValue();
                                    if (rawValue != null) {
                                        listener.onQRCodeScanned(rawValue);
                                        // Останавливаем сканирование после успешного распознавания
                                        stopScanning();
                                        break;
                                    }
                                }
                                imageProxy.close();
                            })
                            .addOnFailureListener(e -> {
                                listener.onScanError("Ошибка распознавания: " + e.getMessage());
                                imageProxy.close();
                            });

                } catch (Exception e) {
                    listener.onScanError("Ошибка обработки изображения: " + e.getMessage());
                    imageProxy.close();
                }
            });

            cameraProvider.unbindAll();
            Camera camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );

        } catch (Exception e) {
            listener.onScanError("Ошибка настройки камеры: " + e.getMessage());
        }
    }

    public static String parseQRData(String qrData) {
        try {
            JSONObject json = new JSONObject(qrData);
            return json.optString("parentDeviceId", "");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse QR data: " + e.getMessage());
            return qrData;
        }
    }
}