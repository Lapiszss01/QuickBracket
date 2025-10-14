package com.example.quickbracket.model

import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Serializable
data class Bracket(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    var sets: List<MatchSet> = emptyList(),
    var entrants: List<Player> = emptyList()
) : Parcelable

enum class BracketPath {
    WINNERS, LOSERS, FINAL
}