plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.mmbridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mmbridge"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "7.0-SafeMode"
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.mlkit:text-recognition:16.0.0")
}
