package com.samsung.android.security.v8.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * CryptoManager - إدارة التشفير
 * 
 * التشفير: AES-256-GCM
 * الحماية: كاملة لجميع البيانات الحساسة
 */
public class CryptoManager {
    
    private static final String TAG = "CryptoManager";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    
    private static SecretKey masterKey;
    private static Context context;
    
    /**
     * تهيئة مدير التشفير
     */
    public static void init(Context ctx) {
        context = ctx;
        masterKey = generateOrLoadKey();
    }
    
    /**
     * توليد أو تحميل المفتاح الرئيسي
     */
    private static SecretKey generateOrLoadKey() {
        try {
            // محاولة تحميل المفتاح المحفوظ
            String savedKey = PreferenceManager.getEncryptionKey(context);
            
            if (savedKey != null && !savedKey.isEmpty()) {
                byte[] keyBytes = Base64.decode(savedKey, Base64.DEFAULT);
                return new SecretKeySpec(keyBytes, "AES");
            }
            
            // توليد مفتاح جديد
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            
            // حفظ المفتاح
            String keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
            PreferenceManager.saveEncryptionKey(context, keyString);
            
            return key;
            
        } catch (Exception e) {
            Log.e(TAG, "Key generation error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * تشفير نص
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        
        try {
            // توليد IV عشوائي
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            
            // إعداد Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
            
            // التشفير
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // دمج IV مع النص المشفر
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return Base64.encodeToString(combined, Base64.DEFAULT);
            
        } catch (Exception e) {
            Log.e(TAG, "Encryption error: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * فك التشفير
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return "";
        }
        
        try {
            // فك Base64
            byte[] combined = Base64.decode(ciphertext, Base64.DEFAULT);
            
            // فصل IV عن النص المشفر
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);
            
            // إعداد Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
            
            // فك التشفير
            byte[] plaintext = cipher.doFinal(encrypted);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            Log.e(TAG, "Decryption error: " + e.getMessage());
            // إذا فشل فك التشفير، نحاول إرجاع النص الأصلي (قد يكون غير مشفر)
            try {
                return new String(Base64.decode(ciphertext, Base64.DEFAULT), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return ciphertext;
            }
        }
    }
    
    /**
     * تشفير بيانات ثنائية
     */
    public static byte[] encryptBytes(byte[] data) {
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
            
            byte[] ciphertext = cipher.doFinal(data);
            
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return combined;
            
        } catch (Exception e) {
            Log.e(TAG, "Bytes encryption error: " + e.getMessage());
            return data;
        }
    }
    
    /**
     * فك تشفير بيانات ثنائية
     */
    public static byte[] decryptBytes(byte[] data) {
        try {
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[data.length - IV_SIZE];
            System.arraycopy(data, 0, iv, 0, IV_SIZE);
            System.arraycopy(data, IV_SIZE, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
            
            return cipher.doFinal(encrypted);
            
        } catch (Exception e) {
            Log.e(TAG, "Bytes decryption error: " + e.getMessage());
            return data;
        }
    }
    
    /**
     * توليد مفتاح عشوائي
     */
    public static String generateRandomKey(int length) {
        byte[] key = new byte[length];
        new SecureRandom().nextBytes(key);
        return Base64.encodeToString(key, Base64.NO_WRAP);
    }
    
    /**
     * حساب Hash SHA-256
     */
    public static String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "SHA-256 error: " + e.getMessage());
            return "";
        }
    }
}
