package com.example.quickbracket.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class MatchSet(
    val setId: Int,
    var roundName: String,
    val path: BracketPath = BracketPath.WINNERS,
    val parentSetId: Int?,
    val player1: Player? = null,
    val player2: Player? = null
) : Parcelable