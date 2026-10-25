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

    // Official channels. Check each handle on YouTube.
    private val handles = listOf("@ShemarooMovies", "@tipsofficial")

    override val mainPage =
        mainPageOf(*handles.map { it to it }.toTypedArray())

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val ch = app.get(
            "$api/channels?part=contentDetails&forHandle=${request.data}&key=$key"
        ).parsed<ChannelResp>()
        val uploads = ch.items?.firstOrNull()
            ?.contentDetails?.relatedPlaylists?.uploads
            ?: return newHomePageResponse(request.name, emptyList())
        val res = app.get(
            "$api/playlistItems?part=snippet&maxResults=30&playlistId=$uploads&key=$key"
        ).parsed<ListResp>()
        val list = res.items.orEmpty().mapNotNull { toResult(it) }
        return newHomePageResponse(request.name, list)
    }

    private fun toResult(item: Item): SearchResponse? {
        val s = item.snippet ?: return null
        val id = s.resourceId?.videoId ?: return null
        val title = s.title ?: return null
        return newMovieSearchResponse(
            title,
