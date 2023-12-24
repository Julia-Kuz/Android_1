package ru.netology.nmedia.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.flatMap
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.transformPagingDataToList
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject


private val defaultPost = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    published = 0L,
    videoLink = null,
    hidden = true,
    authorId = 0
)

@HiltViewModel
class PostViewModel @Inject constructor (
    private val repository: PostRepository,
    appAuth: AppAuth
) : ViewModel() {

    //для авторизации: меняем принцип формирования data - нужно flow из БД объединить с flow авторизации:

//    val data: LiveData<FeedModel> = appAuth
//        .authStateFlow
//        .flatMapLatest { auth -> //эта лямбда вызывается каждый раз, как меняется значение авторизации authStateFlow
//            repository.data.map { posts ->
//                    FeedModel(
//                        posts.map { it.copy(ownedByMe = it.authorId == auth.id) }, //наш пост, если id текущего авторизованного пользователя равно id автора текущего поста
//                        posts.isEmpty()
//                    )
//                }
//        }.asLiveData(Dispatchers.Default)

    //пагинация
    val data: Flow<PagingData<FeedItem>> = appAuth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.data.map { feedItemPagingData ->
                feedItemPagingData.map { feedItem ->
                    if (feedItem is Post) {
                        feedItem.copy(ownedByMe = feedItem.authorId == myId)
                    } else {
                        feedItem
                    }
                }
            }
        }.flowOn(Dispatchers.Default)

    val newerCount = repository.getNewerCount()


//    val newerCount: LiveData<Int> = data.switchMap { //т.е. это пересоздание LiveData (data) при каждом изменении исходной/списка постов/
//        //repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L) //передаем id самого последнего поста, который является id самого первого поста в списке постов
//        repository.getNewerCount(it.firstOrNull()?.id ?: 0L)
//        .catch { e -> e.printStackTrace() }
//            .asLiveData(Dispatchers.Default, 100) //timeout 100 м.установить, чтобы избежать ошибки, когда
//                                                            //неактуальный  flow прервется не на delay, например, а позже => неактуальные данные
//                                                            //запишутся и сотрут новые посты
//    }


//    fun updatePosts() {
//        viewModelScope.launch {
//            try {
//                repository.updatePosts()
//                _dataState.value = FeedModelState()
//            } catch (e: Exception) {
//                _postCreatedError.value = Unit
//            }
//        }
//    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    private val _postCreatedError = SingleLiveEvent<Unit>()
    val postCreatedError: LiveData<Unit> = _postCreatedError

    val edited = MutableLiveData(defaultPost)
    var link = null
    // var draft: String = ""   // для черновика

//    init {
//        loadPosts()
//    }
//
//    fun loadPosts() = viewModelScope.launch {
//        try {
//            _dataState.value = FeedModelState(loading = true)
//            //repository.getAll()
//            _dataState.value = FeedModelState()
//        } catch (e: Exception) {
//            _dataState.value = FeedModelState(error = true)
//        }
//    }

//    fun refreshPosts() = viewModelScope.launch { // зачем по сути дублировать loadPosts ??
//        try {
//            _dataState.value = FeedModelState(refreshing = true)
//            repository.getAll()
//            _dataState.value = FeedModelState()
//        } catch (e: Exception) {
//            _dataState.value = FeedModelState(error = true)
//        }
//    }

    fun changeContentAndSave(content: String) {
        edited.value?.let { itPost ->
            val text = content.trim()
            if (text != itPost.content) {

                viewModelScope.launch {
                    try {
                        val photoModel = _photo.value //читаем из _photo значение, 2 часть сохранения
                        if (photoModel == null) {
                            repository.save(itPost.copy(content = text))
                        } else {
                            repository.saveWithAttachment (itPost.copy(content = text), photoModel)
                        }
                        _dataState.value = FeedModelState(loading = true)
                        _dataState.value = FeedModelState()
                        _postCreated.value = Unit
                    } catch (e: Exception) {
                        _dataState.value = FeedModelState(error = true)
                    }
                }
            }
        }
        edited.value = defaultPost
    }

    fun edit(post: Post) {

        viewModelScope.launch {
            try {
                repository.save(post.copy(saved = false))
                _postCreated.value = Unit
            } catch (e: Exception) {
                _postCreatedError.value = Unit
            }
        }

    }


    fun likeById(post: Post) {
//        val saved = data.value?.posts?.find { it.id == id }?.saved
//        val flag = data.value?.posts?.find { it.id == id }?.likedByMe

        if (post.saved) {
            viewModelScope.launch {
                try {
                    repository.likeById(post.id, post.likedByMe)
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
    }


    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }


    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?>
        get() = _photo

    fun setPhoto(uri: Uri, file: File) {    //это первая часть сохранения, 2 часть - в функции changeContentAndSave (читаем инфо из значения photo)
        _photo.value = PhotoModel(uri, file)
    }

    fun clearPhoto() {
        _photo.value = null
    }




    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)
    fun addLink(id: Long, link: String) = repository.addLink(id, link)
}
