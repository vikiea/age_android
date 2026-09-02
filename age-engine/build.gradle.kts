plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.vikiea.age.engine"
    compileSdk = 37

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

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(files("libs/age-engine-classes.jar"))
}
