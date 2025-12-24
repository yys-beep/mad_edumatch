plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mad_edumatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mad_edumatch"
        minSdk = 24
        targetSdk = 36
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
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck.debug)
    implementation("com.google.firebase:firebase-messaging")

    // AndroidX / UI libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.recyclerview)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 1. Modify the Appwrite dependency to exclude the problematic BOM
    implementation("io.appwrite:sdk-for-android:4.0.0") {
        exclude(group = "com.squareup.okhttp3", module = "okhttp-bom")
    }

    // 2. Manual OkHttp import
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
