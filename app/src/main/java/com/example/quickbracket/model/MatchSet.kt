package com.example.quickbracket.model

import kotlinx.serialization.Serializable


@Serializable
data class MatchSet(
    val player1: Player? = null,
    val player2: Player? = null,
    val winner: Player? = null,
    val parentSetId: Int? = null,
    val loserDropSetId: Int? = null,
    val setId: Int,
    val roundName: String,
    val path: BracketPath
)