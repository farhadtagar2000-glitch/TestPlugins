package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.YoutubeExtractor
import com.lagradost.cloudstream3.utils.*

class YouTubeProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "Waqar Tagar Movies"
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

        val listUrl = "$api/playlistItems?part=snippet&maxResults=50&playlistId=$uploads&key=$key"
        val res = app.get(listUrl).parsed<ListResp>()
        val ids = res.items.orEmpty()
            .mapNotNull { it.snippet?.resourceId?.videoId }
            .joinToString(",")
        if (ids.isEmpty()) return newHomePageResponse(request.name, emptyList())

        val vidUrl = "$api/videos?part=snippet,contentDetails&id=$ids&key=$key"
        val vids = app.get(vidUrl).parsed<VideoResp>()
        val list = vids.items.orEmpty()
            .filter { toSeconds(it.contentDetails?.duration) >= 2400 }
            .mapNotNull { toResult(it) }
        return newHomePageResponse(request.name, list)
    }

    private fun toSeconds(d: String?): Int {
        if (d == null) return 0
        val m = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matchEntire(d)
            ?: return 0
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val mi = m.groupValues[2].toIntOrNull() ?: 0
        val s = m.groupValues[3].toIntOrNull() ?: 0
        return h * 3600 + mi * 60 + s
    }

    private fun toResult(item: VideoItem): SearchResponse? {
        val id = item.id ?: return null
        val title = item.snippet?.title ?: return null
        val poster = item.snippet.thumbnails?.high?.url
        return newMovieSearchResponse(title, id, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=")
        val s = app.get("$api/videos?part=snippet&id=$id&key=$key")
            .parsed<VideoResp>().items?.firstOrNull()?.snippet
        val title = s?.title ?: "Video"
        val poster = s?.thumbnails?.high?.url
        val desc = s?.description
        return newMovieLoadResponse(title, id, TvType.Movie, id) {
            this.posterUrl = poster
            this.plot = desc
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val id = data.substringAfter("v=")
        YoutubeExtractor().getUrl(
            "https://www.youtube.com/watch?v=$id",
            null,
            subtitleCallback,
            callback
        )
        return true
    }

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
    data class VideoResp(val items: List<VideoItem>? = null)
    data class VideoItem(
        val id: String? = null,
        val snippet: Snippet? = null,
        val contentDetails: VideoDetails? = null
    )
    data class VideoDetails(val duration: String? = null)
}
