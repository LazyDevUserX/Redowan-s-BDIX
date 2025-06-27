plugins {
    id("com.lagradost.cloudstream3.provider")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// The version of your plugin
version = 1

android {
    namespace = "com.redowan.bdixftpbd" // Unique namespace for the plugin
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // You can add specific dependencies here if needed
}

// Cloudstream metadata block
cloudstream {
    description = "A BDIX FTP provider for Cloudstream. Works even during internet shutdowns."
    authors = listOf("LazyDevUserX")
    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1 // will be 3 if unspecified

    tvTypes = listOf(
        "Movie",
        "TvSeries",
        "Anime",
        "AnimeMovie",
        "OVA",
        "Cartoon",
        "AsianDrama",
        "Others",
        "Documentary",
    )
    language = "bn"

    // TODO: Add a URL to a direct image link for the plugin icon if you have one.
    // iconUrl = "http://example.com/icon.png"
}
