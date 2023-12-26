import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")


}


android {
    namespace = "com.example.tripbuddy"
    compileSdk = 34

    // Read properties from local.properties
    val propertiesFile = rootProject.file("local.properties")
    val properties = Properties().apply {
        load(FileInputStream(propertiesFile))
    }

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {

        applicationId = "com.example.tripbuddy"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Reference the API key using resValue
        resValue("string", "google_map_api_key", properties.getProperty("api_Key").toString())

        // Add the following line to define BuildConfigField
        buildConfigField("String", "GOOGLE_MAP_API_KEY", "\"${properties.getProperty("api_Key").toString()}\"")


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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


dependencies {

    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.maps:google-maps-services:2.2.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation ("com.google.maps:google-maps-services:2.2.0")
    implementation ("com.google.maps.android:android-maps-utils:3.8.2")
//    implementation ("com.google.android.gms:play-services-ads:22.6.0")


    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}