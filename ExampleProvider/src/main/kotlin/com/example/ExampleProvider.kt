package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "Waqar Hussain Tagar"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    private val tmdbUrl = "https://api.themoviedb.org/3"
    private val tmdbImage = "https://image.tmdb.org/t/p/w500"
    private val tmdbKey = BuildConfig.TMDB_API_KEY

    override val mainPage = mainPageOf(
        "collection:(feature_films) AND mediatype:(movies)" to "Classic Feature Films",
        "collection:(bollywood) AND mediatype:(movies)" to "Classic Bollywood",
        "collection:(classic_tv) AND mediatype:(movies)" to "Classic TV"
    )

    private fun searchUrl(q: String, page: Int): String {
        val query = java.net.URLEncoder.encode(q, "UTF-8")
        return "https://archive.org/advancedsearch.php?q=$query" +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=year" +
            "&sort%5B%5D=downloads+desc&rows=30&page=$page&output=json"
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val res = app.get(searchUrl(request.data, page)).parsedSafe<IaResult>()
        val items = res?.response?.docs?.mapNotNull { it.toSearch() } ?: emptyList()
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = "($query) AND mediatype:(movies)"
        val res = app.get(searchUrl(q, 1)).parsedSafe<IaResult>()
        return res?.response?.docs?.mapNotNull { it.toSearch() } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val url = url.removePrefix("https://archive.org/").trim('/')
        val meta = app.get("https://archive.org/metadata/$url").parsedSafe<IaMeta>()
            ?: return null
        val title = meta.metadata?.title ?: url
        var poster = "https://archive.org/services/img/$url"
        var plot = meta.metadata?.description
        val yearValue = meta.metadata?.date?.take(4)?.toIntOrNull()

        if (tmdbKey.isNotBlank()) {
            val q = java.net.URLEncoder.encode(title, "UTF-8")
            val t = app.get("$tmdbUrl/search/movie?api_key=$tmdbKey&query=$q")
                .parsedSafe<TmdbList>()?.results?.firstOrNull()
            if (t != null) {
                t.posterPath?.let { poster = tmdbImage + it }
                if (!t.overview.isNullOrBlank()) plot = t.overview
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
            this.year = yearValue
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val meta = app.get("https://archive.org/metadata/$data").parsedSafe<IaMeta>()
            ?: return false
        val files = meta.files ?: return false
        val video = files.firstOrNull { it.name?.endsWith(".mp4") == true }
            ?: files.firstOrNull { it.name?.endsWith(".m4v") == true }
            ?: files.firstOrNull { it.name?.endsWith(".ogv") == true }
            ?: return false
        val fileName = java.net.URLEncoder.encode(video.name ?: return false, "UTF-8")
            .replace("+", "%20")
        val link = "https://archive.org/download/$data/$fileName"
        callback(
            newExtractorLink(
                source = name,
                name = name,
                url = link,
                type = ExtractorLinkType.VIDEO
            ) {
                this.referer = mainUrl
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }

    private fun IaDoc.toSearch(): SearchResponse? {
        val id = this.identifier ?: return null
        val t = this.title ?: id
        return newMovieSearchResponse(t, id, TvType.Movie) {
            this.posterUrl = "https://archive.org/services/img/$id"
        }
    }

    data class IaResult(
        @JsonProperty("response") val response: IaResponse? = null
    )

    data class IaResponse(
        @JsonProperty("docs") val docs: List<IaDoc>? = null
    )

    data class IaDoc(
        @JsonProperty("identifier") val identifier: String? = null,
        @JsonProperty("title") val title: String? = null
    )

    data class IaMeta(
        @JsonProperty("metadata") val metadata: IaMetadata? = null,
        @JsonProperty("files") val files: List<IaFile>? = null
    )

    data class IaMetadata(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("date") val date: String? = null
    )

    data class IaFile(
        @JsonProperty("name") val name: String? = null
    )

    data class TmdbList(
        @JsonProperty("results") val results: List<TmdbItem>? = null
    )

    data class TmdbItem(
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("overview") val overview: String? = null
    )
}
