package ru.netology.nmedia.dto

import ru.netology.nmedia.util.AttachmentType
import java.time.OffsetDateTime
import java.time.OffsetTime

sealed interface FeedItem{
    val id: Any
}

data class Ad(
    override val id: Long,
    //val url: String,
    val image: String,
) : FeedItem

data class TimingSeparator (
    override val id : Separator,
    val name: String
): FeedItem

enum class Separator {
    TODAY,
    YESTERDAY,
    TWO_WEEKS_AGO
}

data class Post(
    override val id: Long,
    val author: String,
    val authorId: Long,
    val content: String,
    val published: Long, //OffsetDateTime
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val share: Int = 0,
    val views: Int = 0,
    val videoLink: String? = null,
    val saved: Boolean = false,
    val authorAvatar: String = "",
    val hidden: Boolean,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false
) : FeedItem

data class Attachment(
    val url: String = "",
    val description: String? = null,
    val type: AttachmentType
)