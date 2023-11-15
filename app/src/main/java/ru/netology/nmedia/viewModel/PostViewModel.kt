package ru.netology.nmedia.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File


private val defaultPost = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    published = 0L,
    videoLink = null,
    hidden = true
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao()) // !!! Лучше, чтобы это было внутри конструктора PostViewModel(application: Application, ...)

    val data: LiveData<FeedModel> = repository.data
        .map(::FeedModel)  //возвращает данные в виде flow , поэтому вызываем ф-цию расширения asLiveData
        .catch { // обрабатывает исключения
            it.printStackTrace()
        }
        .asLiveData(Dispatchers.Default) //сюда передаем контекст, на котором будет работать это преобразование: Dispatchers.Default (чтобы не с главного потока),
                                         // поэтому в репозитории flowOn(Dispatchers.Default) можно опустить

    val newerCount: LiveData<Int> = data.switchMap { //т.е. это пересоздание LiveData (data) при каждом изменении исходной/списка постов/
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L) //передаем id самого последнего поста, который является id самого первого поста в списке постов
            .catch { e -> e.printStackTrace() }
            .asLiveData(Dispatchers.Default, 100) //timeout 100 м.установить, чтобы избежать ошибки, когда
                                                            //неактуальный  flow прервется не на delay, например, а позже => неактуальные данные
                                                            //запишутся и сотрут новые посты
    }


    fun updatePosts() {
        viewModelScope.launch {
            try {
                repository.updatePosts()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _postCreatedError.value = Unit
            }
        }
    }

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

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch { // зачем по сути дублировать loadPosts ??
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

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

    fun likeById(id: Long) {
        val saved = data.value?.posts?.find { it.id == id }?.saved
        val flag = data.value?.posts?.find { it.id == id }?.likedByMe
        if (saved == true) {
            if (flag != null) {
                viewModelScope.launch {
                    try {
                        repository.likeById(id, flag)
                        _dataState.value = FeedModelState()
                    } catch (e: Exception) {
                        _dataState.value = FeedModelState(error = true)
                    }
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
