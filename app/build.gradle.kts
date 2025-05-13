plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.servicios"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.servicios"
        minSdk = 33
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

    //Dependicia para escaneo de barras por la camara
    implementation ("com.journeyapps:zxing-android-embedded:4.2.0")
    implementation("com.google.zxing:core:3.3.0")

    //Implementacion para Conexión a base de datos
    implementation ("com.android.volley:volley:1.2.1")

    //Otras independencias
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}