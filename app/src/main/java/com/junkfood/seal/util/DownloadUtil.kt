package com.junkfood.seal.util

import android.util.Log
import com.junkfood.seal.BaseApplication
import com.junkfood.seal.BaseApplication.Companion.audioDownloadDir
import com.junkfood.seal.BaseApplication.Companion.context
import com.junkfood.seal.BaseApplication.Companion.videoDownloadDir
import com.junkfood.seal.R
import com.junkfood.seal.database.DownloadedVideoInfo
import com.junkfood.seal.util.PreferenceUtil.CUSTOM_PATH
import com.junkfood.seal.util.PreferenceUtil.SUBDIRECTORY
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

object DownloadUtil {
    class Result(val resultCode: ResultCode, val filePath: List<String>?) {
        companion object {
            fun failure(): Result {
                return Result(ResultCode.EXCEPTION, null)
            }

            fun success(filePaths: List<String>?): Result {
                return Result(ResultCode.SUCCESS, filePaths)
            }
        }
    }


    enum class ResultCode {
        SUCCESS, EXCEPTION
    }

    private const val TAG = "DownloadUtil"

    data class PlaylistInfo(
        val url: String = "",
        val size: Int = 0,
        val title: String = ""
    )

    suspend fun getPlaylistInfo(playlistURL: String): PlaylistInfo {
        val downloadPlaylist: Boolean = PreferenceUtil.getValue(PreferenceUtil.PLAYLIST)
        var playlistCount = 1
        var playlistTitle = "Unknown"
        if (downloadPlaylist) {
            TextUtil.makeToastSuspend(context.getString(R.string.fetching_playlist_info))
            val request = YoutubeDLRequest(playlistURL)
            with(request) {
                addOption("--flat-playlist")
                addOption("-J")
                addOption("-R", "1")
                addOption("--socket-timeout", "5")
            }
            for (s in request.buildCommand())
                Log.d(TAG, s)
            val resp: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, null)
            val jsonObj = JSONObject(resp.out)
            val tp: String = jsonObj.getString("_type")
            if (tp == "playlist") {
                playlistCount = jsonObj.getInt("playlist_count")
                playlistTitle = jsonObj.getString("title")
            }
        }
        return PlaylistInfo(playlistURL, playlistCount, playlistTitle)
    }

    suspend fun fetchVideoInfo(url: String, playlistItem: Int = 1): VideoInfo {
        TextUtil.makeToastSuspend(context.getString(R.string.fetching_info))
        val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url).apply {
            addOption("-R", "1")
            addOption("--playlist-items", playlistItem)
            addOption("--socket-timeout", "5")
        })
        with(videoInfo) {
            if (title.isNullOrEmpty() or ext.isNullOrEmpty()) {
                throw Exception("Empty videoinfo")
            }
        }
        return videoInfo
    }

    suspend fun downloadVideo(
        videoInfo: VideoInfo,
        playlistInfo: PlaylistInfo,
        playlistItem: Int = -1,
        progressCallback: ((Float, Long, String) -> Unit)?
    ): Result {

        val extractAudio: Boolean = PreferenceUtil.getValue(PreferenceUtil.EXTRACT_AUDIO)
        val createThumbnail: Boolean = PreferenceUtil.getValue(PreferenceUtil.THUMBNAIL)
        val downloadPlaylist: Boolean = PreferenceUtil.getValue(PreferenceUtil.PLAYLIST)
        val subdirectory: Boolean = PreferenceUtil.getValue(SUBDIRECTORY)
        val customPath: Boolean = PreferenceUtil.getValue(CUSTOM_PATH)
        val concurrentFragments: Float = PreferenceUtil.getConcurrentFragments()
        val url = playlistInfo.url.ifEmpty {
            videoInfo.webpageUrl ?: return Result.failure()
        }
        val request = YoutubeDLRequest(url)
        val id = if (extractAudio) "${url.hashCode()}audio" else url.hashCode().toString()
        val pathBuilder = StringBuilder()

        with(request) {
            addOption("--no-mtime")

            if (extractAudio or (videoInfo.ext.matches(Regex("mp3|m4a|opus")))) {
                pathBuilder.append(audioDownloadDir)

                addOption("-x")
                when (PreferenceUtil.getAudioFormat()) {
                    1 -> {
                        addOption("--audio-format", "mp3")
                        addOption("--audio-quality", "0")
                    }
                    2 -> {
                        addOption("--audio-format", "m4a")
                        addOption("--audio-quality", "0")
                    }
                }
                addOption("--embed-metadata")
                addOption("--embed-thumbnail")
                if (playlistInfo.title.isNotEmpty()) {
                    if (playlistItem != -1)
                        addOption("--playlist-items", playlistItem)
                    addOption("--parse-metadata", "%(album|${playlistInfo.title})s:%(meta_album)s")
//                    addOption("--parse-metadata", "%(track_number,playlist_index)d:%(meta_track)d")
                } else
                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")

            } else {
                pathBuilder.append(videoDownloadDir)

                val sorter = StringBuilder()
                when (PreferenceUtil.getVideoQuality()) {
                    1 -> sorter.append("res:1440")
                    2 -> sorter.append("res:1080")
                    3 -> sorter.append("res:720")
                    4 -> sorter.append("res:480")
                }
                when (PreferenceUtil.getVideoFormat()) {
                    1 -> sorter.append(",ext:mp4")
                    2 -> sorter.append(",ext:webm")
                }
                if (sorter.isNotEmpty())
                    addOption("-S", sorter.toString())
            }
            if (concurrentFragments > 0f) {
                addOption("--concurrent-fragments", (concurrentFragments * 16).roundToInt())
            }
            if (createThumbnail) {
                addOption("--write-thumbnail")
                addOption("--convert-thumbnails", "jpg")
            }
            if (!downloadPlaylist) {
                addOption("--no-playlist")
            }
            if (subdirectory) {
                pathBuilder.append("/${videoInfo.extractorKey}")
            }
            addOption("-P", pathBuilder.toString())
            if (customPath)
                addOption("-o", PreferenceUtil.getOutputPathTemplate() + "%(title).60s$id.%(ext)s")
            else
                addOption("-o", "%(title).60s$id.%(ext)s")

            for (s in request.buildCommand())
                Log.d(TAG, s)
            YoutubeDL.getInstance().execute(request, progressCallback)
        }

        val filePaths = FileUtil.scanFileToMediaLibrary(id, pathBuilder.toString())
        if (filePaths != null)
            for (path in filePaths) {
                DatabaseUtil.insertInfo(
                    DownloadedVideoInfo(
                        0,
                        videoInfo.title,
                        if (videoInfo.uploader == null) "null" else videoInfo.uploader,
                        videoInfo.webpageUrl ?: url,
                        TextUtil.urlHttpToHttps(videoInfo.thumbnail ?: ""),
                        path,
                        videoInfo.extractorKey
                    )
                )
            }
        return Result.success(filePaths)
    }

    suspend fun updateYtDlp(): String {
        withContext(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(context)
                TextUtil.makeToastSuspend(context.getString(R.string.yt_dlp_up_to_date))
            } catch (e: Exception) {
                TextUtil.makeToastSuspend(context.getString(R.string.yt_dlp_update_fail))
            }
        }
        YoutubeDL.getInstance().version(context)?.let {
            BaseApplication.ytdlpVersion = it
            PreferenceUtil.updateString(PreferenceUtil.YT_DLP, it)
        }
        return BaseApplication.ytdlpVersion
    }


}