# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}
