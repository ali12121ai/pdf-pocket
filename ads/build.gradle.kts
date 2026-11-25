plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pdfpocket.ads"
    compileSdk = 35
    buildToolsVersion = "36.1.0"
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.android.billingclient:billing-ktx:8.1.0")
    implementation(project(":data"))
}
