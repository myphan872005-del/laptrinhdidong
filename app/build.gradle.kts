plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Dòng này phải nằm ĐƯỚI kotlin.android
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.ued.custommaps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ued.custommaps"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        // Nâng cấp lên Java 17 để tương thích với các thư viện hiện đại
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Phiên bản 1.5.4 phù hợp với Kotlin 1.9.20
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- 1. OSMDroid (Bản đồ) ---
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // --- 2. Core & Lifecycle ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // THIẾU CÁI NÀY: Để dùng .asLiveData() trong ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // THIẾU CÁI NÀY: Để dùng observeAsState() trong MainScreen
    implementation("androidx.compose.runtime:runtime-livedata")

    // --- 3. Jetpack Compose (BOM) ---
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // --- 4. Room Database (Lưu trữ offline) ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- 5. Hilt (Dependency Injection) ---
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    // THIẾU CÁI NÀY: Để dùng hiltViewModel() trong AppNavigation
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // --- 6. Multimedia & Utilities ---
    implementation("io.coil-kt:coil-compose:2.5.0") // Load ảnh cực mượt cho Phase 2
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- 7. Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("io.coil-kt:coil-compose:2.6.0")
}