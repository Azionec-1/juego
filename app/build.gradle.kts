plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.juego"

    // ⬇⬇⬇ CAMBIO
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.juego"
        minSdk = 24

        // ⬇⬇⬇ CAMBIO
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // QR
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ⬇⬇⬇ CAMBIO: bajar activity para no exigir compileSdk 36 ni AGP 8.9.1
    implementation("androidx.activity:activity:1.8.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
