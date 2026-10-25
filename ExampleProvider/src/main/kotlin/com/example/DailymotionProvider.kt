package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class DailymotionProvider : MainAPI() {
    override var mainUrl = "https://www.dailymotion.com"
    override var name = "Waqar Hussain Dramas and Movies"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val api = "https://api.dailymotion.com"

    private val channels = listOf(
        "ARY Digital" to "arydigitalofficial",
        "Hum TV" to "humtv"
    )

    override val mainPage =
        mainPageOf(*channels.map { it.second to it.first }.toTypedArray())

    private fun toResult(v: DmVideo): SearchResponse? {
        val id = v.id ?: return null
        val title = v.title ?: return null
        return newMovieSearchResponse(title, id, TvType.Movie) {
            this.posterUrl = v.thumbnail_720_url
        }
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = "$api/user/${request.data}/videos" +
            "?fields=id,title,thumbnail_720_url&sort=recent&limit=40&page=$page"
        val res = app.get(url).parsed<DmList>()
        val list = res.list.orEmpty().mapNotNull { toResult(it) }
        return newHomePageResponse(request.name, list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val enc = java.net.URLEncoder.encode(query, "UTF-8")
        val all = mutableListOf<SearchResponse>()
        for (c in channels) {
            val url = "$api/user/${c.second}/videos" +
                "?fields=id,title,thumbnail_720_url&search=$enc&limit=20"
            try {
                val res = app.get(url).parsed<DmList>()
                all += res.list.orEmpty().mapNotNull { toResult(it) }
            } catch (e: Exception) {
            }
        }
        return all
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")
        val v = app.get("$api/video/$id?fields=id,title,description,thumbnail_720_url")
            .parsed<DmVideo>()
        return newMovieLoadResponse(v.title ?: "Video", id, TvType.Movie, id) {
            this.posterUrl = v.thumbnail_720_url
            this.plot = v.description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val id = data.substringAfterLast("/")
        return loadExtractor(
            "https://www.dailymotion.com/video/$id",
            subtitleCallback,
            callback
        )
    }

    data class DmList(val list: List<DmVideo>? = null)
    data class DmVideo(
        val id: String? = null,
        val title: String? = null,
        val description: String? = null,
        val thumbnail_720_url: String? = null
    )
}
