package com.example.quickbracket.feature.create_bracket

import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player

interface BracketGenerator {
    fun generate(players: List<Player>): List<MatchSet>
}