package ru.netology.nmedia.activity

import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingSource
import androidx.paging.flatMap
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PagingLoadStateAdapter
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentMainBinding
import ru.netology.nmedia.dto.FeedItem
//import ru.netology.nmedia.dependencyInjection.DependencyContainer
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.Constants
import ru.netology.nmedia.util.PostDealtWith
import ru.netology.nmedia.viewModel.AuthViewModel
import ru.netology.nmedia.viewModel.PostViewModel
import ru.netology.nmedia.viewModel.SignInViewModel
//import ru.netology.nmedia.viewModel.ViewModelFactory

@AndroidEntryPoint
class MainFragment : Fragment() {

//  теперь пишем (см ниже). Так происходит потому что Hilt с определённой версии перестал поддерживать ownerProducer

    private val viewModel: PostViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()

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
                if (viewModelAuth.authenticated) {
                    viewModel.likeById(post)
                } else {
                    findNavController().navigate(R.id.action_mainFragment_to_dialogFragment)
                }
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

                viewModel.shareById(post.id)
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.videoLink))
                val packageManager = requireActivity().packageManager
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }

            override fun showPost(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_mainFragment_to_cardPostFragment)
            }

            override fun showPhoto(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_mainFragment_to_photoFragment2)
            }
            override fun addLink(id: Long, link: String) {
                viewModel.addLink(id, link)
            }

            override fun retryPost(content: String) {
                viewModel.changeContentAndSave(content)
            }
        }
        )

        binding.newPosts.visibility = View.GONE

        // *****************         реализация меню для авторизации с ToolBar   *****************

        val toolbar: Toolbar = binding.toolbarMain
        toolbar.overflowIcon?.colorFilter = BlendModeColorFilter(Color.WHITE, BlendMode.SRC_ATOP) // чтобы три точки стали белыми - (!) Color д. использоваться из android.graphics, а не из androidx.compose.ui.graphics

        // с использованием Live data
//        viewModelAuth.dataAuth.observe(viewLifecycleOwner) {
//            toolbar.invalidateMenu()
//        }

        // с использованием Flow
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){ //наблюдаем за авторизацией, только когда активити/фрагмент доступен для взаимодействия
                viewModelAuth.dataAuth.collect{
                    toolbar.invalidateMenu()
                }
            }
        }

        toolbar.addMenuProvider (object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)

                menu.let {
                    it.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
                    it.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
                }
            }

//            override fun onPrepareMenu(menu: Menu) {
//                menu.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
//                menu.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
//            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.signin -> {
                        findNavController().navigate(R.id.action_mainFragment_to_signInFragment)
                        true
                    }

                    R.id.signup -> {
                        findNavController().navigate(R.id.action_mainFragment_to_signUpFragment)
                        true
                    }

                    R.id.signout -> {
                        findNavController().navigate(R.id.action_mainFragment_to_dialogFragmentSignOut)
                        true
                    }

                    else -> false
                }
        })

        // получаю доступ к RecyclerView
        //binding.recyclerList.setHasFixedSize(true)
        binding.recyclerList.adapter = adapter.withLoadStateHeaderAndFooter(
                header = PagingLoadStateAdapter(object : PagingLoadStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
                footer = PagingLoadStateAdapter(object : PagingLoadStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
        )


        //пагинация
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest{
                    adapter.submitData(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    binding.swiperefresh.isRefreshing = state.refresh is LoadState.Loading
//                        state.refresh is LoadState.Loading ||
//                                state.prepend is LoadState.Loading ||
//                                state.append is LoadState.Loading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {


                adapter.loadStateFlow.collectLatest {

//                    adapter.loadStateFlow
//                        .distinctUntilChangedBy { r -> r.source.refresh } // Only emit when REFRESH LoadState for the paging source changes.
//                        .map { it.source.refresh is LoadState.NotLoading } // Only react to cases where REFRESH completes i.e., NotLoading.
//                        .collectLatest {binding.recyclerList.scrollToPosition(0) }


                    if (it.refresh.endOfPaginationReached) {
                        binding.recyclerList.scrollToPosition (0)
                    }
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener{
            adapter.refresh()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.newerCount.collect {
                    if (it > 0) {binding.newPosts.text = "New Posts"}
                    binding.newPosts.isVisible = it > 0
                }
            }
        }

        binding.newPosts.setOnClickListener {
            adapter.refresh()
            binding.newPosts.visibility = View.GONE
        }

        viewModel.dataState.observe(viewLifecycleOwner) { feedModelState ->
            binding.progress.isVisible = feedModelState.loading
            binding.errorGroup.isVisible = feedModelState.error
        }

        binding.retryButton.setOnClickListener {
            //viewModel.loadPosts()
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.data.collectLatest {
                    adapter.submitData(it)
                }
            }
        }

        binding.addPost.setOnClickListener {
            if (viewModelAuth.authenticated) {
                findNavController().navigate(
                    R.id.action_mainFragment_to_newPostFragment,
                    // Bundle().apply { textArg = viewModel.draft }             // черновик с помощью VieModel
                    Bundle().apply {
                        val text = (activity as AppActivity).getSharedPreferences(
                            Constants.DRAFT_PREF_NAME,
                            Context.MODE_PRIVATE
                        ).getString(Constants.DRAFT_KEY, "")
                        textArg = text
                    }
                )
            } else {
                findNavController().navigate(R.id.action_mainFragment_to_dialogFragment)
            }

        }

        return binding.root
    }
}