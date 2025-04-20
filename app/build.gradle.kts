plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.crowdsenseplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.crowdsenseplus"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // 移除排除规则，确保高德地图SDK能够正确加载
    // configurations.all {
    //     resolutionStrategy {
    //         exclude(group = "com.amap.api", module = "3dmap")
    //         exclude(group = "com.amap.api", module = "location")
    //     }
    // }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // LiveData 集成
    implementation("androidx.compose.runtime:runtime-livedata:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // 高德地图集成 - 使用本地SDK文件
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "libs/AMap3DMap_AMapSearch_AMapLocation", "include" to listOf("*.jar"))))
    
    // 添加so库文件目录 - 直接使用libs目录中的so文件，避免使用不完整的jniLibs
    android.sourceSets.getByName("main") {
        // 完全移除旧的jniLibs引用，避免重复资源
        jniLibs.setSrcDirs(emptySet<File>())
        // 仅使用libs目录中的完整so库文件
        jniLibs.srcDir("libs/AMap3DMap_AMapSearch_AMapLocation")
    }
    
    // 确保正确引用高德地图SDK包名
    implementation(files("libs/AMap3DMap_AMapSearch_AMapLocation/AMap3DMap_10.1.201_AMapSearch_9.7.4_AMapLocation_6.4.9_20250317.jar"))
    
    // 添加额外的依赖以解决未解析引用问题
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}