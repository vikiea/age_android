plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

android {
    namespace = "io.github.vikiea.age.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libgojni.so"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(files("libs/age-engine-classes.jar"))
}
