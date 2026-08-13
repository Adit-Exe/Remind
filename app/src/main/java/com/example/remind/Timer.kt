package com.example.remind

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
sealed class NotificationSetting {
    abstract val id: String

    @Serializable
    @SerialName("Percentage")
    data class PercentageSetting(
        override val id: String = UUID.randomUUID().toString(),
        val percentage: Int
    ) : NotificationSetting()

    @Serializable
    @SerialName("Time")
    data class TimeSetting(
        override val id: String = UUID.randomUUID().toString(),
        val type: TimeType,
        val value: Int,
        val unit: TimeUnitType
    ) : NotificationSetting()
}

@Serializable
enum class TimeType {
    BEFORE, AFTER
}

@Serializable
enum class TimeUnitType {
    MINUTES, DAYS, MONTHS
}

@Serializable
data class Timer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endDateTime: Long,
    val startDateTime: Long,
    val isPinned: Boolean = false
)

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val isForceUpdate: Boolean
)
