package dev.famer.scissors.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class Classification {
    @Serializable
    @SerialName("cover")
    object Cover: Classification()

    @Serializable
    @SerialName("content")
    object Content: Classification()

    @Serializable
    @SerialName("handwrite")
    object HandWrite: Classification()
}
