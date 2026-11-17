plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ilvir.telegramwatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ilvir.telegramwatch"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Нативная libtdjni.so для каждой архитектуры кладётся сборкой CI в
    // app/src/main/jniLibs/<abi>/libtdjni.so — отдельная NDK-сборка в этом
    // модуле не нужна, TDLib подключается как готовая бинарная библиотека.
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    // org.json используется TdApi для сериализации — уже в Android SDK, отдельной
    // зависимости не требует.
}
