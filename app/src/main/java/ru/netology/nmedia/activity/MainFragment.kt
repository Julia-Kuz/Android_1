package ru.netology.nmedia.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.PostDealtWith
import ru.netology.nmedia.viewModel.PostViewModel

class MainFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment   //предоставляем viemodel нескольким фрагментам
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMainBinding.inflate(
            inflater,
            container,
            false
        )

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun like(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun share(post: Post) {
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

                //viewModel.shareById(post.id)
            }

            override fun view(post: Post) {
                viewModel.viewById(post.id)
            }

            override fun remove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun edit(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_mainFragment_to_editPostFragment)
            }

            override fun play(post: Post) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.videoLink)) // в ДЗ
                startActivity(intent)

//                val intent =
//                    Intent(Intent.ACTION_VIEW).apply {             //на сайте https://developer.android.com/guide/components/intents-common#Music
//                        data = Uri.parse(post.videoLink)
//                    }
//                if (intent.resolveActivity(packageManager) != null) {          //c фрагментами горит красным
//                    startActivity(intent)
//                }
            }

            override fun showPost(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_mainFragment_to_cardPostFragment)
            }
        }
        )

        binding.recyclerList.adapter = adapter  // получаю доступ к RecyclerView


        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val newPost =
                posts.size > adapter.currentList.size //проверяем, что это добавление поста, а не др действие (лайк и т.п.)

            adapter.submitList(posts) {
                if (newPost) {
                    binding.recyclerList.smoothScrollToPosition(0)
                } // scroll к верхнему сообщению только при добавлении
            }
        }

        binding.addPost.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_newPostFragment)
        }


        return binding.root
    }
}