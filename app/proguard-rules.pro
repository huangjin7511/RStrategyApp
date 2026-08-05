# R策略 App 混淆规则

# WebView JavaScript 桥接接口不能混淆
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.rstrategy.app.MainActivity$WebAppInterface {
    *;
}

# Kotlin 协程
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler {}

# AndroidX
-dontwarn androidx.**
-keep class androidx.** { *; }

# 保留 Application 类
-keep class com.rstrategy.app.RStrategyApp {
    *;
}
