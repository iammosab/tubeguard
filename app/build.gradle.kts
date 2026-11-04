import java.util.Properties

plugins {
    id("com.android.application")
}

// Release signing comes from keystore.properties (gitignored); without it only debug builds are possible.
val signing = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }
}

android {
    namespace = "io.github.iammosab.tubeguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.iammosab.tubeguard"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // TVs are ARM; the crypto libraries otherwise bring x86 builds too.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    packaging {
        // BouncyCastle's post-quantum tables; nothing here uses them.
        resources.excludes += "org/bouncycastle/pqc/**"
    }

    signingConfigs {
        if (signing != null) create("release") {
            storeFile = file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("com.github.MuntashirAkon:libadb-android:3.1.1")
    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
