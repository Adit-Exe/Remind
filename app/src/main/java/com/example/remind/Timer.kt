package com.example.remind

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Timer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endDateTime: Long,
    val startDateTime: Long,
    val isPinned: Boolean = false,
    val notifiedAt50: Boolean = false,
    val notifiedAt90: Boolean = false,
    val notifiedAt100: Boolean = false
)
