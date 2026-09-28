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

    val keystorePath = System.getenv("MEDICINA_KEYSTORE")

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MEDICINA_STORE_PASSWORD")
                keyAlias = System.getenv("MEDICINA_KEY_ALIAS")
                keyPassword = System.getenv("MEDICINA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
