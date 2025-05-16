# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留项目中使用的依赖库的一些必要规则

# 保留源文件名和行号，有助于调试
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留泛型信息
-keepattributes Signature

# 保留注解
-keepattributes *Annotation*

# 保留所有序列化相关的类
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留Room相关
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt相关规则
-keep class * implements dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class * extends dagger.hilt.android.internal.managers.HiltComponentManager

# Retrofit规则
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn kotlinx.serialization.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# Compose相关规则
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.material.icons.** { *; }
-keepclassmembers class * implements androidx.compose.runtime.Composable {
    public <methods>;
}

# Kotlin相关
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**

# 保留Model类
-keep class ovo.sypw.journal.data.model.** { *; }
-keep class ovo.sypw.journal.data.local.** { *; }

# 保留ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Coil
-dontwarn io.coil-kt.**
-keep class io.coil-kt.** { *; }

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class com.google.mediapipe.** { *; }

# 自动修复缺失类的警告
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.mediapipe.framework.image.BitmapExtractor
-dontwarn com.google.mediapipe.framework.image.ByteBufferExtractor
-dontwarn com.google.mediapipe.framework.image.MPImage
-dontwarn com.google.mediapipe.framework.image.MPImageProperties
-dontwarn com.google.mediapipe.framework.image.MediaImageExtractor
-dontwarn com.google.protobuf.Internal$ProtoMethodMayReturnNull
-dontwarn com.google.protobuf.Internal$ProtoNonnullApi
-dontwarn com.google.protobuf.ProtoField
-dontwarn com.google.protobuf.ProtoPresenceBits
-dontwarn com.google.protobuf.ProtoPresenceCheckedField

# OpenCL Native Libraries
-keep class org.khronos.opencl.** { *; }
-dontwarn org.khronos.opencl.**

# 移除日志输出代码
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 删除未使用的kotlin.jvm.internal对象的bug解决方案
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
    public static void throwNpe(...);
    public static void throwJavaNpe(...);
    public static void throwAssert(...);
    public static void throwIllegalArgument(...);
    public static void throwIllegalState(...);
}