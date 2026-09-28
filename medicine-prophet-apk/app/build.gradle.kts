plugins {
    id("com.android.application")
}

android {
    namespace = "com.xalid.meditsinaproroka"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xalid.meditsinaproroka"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
}
