dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 6

cloudstream {
    description = "TAGAR plugin for movies and series"
    authors = listOf("Waqartagar")

    status = 1

    tvTypes = listOf("Movie", "TvSeries")

    requiresResources = true
    language = "en"

    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    defaultConfig {
        buildConfigField("String", "TMDB_API_KEY", "\"${System.getenv("TMDB_API_KEY") ?: ""}\"")
buildConfigField("String", "YOUTUBE_API_KEY", "\"${System.getenv("YOUTUBE_API_KEY") ?: ""}\"")
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
