package com.example.quickbracket.model

import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
data class Player(
    //val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isEliminated: Boolean = false
)