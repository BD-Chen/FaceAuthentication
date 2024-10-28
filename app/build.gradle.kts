plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.faceauthentication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.faceauthentication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.camera.view)
    implementation("org.tensorflow:tensorflow-lite:2.9.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.3.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // CameraX core library using the camera2 implementation
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    // If you want to additionally use the CameraX Lifecycle library
    implementation(libs.camera.lifecycle)
    // If you want to additionally use the CameraX VideoCapture library
    implementation(libs.camera.video)
    // If you want to additionally use the CameraX View class
    implementation(libs.camera.view.v150alpha02)
    // If you want to additionally add CameraX ML Kit Vision Integration
    implementation(libs.camera.mlkit.vision)
    // If you want to additionally use the CameraX Extensions library
    implementation(libs.camera.extensions)
}