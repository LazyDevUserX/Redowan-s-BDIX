package com.redowan

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class CircleFtpProvider : MainAPI() {
    override var mainUrl = "http://new.circleftp.net"
    private var mainApiUrl = "https://new.circleftp.net:5000"
    private val apiUrl = "https://15.1.1.50:5000"
    override var name = "Circle FTP"
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.Cartoon,
        TvType.AsianDrama,
        TvType.Documentary,
        TvType.OVA,
        TvType.Others
    )
    override val mainPage = mainPageOf(
        "80" to "Featured",
        "6" to "English Movies",
        "9" to "English & Foreign TV Series",
        "22" to "Dubbed TV Series",
        "2" to "Hindi Movies",
        "5" to "Hindi TV Series",
        "3" to "South Indian Dubbed Movie",
        "21" to "Anime Series",
        "1" to "Animation Movies",
        "85" to "Documentary",
        "15" to "WWE"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val json = tryApiCall("$mainApiUrl/api/posts?categoryExact=${request.data}&page=$page&order=desc&limit=10")
        val home = AppUtils.parseJson<PageData>(json.text).posts.mapNotNull { post ->
            toSearchResult(post)
        }
        return newHomePageResponse(request.name, home, true)
    }

    private fun toSearchResult(post: Post): SearchResponse? {
        if (post.type == "singleVideo" || post.type == "series") {
            return newAnimeSearchResponse(post.title, "$mainUrl/content/${post.id}", TvType.Movie) {
                this.posterUrl = "$apiUrl/uploads/${post.imageSm}"
                val check = post.title.lowercase()
                this.quality = getSearchQuality(check)
                addDubStatus(
                    dubExist = when {
                        "dubbed" in check -> true
                        "dual audio" in check -> true
                        "multi audio" in check -> true
                        else -> false
                    },
                    subExist = false
                )
            }
        }
        return null
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val json = tryApiCall("$mainApiUrl/api/posts?searchTerm=$query&order=desc")
        return AppUtils.parseJson<PageData>(json.text).posts.mapNotNull { post ->
            toSearchResult(post)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val json = tryApiCall(url.replace("$mainUrl/content/", "$mainApiUrl/api/posts/"))
        val urlCheck = json.url.contains(mainApiUrl)
        val loadData = AppUtils.parseJson<Data>(json.text)
        val title = loadData.title
        val poster = "$apiUrl/uploads/${loadData.image}"
        val description = loadData.metaData
        val year = selectUntilNonInt(loadData.year)

        return if (loadData.type == "singleVideo") {
            val movieData = json.parsed<Movies>()
            val link = if(urlCheck) movieData.content else linkToIp(movieData.content)
            val duration = getDurationFromString(loadData.watchTime)
            
            val mediaData = MediaData(videoUrl = link ?: "")

            newMovieLoadResponse(title, url, TvType.Movie, AppUtils.toJson(mediaData)) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.duration = duration
                addTrailer(movieData.trailer)
            }
        } else {
            val tvData = json.parsed<TvSeries>()
            val episodesData = mutableListOf<Episode>()
            tvData.content.forEachIndexed { seasonIndex, season ->
                season.episodes.forEachIndexed { episodeIndex, episode ->
                    val episodeUrl = episode.link
                    val link = if(urlCheck) episodeUrl else linkToIp(episodeUrl)
                    
                    val mediaData = MediaData(videoUrl = link)

                    episodesData.add(
                        Episode(
                            AppUtils.toJson(mediaData),
                            episode.title,
                            seasonIndex + 1,
                            episodeIndex + 1
                        )
                    )
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodesData) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                addTrailer(tvData.trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<MediaData>(data)
        
        callback.invoke(
            ExtractorLink(
                mainApiUrl,
                this.name,
                url = mediaData.videoUrl,
                mainApiUrl,
                quality = getVideoQuality(mediaData.videoUrl),
                isM3u8 = false,
                isDash = false,
                extraData = mapOf(
                    "hasMultipleAudioTracks" to "true",
                    "hasEmbeddedSubtitles" to "true"
                )
            )
        )
        return true
    }

    private fun linkToIp(data: String?): String {
        val replacements = mapOf(
            "index.circleftp.net" to "15.1.4.2",
            "index2.circleftp.net" to "15.1.4.5",
            "index1.circleftp.net" to "15.1.4.9",
            "ftp3.circleftp.net" to "15.1.4.7",
            "ftp4.circleftp.net" to "15.1.1.5",
            "ftp5.circleftp.net" to "15.1.1.15",
            "ftp6.circleftp.net" to "15.1.2.3",
            "ftp7.circleftp.net" to "15.1.4.8",
            "ftp8.circleftp.net" to "15.1.2.2",
            "ftp9.circleftp.net" to "15.1.2.12",
            "ftp10.circleftp.net" to "15.1.4.3",
            "ftp11.circleftp.net" to "15.1.2.6",
            "ftp12.circleftp.net" to "15.1.2.1",
            "ftp13.circleftp.net" to "15.1.1.18",
            "ftp15.circleftp.net" to "15.1.4.12",
            "ftp17.circleftp.net" to "15.1.3.8"
        )
        return data?.let { url ->
            replacements.entries.fold(url) { acc, (key, value) ->
                acc.replace(key, value)
            }
        } ?: ""
    }

    private fun selectUntilNonInt(string: String?): Int? {
        return string?.let { Regex("^\\d+").find(it)?.value?.toIntOrNull() }
    }

    private fun getSearchQuality(check: String?): SearchQuality? {
        return when {
            check == null -> null
            check.contains("webrip") || check.contains("web-dl") -> SearchQuality.WebRip
            check.contains("bluray") -> SearchQuality.BlueRay
            check.contains("hdts") || check.contains("hdcam") || check.contains("hdtc") -> SearchQuality.HdCam
            check.contains("dvd") -> SearchQuality.DVD
            check.contains("cam") -> SearchQuality.Cam
            check.contains("camrip") || check.contains("rip") -> SearchQuality.CamRip
            check.contains("hdrip") || check.contains("hd") || check.contains("hdtv") -> SearchQuality.HD
            check.contains("telesync") -> SearchQuality.Telesync
            check.contains("telecine") -> SearchQuality.Telecine
            else -> null
        }
    }

    private fun getVideoQuality(string: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(string ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    private suspend fun tryApiCall(url: String): Response {
        return try {
            app.get(url, verify = false, cacheTime = 60)
        } catch (e: Exception) {
            app.get(url.replace(mainApiUrl, apiUrl), verify = false, cacheTime = 60)
        }
    }

    data class PageData(val posts: List<Post>)
    data class Post(val id: Int, val type: String, val imageSm: String, val title: String, val name: String?)
    data class Data(
        val type: String,
        val imageSm: String,
        val title: String,
        val image: String,
        val metaData: String?,
        val name: String,
        val quality: String?,
        val year: String?,
        val watchTime: String?
    )
    data class TvSeries(val content: List<Content>, val trailer: String?)
    data class Content(val episodes: List<EpisodeData>, val seasonName: String)
    data class EpisodeData(val link: String, val title: String)
    data class Movies(val content: String?, val trailer: String?)
    data class MediaData(val videoUrl: String)
}
