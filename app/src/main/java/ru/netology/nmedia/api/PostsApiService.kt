package ru.netology.nmedia.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.NewerCount
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.PushToken

// 1. описываем интерфейс для доступа к нашему серверу, в котором перечисляем все методы

interface PostsApiService {
    @GET ("posts")
    suspend fun getAll () : Response <List <Post>>   // импортировать нужно из retrofit2

    @GET ("posts/latest")
    suspend fun getLatest (@Query("count") count: Int) : Response <List <Post>>

    @GET ("posts/{id}/before")
    suspend fun getBefore (@Path("id") id: Long, @Query("count") count: Int): Response <List <Post>>

    @GET ("posts/{id}/after")
    suspend fun getAfter (@Path("id") id: Long, @Query("count") count: Int): Response <List <Post>>

    @GET("posts/{id}/newer-count")
    suspend fun getNewer(@Path("id") id: Long): Response<NewerCount>

    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response <Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Long): Response <Post>

    @POST("posts")
    suspend fun save(@Body post: Post): Response<Post>

    @DELETE("posts/{id}")
    suspend fun removeById(@Path("id") id: Long): Response <Unit>

    @Multipart
    @POST("media")
    suspend fun saveMedia(@Part media: MultipartBody.Part): Response<Media>

    @POST("users/push-tokens")
    suspend fun sendPushToken (@Body pushToken: PushToken): Response<Unit>

}

