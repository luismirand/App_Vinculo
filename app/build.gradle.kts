plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google services Gradle plugin
    //antes era://id("com.google.gms.google-services")
    alias(libs.plugins.googleService)
}

android {

    buildFeatures{
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
    implementation(libs.firebaseAuth)
    implementation(libs.firebaseDatabase)
    implementation(libs.loginGoogle)
    implementation(libs.glide)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.ccp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //la quitamos por ahora //implementation("com.google.firebase:firebase-analytics")
}