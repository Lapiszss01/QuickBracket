package com.example.quickbracket.feature.create_bracket

import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player

class SingleEliminationGenerator : BracketGenerator {
    override fun generate(players: List<Player>): List<MatchSet> {
        val initialSets = generateSingleEliminationBracket(players)
        return processByes(initialSets)
    }

    fun generateSingleEliminationBracket(players: List<Player>): List<MatchSet> {
        val playerCount = players.size
        if (playerCount <= 1) return emptyList()

        // Determinar tamaño de la potencia de 2
        var bracketSize = 1
        while (bracketSize < playerCount) bracketSize *= 2

        // Obtener orden de los seeds y mapear jugadores
        val seedOrder = getSeedOrder(bracketSize)
        val playersBySeed = players.associateBy { it.seed }
        val r1PlayersInOrder = seedOrder.map { seed -> playersBySeed[seed] }

        val allSets = mutableListOf<MatchSet>()
        var nextSetId = 1

        // 1. Crear la Ronda 1
        val r1Sets = mutableListOf<MatchSet>()
        for (i in 0 until bracketSize step 2) {
            r1Sets.add(MatchSet(
                setId = nextSetId++,
                roundName = "Ronda 1",
                player1 = r1PlayersInOrder[i],
                player2 = r1PlayersInOrder[i + 1],
                nextMatchId = null,
                loserNextMatchId = null
            ))
        }
        allSets.addAll(r1Sets)

        // 2. Construir rondas superiores
        var previousRoundSets = r1Sets
        while (previousRoundSets.size > 1) {
            val currentRoundSets = mutableListOf<MatchSet>()
            val numMatchesInRound = previousRoundSets.size / 2

            val roundName = when (numMatchesInRound) {
                1 -> "Final"
                2 -> "Semifinal"
                4 -> "Cuartos de Final"
                else -> "Ronda Superior"
            }

            for (i in 0 until numMatchesInRound) {
                val currentId = nextSetId++
                val newSet = MatchSet(
                    setId = currentId,
                    roundName = roundName,
                    player1 = null,
                    player2 = null,
                    nextMatchId = null,
                    loserNextMatchId = null
                )

                // Enlazar hijos a este padre
                val child1 = previousRoundSets[i * 2]
                val child2 = previousRoundSets[i * 2 + 1]

                // Actualizar las instancias en la lista principal con el parentSetId
                updateSetParent(allSets, child1.setId, currentId)
                updateSetParent(allSets, child2.setId, currentId)

                currentRoundSets.add(newSet)
            }
            allSets.addAll(currentRoundSets)
            previousRoundSets = currentRoundSets
        }

        return allSets
    }

    private fun getSeedOrder(size: Int): List<Int> {
        var seeds = mutableListOf(1)
        while (seeds.size < size) {
            val nextSeeds = mutableListOf<Int>()
            val currentSum = seeds.size * 2 + 1
            for (seed in seeds) {
                nextSeeds.add(seed)
                nextSeeds.add(currentSum - seed)
            }
            seeds = nextSeeds
        }
        return seeds
    }

    private fun updateSetParent(list: MutableList<MatchSet>, setId: Int, parentId: Int) {
        val index = list.indexOfFirst { it.setId == setId }
        if (index != -1) {
            list[index] = list[index].copy(nextMatchId = parentId)
        }
    }

    ///////

    fun processByes(generatedSets: List<MatchSet>): List<MatchSet> {
        val setMap = generatedSets.associateBy { it.setId }.toMutableMap()
        val setsToRemoveIds = mutableSetOf<Int>()

        // Ordenamos por ID para procesar desde la R1 hacia arriba
        val sortedSets = generatedSets.sortedBy { it.setId }

        for (currentSet in sortedSets) {
            // Un set es un Bye si solo tiene un jugador
            val p1 = currentSet.player1
            val p2 = currentSet.player2

            val isBye = (p1 != null && p2 == null) || (p1 == null && p2 != null)

            if (isBye) {
                val advancingPlayer = p1 ?: p2
                val parentId = currentSet.nextMatchId

                if (parentId != null && advancingPlayer != null) {
                    val parentSet = setMap[parentId]
                    if (parentSet != null) {
                        // Subir al jugador al set padre
                        val updatedParent = if (parentSet.player1 == null) {
                            parentSet.copy(player1 = advancingPlayer)
                        } else {
                            parentSet.copy(player2 = advancingPlayer)
                        }
                        setMap[parentId] = updatedParent
                        setsToRemoveIds.add(currentSet.setId)
                    }
                }
            }
        }

        // Filtrar y re-etiquetar (A, B, C...)
        return setMap.values
            .filter { it.setId !in setsToRemoveIds }
            .sortedBy { it.setId }
            .mapIndexed { index, matchSet ->
                matchSet.copy(setLetter = ('A' + index).toString())
            }
    }

}