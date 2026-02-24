package com.example.quickbracket.feature.create_bracket

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateBracketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BracketRepository(application)

    val allBracketsLiveData: LiveData<List<Bracket>> = repository.allBrackets.asLiveData()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _bracketCreated = MutableLiveData<Boolean>()
    val bracketCreated: LiveData<Boolean> = _bracketCreated

    private val _generatedBracketSets = MutableLiveData<List<MatchSet>>()
    val generatedBracketSets: LiveData<List<MatchSet>> = _generatedBracketSets

    fun saveNewBracket(bracket: Bracket, playerCount: String) {
        if (bracket.name.isBlank() || bracket.type.isBlank()) {
            _statusMessage.value = "Must not leave any field blank."
            return
        } else if (playerCount.toIntOrNull() ?: 0 <= 1) {
            _statusMessage.value = "Must have at least 2 players"
            return
        } else if (playerCount.toInt() != bracket.entrants.size) {
            _statusMessage.value = "Fill all the players fields"
            return
        }

        viewModelScope.launch {
            try {
                repository.addBracket(bracket)
                _statusMessage.postValue("Bracket '${bracket.name}' created.")
                _bracketCreated.postValue(true)
            } catch (e: Exception) {
                _statusMessage.postValue("Error saving: ${e.message}")
                _bracketCreated.postValue(false)
            }
        }
    }

    fun createBracket(players: List<Player>) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Generar la estructura completa con potencias de 2
                val initialBracketSets = generateSingleEliminationBracket(players)
                // 2. Limpiar los Byes y ajustar los nombres de las letras/rondas
                val finalBracketSets = processByes(initialBracketSets)
                _generatedBracketSets.postValue(finalBracketSets)
            } catch (e: Exception) {
                Log.e("Bracket", "Error: ${e.message}")
                _statusMessage.postValue("Error: ${e.message}")
            }
        }
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
                parentSetId = null
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
                    parentSetId = null
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

    private fun updateSetParent(list: MutableList<MatchSet>, setId: Int, parentId: Int) {
        val index = list.indexOfFirst { it.setId == setId }
        if (index != -1) {
            list[index] = list[index].copy(parentSetId = parentId)
        }
    }

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
                val parentId = currentSet.parentSetId

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