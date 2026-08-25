plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull

android {
    namespace = "com.majkeylab.qrscannercreator"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.majkeylab.qrscannercreator"
        minSdk = 29
        targetSdk = 37
        versionCode = 7
        versionName = "1.1.4"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-6991329209066655~5561017627"
        }
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-6991329209066655~5561017627"
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.fragment:fragment:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.google.zxing:core:3.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
