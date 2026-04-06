// File: build.gradle.kts (Project)
plugins {
    // AGP 8.3.2 là bản "vàng" chạy cực mượt với Gradle 8.7
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false

    // Kotlin 1.9.22 rất ổn định cho Compose và Kapt
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Hilt 2.51.1 là bản mới nhất, tương thích Java 17
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}