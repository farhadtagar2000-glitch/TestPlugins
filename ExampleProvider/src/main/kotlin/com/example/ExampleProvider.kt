package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://www.themoviedb.org"
    override var name = "My TMDB Plugin"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    private val apiUrl = "https://api.themoviedb.org/3"
    private val imageUrl = "https://image.tmdb.org/t/p/w500"
    private val key = BuildConfig.TMDB_API_KEY

    override val mainPage = mainPageOf(
        "$apiUrl/movie/popular?api_key=$key" to "Popular Movies",
        "$apiUrl/tv/popular?api_key=$key" to "Popular Series"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val isTv = request.data.contains("/tv/")
        val res = app.get("${request.data}&page=$page").parsedSafe<TmdbList>()
        val items = res?.results?.mapNotNull { it.toSearch(isTv) } ?: emptyList()
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.get("$apiUrl/search/multi?api_key=$key&query=$query")
            .parsedSafe<TmdbList>()
        return res?.results?.mapNotNull {
            when (it.mediaType) {
                "movie" -> it.toSearch(false)
                "tv" -> it.toSearch(true)
                else -> null
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val isTv = url.startsWith("tv/")
        val item = app.get("$apiUrl/$url?api_key=$key").parsedSafe<TmdbItem>()
            ?: return null
        val title = item.title ?: item.name ?: return null
        val poster = item.posterPath?.let { imageUrl + it }
        val yearValue = (item.releaseDate ?: item.firstAirDate)
            ?.take(4)?.toIntOrNull()
        return if (isTv) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, emptyList<Episode>()) {
                this.posterUrl = poster
                this.plot = item.overview
                this.year = yearValue
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = item.overview
                this.year = yearValue
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
    }

    private fun TmdbItem.toSearch(isTv: Boolean): SearchResponse? {
        val t = this.title ?: this.name ?: return null
        val poster = this.posterPath?.let { imageUrl + it }
        val link = (if (isTv) "tv/" else "movie/") + this.id
        return if (isTv) {
            newTvSeriesSearchResponse(t, link, TvType.TvSeries) {
                this.posterUrl = poster
            }
        } else {
            newMovieSearchResponse(t, link, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    data class TmdbList(
        @JsonProperty("results") val results: List<TmdbItem>? = null
    )

    data class TmdbItem(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("media_type") val mediaType: String? = null,
        @JsonProperty("release_date") val releaseDate: String? = null,
        @JsonProperty("first_air_date") val firstAirDate: String? = null
    )
}
