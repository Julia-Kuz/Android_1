package ru.netology.nmedia.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity

@Database(entities = [PostEntity::class], version = 1)
//если несколько таблиц в БД, то передаем через запятую: @Database(entities = [PostEntity::class, Table2::class, и т.д.], version = 1)

abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao          //используем в Viewmodel: private val repository: PostRepository = PostRepositoryRoomImpl(AppDb.getInstance(context = application).postDao())

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDb::class.java, "app.db")
                .fallbackToDestructiveMigration()
                //.allowMainThreadQueries() //с корутинами не нужно
                .build()
    }
}