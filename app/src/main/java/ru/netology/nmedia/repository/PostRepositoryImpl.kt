package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryImpl : PostRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val postsType = object : TypeToken<List<Post>>() {}.type //нужно для парсинга

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999/api/slow"
        private val jsonType = "application/json".toMediaType()
    }

    override fun getAllAsync(callback: PostRepository.GetMyCallback <List<Post>>) {
        val request = Request.Builder()
            .url("${BASE_URL}/posts")
            .build()      // создали запрос

        client.newCall(request)
            //при импорте интерфейса Callback нужно выбирать библиотеку okhttp3
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {  //ошибка соединения с сервером
                    callback.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseString = response.body?.string()
                    try {
                        callback.onSuccess(gson.fromJson(responseString, postsType))
                    } catch (e: Exception) {   // ответ сервера с ошибкой, вернул не то, что нам нужно
                        callback.onError(e)
                    }
                }
            })
    }

    override fun likeByIdAsync(
        id: Long,
        flag: Boolean,
        callback: PostRepository.GetMyCallback<Post>
    ) {
        if (!flag) {
            val request = Request.Builder()
                .url("${BASE_URL}/posts/${id}/likes")
                .post(gson.toJson("${BASE_URL}/posts/${id}/likes").toRequestBody(jsonType))
                .build()

            return client.newCall(request)
                .enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseString = response.body?.string()
                        try {
                            callback.onSuccess(gson.fromJson(responseString, Post::class.java))
                        } catch (e: Exception) {
                            callback.onError(e)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }
                })
        } else {
            val request = Request.Builder()
                .url("${BASE_URL}/posts/${id}/likes")
                .delete(gson.toJson("${BASE_URL}/posts/${id}/likes").toRequestBody(jsonType))
                .build()

            return client.newCall(request)
                .enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseString = response.body?.string()
                        try {
                            callback.onSuccess(gson.fromJson(responseString, Post::class.java))
                        } catch (e: Exception) {
                            callback.onError(e)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }
                })
        }
    }

    override fun saveAsync(post: Post, callback: PostRepository.GetMyCallback <Post>) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseString = response.body?.string()
                    try {
                        callback.onSuccess(gson.fromJson(responseString, Post::class.java))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }
            })
    }

    override fun removeByIdAsync(id: Long, callback: PostRepository.GetMyCallback<Boolean>) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/posts/$id")
            .delete()
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {  //ошибка соединения с сервером
                    callback.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback.onSuccess(true)
                }
            })
    }


    //**************

    override fun getAll(): List<Post> {
        val request = Request.Builder()
            .url("${BASE_URL}/posts")
            .build()      // создали запрос

        val call = client.newCall(request) // создали сетевой вызов
        val response = call.execute() //вызов запустили, получили ответ
        val responseString = response.body?.string() ?: error("Body is null") //ответ в виде строки
        return gson.fromJson(
            responseString,
            postsType
        ) // ответ парсим к нужному типу с помощью gson
    }

    override fun likeById(id: Long, flag: Boolean) : Post {
        if (!flag) {
            val request = Request.Builder()
                .url("${BASE_URL}/posts/${id}/likes")
                .post(gson.toJson("${BASE_URL}/posts/${id}/likes").toRequestBody(jsonType))
                .build()

            return client.newCall(request)
                .execute()
                .let { it.body?.string() }
                .let {
                    gson.fromJson(it, Post::class.java)
                }
        } else {
            val request = Request.Builder()
                .url("${BASE_URL}/posts/${id}/likes")
                .delete(gson.toJson("${BASE_URL}/posts/${id}/likes").toRequestBody(jsonType))
                .build()

            return client.newCall(request)
                .execute()
                .let { it.body?.string() }
                .let {
                    gson.fromJson(it, Post::class.java)
                }
        }
    }

    override fun removeById(id: Long) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/posts/$id")
            .delete()
            .build()

        client.newCall(request)
            .execute()
            .close()
    }

    override fun save(post: Post) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/posts")
            .build()

        client.newCall(request)
            .execute()
            .close()
    }

    override fun shareById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun viewById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun addLink(id: Long, link: String) {
        TODO("Not yet implemented")
    }
}