package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class YouTubeProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "YouTube Movies"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)

    private val key = BuildConfig.YOUTUBE_API_KEY
    private val api = "https://www.googleapis.com/youtube/v3"

    private val handles = listOf("@ShemarooMovies", "@tipsofficial")

    override val mainPage =
        mainPageOf(*handles.map { it to it }.toTypedArray())

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val chUrl = "$api/channels?part=contentDetails&forHandle=${request.data}&key=$key"
        val ch = app.get(chUrl).parsed<ChannelResp>()
        val uploads = ch.items?.firstOrNull()
            ?.contentDetails?.relatedPlaylists?.uploads
            ?: return newHomePageResponse(request.name, emptyList())
        val listUrl = "$api/playlistItems?part=snippet&maxResults=30&playlistId=$uploads&key=$key"
        val res = app.get(listUrl).parsed<ListResp>()
        val list = res.items.orEmpty().mapNotNull { toResult(it) }
        return newHomePageResponse(request.name, list)
    }

    private fun toResult(item: Item): SearchResponse? {
        val s = item.snippet ?: return null
        val id = s.resourceId?.videoId ?: return null
        val title = s.title ?: return null
        val link = "https://www.youtube.com/watch?v=" + id
        val poster = s.thumbnails?.high?.url
        return newMovieSearchResponse(title, link, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=")
        val s = app.get("$api/videos?part=snippet&id=$id&key=$key")
            .parsed<ListResp>().items?.firstOrNull()?.snippet
        val title = s?.title ?: "YouTube video"
        val poster = s?.thumbnails?.high?.url
        val desc = s?.description
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = desc
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = loadExtractor(data, subtitleCallback, callback)

    data class ChannelResp(val items: List<ChItem>? = null)
    data class ChItem(val contentDetails: CD? = null)
    data class CD(val relatedPlaylists: RP? = null)
    data class RP(val uploads: String? = null)
    data class ListResp(val items: List<Item>? = null)
    data class Item(val snippet: Snippet? = null)
    data class Snippet(
        val title: String? = null,
        val description: String? = null,
        val thumbnails: Thumbs? = null,
        val resourceId: ResId? = null
    )
    data class Thumbs(val high: Thumb? = null)
    data class Thumb(val url: String? = null)
    data class ResId(val videoId: String? = null)
}
