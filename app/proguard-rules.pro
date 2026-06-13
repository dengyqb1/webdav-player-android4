# ProGuard rules for WebDAV Player
-keepattributes *Annotation*
-keep class com.github.sardine.** { *; }
-keep class org.apache.http.** { *; }
-dontwarn javax.xml.**
