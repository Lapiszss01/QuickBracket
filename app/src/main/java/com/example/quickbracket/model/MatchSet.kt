package com.example.quickbracket.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class MatchSet(
    val setId: Int,
    var roundName: String,
    //val path: BracketPath = BracketPath.WINNERS,
    val parentSetId: Int?,
    var player1: Player? = null,
    var player2: Player? = null,
    var isFinished: Boolean? = false,
    var winner: Player? = null
) : Parcelable