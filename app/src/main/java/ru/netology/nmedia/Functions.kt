package ru.netology.nmedia

import android.widget.ImageView
import androidx.paging.PagingData
import androidx.paging.map
import com.bumptech.glide.Glide
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Separator
import ru.netology.nmedia.dto.TimingSeparator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset


fun numberRepresentation(number: Int): String {

    return when (number) {
        in 0 until 1000 -> number.toString()
        in 1000 until 1100 -> (number / 1000).toString() + "K"
        in 1100 until 10_000 -> {
            if (number % 1000 == 0) (number / 1000).toString() + "K" else
                ((number.toDouble()) / 1000).toString().take(3) + "K"
        }

        in 10_000 until 1000_000 -> (number / 1000).toString() + "K"
        in 1000_000 until 1100_000 -> (number / 1000_000).toString() + "M"
        else -> {
            if (number % 1000_000 == 0) (number / 1000_000).toString() + "M" else
                ((number.toDouble()) / 1000_000).toString().take(3) + "M"
        }
    }
}

fun ImageView.loadCircle (url: String) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_downloading_24)
        .error(R.drawable.ic_error_24)
        .timeout(10_000)
        .circleCrop()
        .into(this)
}

fun ImageView.load (url: String) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_downloading_24)
        .error(R.drawable.ic_error_24)
        .timeout(10_000)
        .into(this)
}

fun transformPagingDataToList(pagingData: PagingData<FeedItem>): List<Post> {
    val list = mutableListOf<Post>()
    pagingData.map { feedItem ->
        if (feedItem is Post) {
            list.add(feedItem)
        }
    }
    return list.toList()
}

//***** Timing Separator

class Time (post: Post?) {

    private val today: OffsetDateTime = OffsetDateTime.now()
    private val yesterday: OffsetDateTime = today.minusDays(1)
    private val lastWeek: OffsetDateTime = today.minusDays(2)
    private val postTime: OffsetDateTime? = if (post != null) {
        Instant.ofEpochMilli(post.published*1000).atOffset(ZoneOffset.UTC)
    } else null

    fun isLastWeek(): Boolean =
        lastWeek.year == postTime?.year && lastWeek.dayOfYear == postTime.dayOfYear

    fun isYesterday(): Boolean =
        yesterday.year == postTime?.year && yesterday.dayOfYear == postTime.dayOfYear

    fun isToday(): Boolean =
        today.year == postTime?.year && today.dayOfYear == postTime.dayOfYear

}


fun chooseSeparator (previous: Post?, next: Post?) : TimingSeparator? {
    val previousPost = Time (previous)
    val nextPost = Time (next)

    return if (!previousPost.isToday() && nextPost.isToday() && next != null) {
        TimingSeparator(Separator.TODAY, "Today")
    } else if (!previousPost.isYesterday() && nextPost.isYesterday()) {
        TimingSeparator (Separator.YESTERDAY, "Yesterday")
    } else if (!previousPost.isLastWeek() && nextPost.isLastWeek()) {
        TimingSeparator(Separator.TWO_WEEKS_AGO, "Two weeks ago")
    }
    else null
}