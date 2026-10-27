@file:OptIn(UnstableApi::class)

package io.github.hipsterbwp.rp5launcher.ui

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.hipsterbwp.rp5launcher.BuildConfig
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import kotlinx.coroutines.delay
import java.io.File

/** Disk cache for gameplay videos, so each one downloads once. */
private object VideoCache {
    private const val MAX_BYTES = 512L * 1024 * 1024
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): SimpleCache = cache ?: SimpleCache(
        File(context.cacheDir, "video"),
        LeastRecentlyUsedCacheEvictor(MAX_BYTES),
        StandaloneDatabaseProvider(context.applicationContext),
    ).also { cache = it }
}

/**
 * Looping, center-cropped gameplay video. Starts after a short pause on the same game (so fast
 * scrolling doesn't download every clip), fades in on its first frame, pauses with the app.
 */
@Composable
fun GameVideo(url: String?, withSound: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var firstFrame by remember(url) { mutableStateOf(false) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    val player = remember {
        val upstream = DefaultHttpDataSource.Factory().setUserAgent("rp5-launcher/${BuildConfig.VERSION_NAME}")
        val cached = CacheDataSource.Factory().setCache(VideoCache.get(context)).setUpstreamDataSourceFactory(upstream)
        // Downloads go through the cache; local files and ES-DE's videos (content://) are read directly.
        val source = DefaultDataSource.Factory(context, cached)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(source))
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame = true
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    player.volume = if (withSound) 1f else 0f

    LaunchedEffect(url) {
        player.stop()
        player.clearMediaItems()
        if (url == null) return@LaunchedEffect
        delay(START_DELAY_MS)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    // Pause while a game is running (the launcher is in the background).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (player.mediaItemCount > 0) player.play()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val alpha by animateFloatAsState(if (firstFrame) 1f else 0f, Motion.colorFade(), label = "video-in")
    AndroidView(
        factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
        update = { view -> centerCrop(view, videoSize) },
        modifier = modifier.alpha(alpha),
    )
}

private const val START_DELAY_MS = 700L

/** Scales the video to fill the view, cropping the overflow (TextureView stretches by default). */
private fun centerCrop(view: TextureView, size: VideoSize) {
    val w = view.width.toFloat()
    val h = view.height.toFloat()
    if (w <= 0 || h <= 0 || size.width <= 0 || size.height <= 0) {
        view.post { if (view.width > 0 && size.width > 0) centerCrop(view, size) }
        return
    }
    val videoW = size.width * size.pixelWidthHeightRatio
    val scale = maxOf(w / videoW, h / size.height)
    val matrix = Matrix().apply { setScale(videoW * scale / w, size.height * scale / h, w / 2, h / 2) }
    view.setTransform(matrix)
}
