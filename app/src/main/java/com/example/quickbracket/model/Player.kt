package com.example.quickbracket.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
@Parcelize
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isEliminated: Boolean = false,
    val seed: Int = 0
) : Parcelable