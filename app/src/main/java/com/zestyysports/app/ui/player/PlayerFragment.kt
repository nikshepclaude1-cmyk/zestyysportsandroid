package com.zestyysports.app.ui.player

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.bumptech.glide.Glide
import com.zestyysports.app.R
import com.zestyysports.app.data.repository.AppDatabase
import com.zestyysports.app.data.repository.ChannelRepository
import com.zestyysports.app.databinding.FragmentPlayerBinding
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args: PlayerFragmentArgs by navArgs()
    private var player: ExoPlayer? = null

    private val viewModel: PlayerViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        PlayerViewModelFactory(ChannelRepository(db.favoriteDao()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvChannelName.text = args.channelName
        binding.tvChannelGroup.text = args.channelGroup.uppercase()

        if (args.channelLogo.isNotBlank()) {
            Glide.with(this).load(args.channelLogo)
                .error(R.drawable.ic_tv_placeholder)
                .into(binding.ivChannelLogo)
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnFav.setOnClickListener { viewModel.toggleFavorite(args.channelId) }

        viewModel.favoriteIds.observe(viewLifecycleOwner) { ids ->
            binding.btnFav.setImageResource(
                if (ids.contains(args.channelId)) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
        }

        initPlayer()
    }

    private fun initPlayer() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Referer", "https://zestyysports.vercel.app/")
                    .build()
                chain.proceed(req)
            }
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        player = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exo ->
                binding.playerView.player = exo

                val mediaItem = MediaItem.fromUri(args.channelUrl)
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true

                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        binding.progressPlayer.isVisible = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) binding.tvBuffering.isVisible = false
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        binding.tvError.isVisible = true
                        binding.tvError.text = "Stream error — try another channel"
                        binding.progressPlayer.isVisible = false
                    }
                })
            }
    }

    override fun onStart() {
        super.onStart()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}
