package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.Response
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: PostsApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostEntity>): MediatorResult {

        try {

            val response = when (loadType) {

                LoadType.REFRESH -> service.getLatest(state.config.pageSize)

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true) // Автоматический PREPEND был отключен, т. е. при scroll к первому сверху элементу данные автоматически не подгружались.
//                    val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(endOfPaginationReached = false)
//                    service.getAfter(id, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    service.getBefore(id, state.config.pageSize)
                }

            }


            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val bodyResponse = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )

            if (bodyResponse.isEmpty()) return MediatorResult.Success(endOfPaginationReached = true)
            if ( bodyResponse.isNotEmpty())
            {
                appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {

                        postRemoteKeyDao.removeAll()
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    id = bodyResponse.first().id,
                                ),
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    id = bodyResponse.last().id,
                                )
                            )
                        )
                        postDao.removeAll()
                    }


                    LoadType.PREPEND -> Unit

//                    {
//                        postRemoteKeyDao.insert(
//                            PostRemoteKeyEntity(
//                                type = PostRemoteKeyEntity.KeyType.AFTER,
//                                id = bodyResponse.first().id,
//                            )
//                        )
//                    }

                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                id = bodyResponse.last().id,
                            )
                        )
                    }

                }

                postDao.insert(bodyResponse.map { it.copy(saved = true) }.toEntity())

                }
            }

            return MediatorResult.Success(endOfPaginationReached = bodyResponse.isEmpty())

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}