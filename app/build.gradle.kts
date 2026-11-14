plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.yakyeon2"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Matches the Android app registered in app/google-services.json.
        applicationId = "com.gamcho.yakyeon"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "3.0"
        val apiBaseUrl = (project.findProperty("yakyeon.apiBaseUrl") as String?)
            ?: "https://kue200p7z3.execute-api.ap-northeast-2.amazonaws.com"
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"" + apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")
}