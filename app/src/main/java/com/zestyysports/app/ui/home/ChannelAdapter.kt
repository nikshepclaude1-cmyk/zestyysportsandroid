package com.zestyysports.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.zestyysports.app.R
import com.zestyysports.app.data.model.Channel

class ChannelAdapter(
    private val onPlay: (Channel) -> Unit,
    private val onFavorite: (Channel) -> Unit,
    private var favoriteIds: Set<String> = emptySet()
) : ListAdapter<Channel, ChannelAdapter.ViewHolder>(DIFF) {

    fun updateFavorites(ids: Set<String>) {
        favoriteIds = ids
        notifyItemRangeChanged(0, itemCount, "fav")
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView     = view.findViewById(R.id.iv_logo)
        val name: TextView      = view.findViewById(R.id.tv_name)
        val group: TextView     = view.findViewById(R.id.tv_group)
        val btnFav: ImageView   = view.findViewById(R.id.btn_favorite)

        init {
            view.setOnClickListener { onPlay(getItem(adapterPosition)) }
            btnFav.setOnClickListener { onFavorite(getItem(adapterPosition)) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bind(holder, getItem(position), null)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("fav")) {
            val ch = getItem(position)
            holder.btnFav.setImageResource(
                if (favoriteIds.contains(ch.id)) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
            return
        }
        bind(holder, getItem(position), payloads)
    }

    private fun bind(holder: ViewHolder, ch: Channel, payloads: Any?) {
        holder.name.text = ch.name
        holder.group.text = ch.group.uppercase()
        holder.btnFav.setImageResource(
            if (favoriteIds.contains(ch.id)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
        if (ch.logo.isNotBlank()) {
            Glide.with(holder.logo)
                .load(ch.logo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_tv_placeholder)
                .error(R.drawable.ic_tv_placeholder)
                .into(holder.logo)
        } else {
            holder.logo.setImageResource(R.drawable.ic_tv_placeholder)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Channel>() {
            override fun areItemsTheSame(a: Channel, b: Channel) = a.id == b.id
            override fun areContentsTheSame(a: Channel, b: Channel) = a == b
        }
    }
}
