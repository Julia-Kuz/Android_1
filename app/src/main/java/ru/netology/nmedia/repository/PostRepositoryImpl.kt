package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
//import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.Response
//import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import java.io.IOException
import retrofit2.Response
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.chooseSeparator
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.util.AttachmentType
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val postApiService: PostsApiService,
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb
) : PostRepository {

//    override val data = dao.getAll()
//        .map(List<PostEntity>::toDto) //map PostEntity into Posts //после изменения на flow - import kotlinx.coroutines.flow.map
//       // .flowOn(Dispatchers.Default) //можно опустить, поскольку во viewmodel укажем контекст, на каком потоке работать


//    //пагинация только сервер
//    override val data: Flow<PagingData<Post>> = Pager(
//        config = PagingConfig(pageSize = 5, enablePlaceholders = false),
//        pagingSourceFactory = { PostPagingSource(postApiService) },
//    ).flow

    //пагинация только БД
//    override val data: Flow<PagingData<Post>> = Pager(
//        config = PagingConfig(pageSize = 5, enablePlaceholders = false),
//        pagingSourceFactory = { dao.getPagingSource()},
//    ).flow.map { pagingData -> pagingData.map(PostEntity::toDto) }

    //пагинация БД и сервер
    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(pageSize = 8, enablePlaceholders = true),
        pagingSourceFactory = {dao.getPagingSource()},
        remoteMediator = PostRemoteMediator(postApiService, dao, postRemoteKeyDao = postRemoteKeyDao, appDb = appDb)
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
            .insertSeparators { previousItem, nextItem ->
                if (previousItem?.id?.rem(5) == 0L) {
                    Ad(Random.nextLong(), "figma.jpg")
                } else {
                   chooseSeparator(previousItem, nextItem)
                }
                //else null
            }
    }

//    override suspend fun getAll() {
//        dao.getAll()
//        try {
//            val response =
//                postApiService.getAll() //получаем посты из сети // при внедрении зависимостей, передаем в конструктор, здесь используем переменную
//            if (!response.isSuccessful) {
//                throw ApiError(response.code(), response.message())
//            }
//
//            val body =
//                response.body() ?: throw ApiError(response.code(), response.message()) //тело ответа
//
//            val newBody = body.map {
//                it.copy(saved = true)
//            }
//
//            dao.insert(newBody.toEntity()) // записывает ответ в базу данных
//
//        } catch (e: IOException) {
//            throw NetworkError
//        } catch (e: Exception) {
//            throw UnknownError
//        }
//    }

    override fun getNewerCount(): Flow<Long> = dao.max()
        .flatMapLatest {
            if (it != null) {
                flow {           //импорт из пакета корутин
                    while (true) {                 // м.сделать бесконечный цикл, т.к. flatMapLatest неактуальные flow отменяет
                        delay(10_000L)
                        val response = postApiService.getNewer(it)
                        val body = response.body()
                        emit(body?.count ?: 0 )
                    }
                }
            } else emptyFlow()
        }
        .catch { e -> e.printStackTrace() } // e -> throw AppError.from(e) - роняет приложение
        .flowOn(Dispatchers.Default)


    override suspend fun updatePosts() {
        val newPosts = dao.getNewPosts()
        val updatedPosts = newPosts.map {
            it.copy(hidden = false)
        }
        dao.insert(updatedPosts)
    }

    override suspend fun save(post: Post) {

        dao.insert(PostEntity.fromDto(post))

        try {
            val response = postApiService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val result = response.body() ?: throw ApiError(response.code(), response.message())

            dao.updatePost(PostEntity.fromDto(result))

            //dao.insert(PostEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, photoModel: PhotoModel) {
        dao.insert(PostEntity.fromDto(post))

        try {
            val mediaResponse = saveMedia(photoModel.file)   //отправили изображение на сервер

            if (!mediaResponse.isSuccessful) {
                throw ApiError(mediaResponse.code(), mediaResponse.message())
            }

            val media = mediaResponse.body() ?: throw ApiError(
                mediaResponse.code(),
                mediaResponse.message()
            ) //получили результат

            val response = postApiService.save(
                post.copy(
                    attachment = Attachment(
                        media.id,
                        "",
                        AttachmentType.IMAGE
                    )
                )
            ) //добавили копию поста, записали в него attachment

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val result = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )   //получили ответ

            dao.updatePost(PostEntity.fromDto(result))  //записываем в базу

            //dao.insert(PostEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    //функция по сохранению media на сервер
    private suspend fun saveMedia(file: File): Response<Media> {
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody()
        ) //file.asRequestBody() - данные, кот отправятся на сервер
        return postApiService.saveMedia(part)
    }

    override suspend fun likeById(id: Long, flag: Boolean) {
        dao.likeById(id)

        try {
            val response = if (!flag) {
                postApiService.likeById(id)
            } else {
                postApiService.dislikeById(id)
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
//                dao.likeById(body.id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        dao.removeById(id)
        try {
            val response = postApiService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            response.body() ?: throw ApiError(response.code(), response.message())
//            dao.removeById(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
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

    override fun getAllAsync(callback: PostRepository.GetMyCallback<List<Post>>) {
        TODO("Not yet implemented")
    }

    override fun likeByIdAsync(
        id: Long,
        flag: Boolean,
        callback: PostRepository.GetMyCallback<Post>
    ) {
        TODO("Not yet implemented")
    }

    override fun removeByIdAsync(id: Long, callback: PostRepository.GetMyCallback<Unit>) {
        TODO("Not yet implemented")
    }

    override fun saveAsync(post: Post, callback: PostRepository.GetMyCallback<Post>) {
        TODO("Not yet implemented")
    }
}