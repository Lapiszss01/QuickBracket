package com.example.quickbracket.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class MatchSet(
    val setId: Int,
    val nextMatchId: Int?,
    val loserNextMatchId: Int?,

    val setLetter: String? = "",
    var roundName: String,

    var player1: Player? = null,
    var player2: Player? = null,
    var status: String? = "",

    //val path: BracketPath = BracketPath.WINNERS,
    var isFinished: Boolean? = false,
    var winner: Player? = null
) : Parcelable