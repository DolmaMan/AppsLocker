plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.appslocker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appslocker"
        minSdk = 30
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation ("com.google.firebase:firebase-auth:22.1.2")
    implementation ("com.google.firebase:firebase-database:20.2.2")
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation ("com.firebaseui:firebase-ui-auth:8.0.2")

    // QR Code Generation
    implementation ("com.google.zxing:core:3.4.1")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // CameraX
    implementation ("androidx.camera:camera-core:1.2.3")
    implementation ("androidx.camera:camera-camera2:1.2.3")
    implementation ("androidx.camera:camera-lifecycle:1.2.3")
    implementation ("androidx.camera:camera-view:1.2.3")

    // Permissions
    implementation ("com.karumi:dexter:6.2.3")

    implementation ("com.google.mlkit:barcode-scanning:17.1.0")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}