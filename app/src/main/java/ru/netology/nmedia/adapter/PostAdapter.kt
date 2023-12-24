package ru.netology.nmedia.adapter

import android.icu.text.SimpleDateFormat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.chooseSeparator
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.FragmentCardPostBinding
import ru.netology.nmedia.databinding.TimingSeparatorBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Separator
import ru.netology.nmedia.dto.TimingSeparator
import ru.netology.nmedia.load
import ru.netology.nmedia.loadCircle
import ru.netology.nmedia.numberRepresentation
import java.util.Date
import java.util.Locale

interface OnInteractionListener {
    fun like(post: Post)
    fun share(post: Post)
    fun view(post: Post)
    fun remove(post: Post)
    fun edit(post: Post)
    fun play(post: Post)
    fun showPost(post: Post)
    fun showPhoto (post: Post)
    fun addLink(id: Long, link: String)
    fun retryPost (content: String)
}

class PostsAdapter(private val onInteractionListener: OnInteractionListener) :
    PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Ad -> R.layout.card_ad
            is Post -> R.layout.fragment_card_post
            is TimingSeparator -> R.layout.timing_separator
            null -> throw IllegalArgumentException("unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            R.layout.card_ad -> {
                val binding =
                    CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }

            R.layout.fragment_card_post -> {
                val binding =
                    FragmentCardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }

            R.layout.timing_separator -> {
                val binding = TimingSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SeparatorViewHolder (binding)
            }

            else -> throw IllegalArgumentException("unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = getItem(position)) {
            is Post -> (holder as? PostViewHolder)?.bind(item)
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is TimingSeparator -> (holder as? SeparatorViewHolder)?.bind(item)
            null -> error("unknown item type")
        }
    }
}


class AdViewHolder(
    private val binding: CardAdBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(ad: Ad) {
        binding.image.load("${BuildConfig.BASE_URL}/media/${ad.image}")
    }
}

class SeparatorViewHolder(
    private val binding: TimingSeparatorBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(timingSeparator: TimingSeparator) {
        binding.nameSeparator.text = timingSeparator.name
    }
}

class PostViewHolder(
    private val binding: FragmentCardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {

        binding.apply {
            author.text = post.author
            published.text = SimpleDateFormat("dd MMM yyyy в HH:mm", Locale.getDefault())
                .format(Date((post.published * 1000))) //post.published нужно умножить на 1000, так как Date() ожидает время в миллисекундах.
            content.text = post.content
            content.movementMethod = ScrollingMovementMethod()
            likesIcon.isChecked = post.likedByMe
            likesIcon.text = numberRepresentation(post.likes)
            shareIcon.text = numberRepresentation(post.share)
            viewIcon.text = numberRepresentation(post.views)
            groupLink.visibility = View.GONE

            if (post.videoLink != null) {
                groupPlay.visibility = View.VISIBLE
            } else groupPlay.visibility = View.GONE

            if (post.saved) {
                serverGroup.visibility = View.GONE
            } else serverGroup.visibility = View.VISIBLE

            binding.serverRetry.setOnClickListener {
                onInteractionListener.retryPost(binding.content.text.toString())
            }

            //**** ДЗ Glide

            val urlAvatar = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
            avatar.loadCircle(urlAvatar)

            if (post.attachment != null) {
                post.attachment.url.let {
                    val url = "http://10.0.2.2:9999/media/${it}"
                    attachmentImage.load(url)
                }
                attachmentImage.visibility = View.VISIBLE
            } else {
                attachmentImage.visibility = View.GONE
            }

            //****

            attachmentImage.setOnClickListener {
                post.attachment?.let { it1 ->
                    onInteractionListener.showPhoto(post)
                }
            }

            likesIcon.setOnClickListener {
                onInteractionListener.like(post)
            }
            shareIcon.setOnClickListener {
                onInteractionListener.share(post)
            }
            viewIcon.setOnClickListener {
                onInteractionListener.view(post)
            }

            menu.isVisible = post.ownedByMe //меню видимо только, если пост наш

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_optons)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                onInteractionListener.remove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.edit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()           // ПОМНИТЬ прo show()!!!
            }
            play.setOnClickListener {
                onInteractionListener.play(post)
            }
            videoLink.setOnClickListener {
                onInteractionListener.play(post)
            }
            content.setOnClickListener {
                onInteractionListener.showPost(post)
            }

            linkIcon.setOnClickListener {
                groupLink.visibility = View.VISIBLE
                linkSave.setOnClickListener {
                    if (videoLinkText.text.toString().isNotBlank()) {
                        onInteractionListener.addLink(post.id, videoLinkText.text.toString())
                    } else groupLink.visibility = View.GONE
                }
            }

        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {

        if (oldItem::class != newItem::class) {
            return false
        }

        return oldItem.id == newItem.id

    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}
