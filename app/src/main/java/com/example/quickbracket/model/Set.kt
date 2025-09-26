package com.example.quickbracket.model

import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
data class Set(
    val id: String = UUID.randomUUID().toString(),
)