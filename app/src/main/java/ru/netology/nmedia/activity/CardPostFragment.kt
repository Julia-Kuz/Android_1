package ru.netology.nmedia.activity

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.filter
import androidx.paging.map
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentCardPostBinding
//import ru.netology.nmedia.dependencyInjection.DependencyContainer
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.load
import ru.netology.nmedia.loadCircle
import ru.netology.nmedia.numberRepresentation
import ru.netology.nmedia.transformPagingDataToList
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.Constants
import ru.netology.nmedia.util.PostDealtWith
import ru.netology.nmedia.viewModel.PostViewModel
//import ru.netology.nmedia.viewModel.ViewModelFactory
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CardPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCardPostBinding.inflate(
            inflater,
            container,
            false
        )

        val post = PostDealtWith.get()

        binding.content.movementMethod = ScrollingMovementMethod()

        fun fill(post: Post) {
            with(binding) {

                author.text = post.author
                published.text = SimpleDateFormat("dd MMM yyyy в HH:mm", Locale.getDefault()).format(Date((post.published * 1000)))
                //post.published нужно умножить на 1000, так как Date() ожидает время в миллисекундах.
                content.text = post.content
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

                //**** ДЗ Glide

                val url = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
                avatar.loadCircle(url)

                if (post.attachment != null) {
                    post.attachment.url.let {
                        val url = "http://10.0.2.2:9999/media/${it}"
                        attachmentImage.load (url)
                    }
                    attachmentImage.visibility = View.VISIBLE
                } else {
                    attachmentImage.visibility = View.GONE
                }

                //****

                attachmentImage.setOnClickListener {
                    post.attachment?.let {
                        PostDealtWith.savePostDealtWith(post)
                        findNavController().navigate(R.id.action_cardPostFragment_to_photoFragment2)
                    }
                }


                likesIcon.setOnClickListener {
                    viewModel.likeById(post)
                }

                shareIcon.setOnClickListener {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.content)
                        type = "text/plain"
                    }
                    val shareIntent =
                        Intent.createChooser(
                            intent,
                            getString(R.string.chooser_share_post)
                        ) //создается chooser - выбор между приложениями
                    startActivity(shareIntent)

                    viewModel.shareById(post.id)
                }

                viewIcon.setOnClickListener {
                    viewModel.viewById(post.id)
                }

                menu.isVisible = post.ownedByMe //меню видимо только, если пост наш

                menu.setOnClickListener {

                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.menu_optons)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.remove -> {
                                    viewModel.removeById(post.id)
                                    findNavController().navigateUp()
                                    true
                                }

                                R.id.edit -> {
                                    PostDealtWith.savePostDealtWith(post)
                                    findNavController().navigate(R.id.action_cardPostFragment_to_editPostFragment)
                                    true
                                }

                                else -> false
                            }
                        }
                    }.show()           // ПОМНИТЬ прo show()!!!

                }

                play.setOnClickListener {
                    playMedia(Uri.parse(post.videoLink))
                }
                videoLink.setOnClickListener {
                    playMedia(Uri.parse(post.videoLink))
                }

                linkIcon.setOnClickListener {
                    groupLink.visibility = View.VISIBLE
                    linkSave.setOnClickListener {
                        if (videoLinkText.text.toString().isNotBlank()) {
                            viewModel.addLink(post.id, videoLinkText.text.toString())
                        } else groupLink.visibility = View.GONE
                    }
                }

                binding.serverRetry.setOnClickListener {
                    viewModel.changeContentAndSave(binding.content.text.toString())
                }
            }
        }

        fill(post)

//        viewModel.data.observe(viewLifecycleOwner) {feedModel ->
//            feedModel.posts.find { it.id == post.id }?.let { fill(it.copy()) }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest { pagingDataPost ->
                    transformPagingDataToList(pagingDataPost).find { it.id == post.id }?.let { fill(it.copy()) }
                }
            }
        }

        return binding.root
    }


    private fun playMedia(file: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = file
        }
        val packageManager = requireActivity().packageManager
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

}