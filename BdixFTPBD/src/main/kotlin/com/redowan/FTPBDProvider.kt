package com.redowan.BdixFTPBD

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.MimeTypeUtils
import org.jsoup.nodes.Element

class FTPBDProvider : MainAPI() {
    // Set to the specific Anime & Cartoon directory for our initial test
    override var mainUrl = "http://server5.ftpbd.net/FTP-5/Anime%20&%20Cartoon%20TV%20Series/"
    override var name = "FTPBD (Anime)" // Renamed for clarity during testing
    override var lang = "bn"
    override val hasMainPage = true

    // We are focusing on TV Series (Anime/Cartoon) for this test
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.Cartoon,
        TvType.TvSeries
    )

    // This function turns a link on the page into a search result item
    private fun Element.toSearchResult(): SearchResponse? {
        val href = this.attr("abs:href")
        // Skips links to the parent directory
        if (text().startsWith("Parent Directory")) return null

        // Cleans up the name by removing trailing slashes
        val title = text().removeSuffix("/")

        // Creates a TV series search result, as this section contains series
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            posterUrl = null // FTP listings don't have posters
        }
    }

    // This function loads the content for the main page of the plugin
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("a").mapNotNull { it.toSearchResult() }
        val list = HomePageList("Anime & Cartoon TV Series", items)
        return newHomePageResponse(list, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // We will implement search later if the site supports it
        return emptyList()
    }

    // This function is called when you click on a specific show
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        // Extracts a clean title for the show
        val title = document.selectFirst("title")?.text()
            ?.substringAfter("Index of /FTP-5/Anime & Cartoon TV Series/")?.trim()?.removeSuffix("/")
            ?: url.split('/').dropLast(2).last()

        // Lists all the files in the directory as episodes
        val episodes = document.select("a").mapNotNull { element ->
            val epHref = element.attr("abs:href")
            val epText = element.text()
            // Skip non-media files and parent links
            if (epText.startsWith("Parent Directory") || MimeTypeUtils.getMimeType(epHref) == "application/octet-stream") {
                null
            } else {
                newEpisode(epHref) {
                    name = epText
                }
            }
        }

        return newTvSeriesLoadResponse(
            name = title,
            url = url,
            type = TvType.TvSeries,
            episodes = episodes
        )
    }

    // This function is called when you click on an episode to play it
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // The 'data' is the direct URL to the media file
        callback.invoke(
            ExtractorLink(
                source = name,
                name = name,
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = data.endsWith(".m3u8")
            )
        )
        return true
    }
}
