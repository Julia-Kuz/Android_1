package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.PhotoModel

interface PostRepository {

    //val data: Flow<List<Post>> //Добавим свойство, которое будет отвечать за предоставление данных в виде Flow (с корректным импортом!)

    val data: Flow<PagingData<FeedItem>>  //пагинация

    fun getNewerCount(): Flow<Long>

    //suspend fun getAll()
    suspend fun updatePosts()
    suspend fun save(post: Post)
    suspend fun likeById(id: Long, flag: Boolean)
    suspend fun removeById(id: Long)
    suspend fun saveWithAttachment(post: Post, photoModel: PhotoModel)

    fun shareById(id: Long)
    fun viewById(id: Long)
    fun addLink(id: Long, link: String)

    fun getAllAsync(callback: GetMyCallback<List<Post>>)
    fun likeByIdAsync(id: Long, flag: Boolean, callback: GetMyCallback<Post>)
    fun removeByIdAsync(id: Long, callback: GetMyCallback<Unit>)
    fun saveAsync(post: Post, callback: GetMyCallback<Post>)

    interface GetMyCallback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Exception) {}
    }

//пример до generic - передавался в  getAllAsync: fun getAllAsync(callback: GetAllCallback)
//    interface  {
//        fun onSuccess(posts: List<Post>) //{}
//        fun onError(e: Exception) //{}
//    }

}