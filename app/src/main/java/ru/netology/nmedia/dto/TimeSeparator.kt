//package ru.netology.nmedia.dto
//
//import java.time.Instant
//import java.time.OffsetDateTime
//import java.time.ZoneOffset
//
//class TimeSeparator (post: Post?) {
//
//    private val today: OffsetDateTime = OffsetDateTime.now()
//    private val yesterday: OffsetDateTime = today.minusDays(1)
//    private val lastWeek: OffsetDateTime = today.minusDays(2)
//    private val postTime: OffsetDateTime? = if (post != null) {
//        OffsetDateTime.ofInstant(Instant.ofEpochMilli(post.published), ZoneOffset.UTC)
//    } else null
//
//
//    private fun Post?.isLastWeek (): Boolean = lastWeek.year == postTime?.year && lastWeek.dayOfYear == postTime.dayOfYear
//    private fun Post?.isYesterday (): Boolean = yesterday.year == postTime?.year && yesterday.dayOfYear == postTime.dayOfYear
//    private fun Post?.isToday (): Boolean = today.year == postTime?.year && today.dayOfYear == postTime.dayOfYear
//
//    private fun Post?.isNotLastWeek (): Boolean = this == null || !isLastWeek()
//    private fun Post?.isNotYesterday (): Boolean = this == null || !isYesterday()
//    private fun Post?.isNotToday (): Boolean = this == null || !isToday()
//
//
//    fun separator (previousPost: Post?, nextPost: Post?): TimingSeparator? =
//        if (previousPost.isNotToday() && nextPost.isToday()) {
//        TimingSeparator (idSeparator = Separator.TODAY, name ="Today")
//    } else if (previousPost.isNotYesterday() && nextPost.isYesterday()) {
//        TimingSeparator (idSeparator = Separator.YESTERDAY, name ="Yesterday")
//    } else if (previousPost.isNotLastWeek() && nextPost.isLastWeek()) {
//        TimingSeparator(idSeparator = Separator.TWO_WEEKS_AGO, name ="Two weeks ago")
//    } else if (previousPost.isToday() && nextPost == null) {
//            TimingSeparator (idSeparator = Separator.TODAY, name ="Today")
//    } else null
//
//}