package com.example.quickbracket.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Bracket(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val sets: List<MatchSet> = emptyList()
)

enum class BracketPath {
    WINNERS,
    LOSERS
}