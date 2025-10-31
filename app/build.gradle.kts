plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleService) // com.google.gms.google-services
}

android {
    buildFeatures {
        viewBinding = true
    }

    namespace = "com.unison.binku"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unison.binku"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.loginGoogle)
    implementation(libs.glide)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.ccp)
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Firebase (BOM + KTX)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // implementation("com.google.firebase:firebase-analytics-ktx") // si lo necesitas luego

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
