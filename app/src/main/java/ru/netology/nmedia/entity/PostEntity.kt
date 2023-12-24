package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AttachmentType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Entity (tableName = "PostEntity") //по умолчанию имя таблицы = названию класса, при желании можно изменить здесь, в кругых скобках
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likes: Int,
    val likedByMe: Boolean,
    val share: Int,
    val views: Int,
    val videoLink: String?,
    val saved: Boolean,
    val authorAvatar: String,
    val hidden: Boolean,

    @Embedded (prefix = "attachment_")
    val attachment: AttachmentEmbeddable?
//@Embedded (prefix = "attachment_") @Embedded аннотация используется для включения поля attachment прямо в таблицу PostEntity.
// prefix аргумент в аннотации @Embedded используется для предотвращения конфликтов имен столбцов в базе данных, добавляя префикс к каждому имени столбца встроенного класса.

) {
    fun toDto() = Post(id, author, authorId, content, published, likes, likedByMe, share, views, videoLink, saved, authorAvatar, hidden, attachment?.toDto())


    companion object {
        fun fromDto(dto: Post) : PostEntity {
            return PostEntity(dto.id, dto.author, dto.authorId, dto.content, dto.published, dto.likes, dto.likedByMe, dto.share, dto.views, dto.videoLink,
                dto.saved, dto.authorAvatar, dto.hidden, AttachmentEmbeddable.fromDto(dto.attachment))
        }
    }
}


data class AttachmentEmbeddable(
    val url: String,
    val description: String?,
    val type: AttachmentType,
) {
    fun toDto() = Attachment(url, description, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.description, it.type)
        }
    }
}


fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)