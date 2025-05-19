plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "ovo.sypw.journal"
    compileSdk = 35

    defaultConfig {
        applicationId = "ovo.sypw.journal"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 限制语言资源，仅保留中文和英文
//        androidResources {
//            localeFilters += listOf("zh", "en")
//        }
        
        // 启用R8优化
//        proguardFiles(
//            getDefaultProguardFile("proguard-android-optimize.txt"),
//            "proguard-rules.pro"
//        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
        }
        debug {
            // Debug模式下不启用minify，避免编译警告并加快构建速度
            isMinifyEnabled = false
//            isShrinkResources = true
        }
    }
    
    // 启用拆分APK，根据ABI进行优化
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86")
            isUniversalApk = false
        }
    }
    
    // 配置bundled - 推荐的现代方式替代splits.density
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    
    // 配置构建特性
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        // 优化Kotlin编译，移除不支持的标志
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
    buildToolsVersion = "35.0.1"
}

dependencies {
    implementation(fileTree(baseDir = "libs"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // 添加Material图标支持
    implementation(libs.androidx.material)
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)
    
    // 添加Navigation导航支持
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    
    implementation(libs.retrofit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room数据库
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

//    hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

//    markdown
    implementation(libs.compose.markdown)

    implementation (libs.tasks.genai)

    // ML Kit 自然语言处理库
    //noinspection UseTomlInstead
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation(libs.translate)
    
    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.task.text)
    implementation(libs.tensorflow.lite.gpu) // 可选，用于GPU加速
}