import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 读取版本信息
val versionProps = Properties()
val versionPropsFile = rootProject.file("version.properties")
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
}

val versionMajor = (versionProps["versionMajor"] as String?)?.toIntOrNull() ?: 1
val versionMinor = (versionProps["versionMinor"] as String?)?.toIntOrNull() ?: 0
val versionPatch = (versionProps["versionPatch"] as String?)?.toIntOrNull() ?: 0
val versionCodeNum = (versionProps["versionCode"] as String?)?.toIntOrNull() ?: 1

val versionNameStr = "$versionMajor.$versionMinor.$versionPatch"

android {
    namespace = "com.rstrategy.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rstrategy.app"
        minSdk = 24
        targetSdk = 34
        versionCode = versionCodeNum
        versionName = versionNameStr

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 签名配置 - CI 通过环境变量注入
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: ""
            val keystorePwd = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("KEY_ALIAS") ?: ""
            val keyPwd = System.getenv("KEY_PASSWORD") ?: ""

            if (keystorePath.isNotEmpty() && File(keystorePath).exists()) {
                storeFile = File(keystorePath)
                storePassword = keystorePwd
                keyAlias = keyAlias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 如果 CI 提供了签名信息就用，否则不签名（供 fork 项目也能构建）
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null && System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // 输出 APK 文件名规范化
    androidComponents {
        onVariants { variant ->
            val variantName = variant.name
            val capitalizedName = variantName.replaceFirstChar { it.uppercase() }
            tasks.register("print${capitalizedName}ApkPath") {
                doLast {
                    println("APK for $variantName: ${variant.outputs.first().outputFile.get()}")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
