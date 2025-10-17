package com.example.quickbracket.feature.create_bracket

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
        if (bracket.name.isBlank() || bracket.type.isBlank() || playerCount.toInt() != bracket.entrants.size) {
            _statusMessage.value = "Must not leave any field blank."
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
                val initialBracketSets = generateSingleEliminationBracket(players)
                val finalBracketSets = processByes(initialBracketSets)
                _generatedBracketSets.postValue(finalBracketSets)
            } catch (e: IllegalArgumentException) {
                Log.e("Bracket", "Error de límite o lógica: ${e.message}")
                _statusMessage.postValue("Error en la creación del bracket: ${e.message}")
            } catch (e: OutOfMemoryError) {
                Log.e("Bracket", "Se agotó la memoria: ${e.message}")
                _statusMessage.postValue("Error de memoria. Demasiados jugadores.")
            }
        }
    }

    fun generateSingleEliminationBracket(players: List<Player>): List<MatchSet> {

        val playerCount = players.size
        if (playerCount <= 1) return emptyList()

        val MAX_PLAYERS = 4096
        if (playerCount > MAX_PLAYERS) {
            throw IllegalArgumentException("Máximo de $MAX_PLAYERS jugadores permitido.")
        }

        var bracketSize = 1
        while (bracketSize < playerCount) {
            bracketSize *= 2
        }

        val numByes = bracketSize - playerCount
        val sortedPlayers = players.sortedBy { it.seed }
        val playersWithBye = sortedPlayers.take(numByes)
        val playersInR1 = sortedPlayers.drop(numByes)

        val r1Order = MutableList<Player?>(bracketSize) { null }

        // 1. Insertar a los jugadores con BYE en su posición y el slot de su oponente como null
        for (i in 0 until numByes) {
            val playerWithBye = playersWithBye[i]
            val pairIndex = getByePosition(i, bracketSize)

            // Colocamos el jugador en el slot par y su oponente nulo en el impar
            r1Order[pairIndex] = playerWithBye
            r1Order[pairIndex + 1] = null
        }

        // 2. Generar la secuencia de emparejamientos para R1 (Seed 4 vs Seed 5, etc.)
        val r1MatchupOrder = mutableListOf<Player?>()
        var low = 0
        var high = playersInR1.size - 1

        while (low <= high) {
            if (low < high) {
                r1MatchupOrder.add(playersInR1[low])
                r1MatchupOrder.add(playersInR1[high])
            } else if (low == high) {
                r1MatchupOrder.add(playersInR1[low])
            }
            low++
            high--
        }

        // 3. Asignar directamente los jugadores de R1 a los slots restantes (pares de nulls)
        var r1MatchupIndex = 0
        for (i in 0 until bracketSize step 2) {
            // Buscamos un par de slots que sean AMBOS nulos (indicando un partido de R1 sin BYE)
            if (r1Order[i] == null && r1Order[i + 1] == null) {

                if (r1MatchupIndex < r1MatchupOrder.size) {
                    r1Order[i] = r1MatchupOrder[r1MatchupIndex++]
                }
                if (r1MatchupIndex < r1MatchupOrder.size) {
                    r1Order[i + 1] = r1MatchupOrder[r1MatchupIndex++]
                }
            }
        }

        val allSetsAsc = mutableListOf<MatchSet>()
        val r1Matches = mutableListOf<Player?>()

        for (i in 0 until bracketSize step 2) {
            val p1 = r1Order[i]
            val p2 = r1Order[i + 1]

            r1Matches.add(p1)
            r1Matches.add(p2)
        }

        var nextSetIdAsc = 1
        var playerIndexR1 = 0
        var setsInCurrentRoundAsc = bracketSize / 2
        var roundCounterAsc = 1

        while (setsInCurrentRoundAsc >= 1) {
            val setsEndIdAsc = nextSetIdAsc + setsInCurrentRoundAsc - 1
            val nextRoundSetsAsc = setsInCurrentRoundAsc / 2
            val isFinalRound = (nextRoundSetsAsc == 0)
            val roundNameAsc = if (isFinalRound) "Final" else "Ronda $roundCounterAsc"

            for (i in 0 until setsInCurrentRoundAsc) {
                val currentIdAsc = nextSetIdAsc++

                var p1: Player? = null
                var p2: Player? = null

                if (roundCounterAsc == 1) {
                    if (playerIndexR1 < r1Matches.size) {
                        p1 = r1Matches[playerIndexR1++]
                    }
                    if (playerIndexR1 < r1Matches.size) {
                        p2 = r1Matches[playerIndexR1++]
                    }
                }

                val parentIdAsc: Int? = if (!isFinalRound) {
                    setsEndIdAsc + 1 + (i / 2)
                } else {
                    null
                }

                allSetsAsc.add(MatchSet(
                    setId = currentIdAsc,
                    roundName = roundNameAsc,
                    parentSetId = parentIdAsc,
                    player1 = p1,
                    player2 = p2
                ))
            }

            setsInCurrentRoundAsc = nextRoundSetsAsc
            roundCounterAsc++
        }

        val totalSetsGenerated = allSetsAsc.size
        var setsInRoundToRename = 1
        var setsProcessed = 0
        var setsRemaining = totalSetsGenerated
        val totalRounds = roundCounterAsc - 1

        while (setsRemaining > 0) {
            val numSetsInThisRound = minOf(setsInRoundToRename, setsRemaining)
            val currentRoundIndex = totalRounds - (setsProcessed / setsInRoundToRename)

            val newRoundName = when (setsInRoundToRename) {
                1 -> "Final"
                2 -> "Semifinal"
                4 -> "Cuartos de Final"
                else -> "Ronda $currentRoundIndex"
            }

            val startIndex = setsRemaining - numSetsInThisRound

            for (i in startIndex until setsRemaining) {
                val currentSet = allSetsAsc[i]
                allSetsAsc[i] = currentSet.copy(roundName = newRoundName)
            }

            setsRemaining = startIndex
            setsProcessed += numSetsInThisRound
            setsInRoundToRename *= 2
        }

        return allSetsAsc.toList()
    }

    private fun getByePosition(byeIndex: Int, bracketSize: Int): Int {
        if (byeIndex < 0 || bracketSize < 2) return -1

        val numSlots = bracketSize / 2
        val slots = (0 until numSlots).toList()

        val byeSlots = mutableListOf<Int>()
        byeSlots.add(slots.first())
        if (numSlots > 1) {
            byeSlots.add(slots.last())
        }

        var low = 1
        var high = slots.size - 2

        while (low <= high) {
            if (low <= high) byeSlots.add(slots[high--])
            if (low <= high) byeSlots.add(slots[low++])
        }

        if (byeIndex >= byeSlots.size) return -1

        val slotIndex = byeSlots[byeIndex]
        return slotIndex * 2
    }

    fun processByes(generatedSets: List<MatchSet>): List<MatchSet> {
        val updatedSets = generatedSets.toMutableList()

        val setMap = updatedSets.associateBy { it.setId }.toMutableMap()

        for (currentSet in generatedSets) {
            if (currentSet.player1 == null && currentSet.player2 == null) {
                continue
            }

            val advancingPlayer: Player? = when {

                currentSet.player1 != null && currentSet.player2 == null -> {
                    Log.d("ByeProcessor", "Set ${currentSet.setId}: ${currentSet.player1?.name} avanza por BYE.")
                    currentSet.player1
                }
                currentSet.player1 == null && currentSet.player2 != null -> {
                    Log.d("ByeProcessor", "Set ${currentSet.setId}: ${currentSet.player2?.name} avanza por BYE.")
                    currentSet.player2
                }
                else -> continue
            }

            if (advancingPlayer != null) {
                val parentId = currentSet.parentSetId

                if (parentId != null) {
                    val parentSet = setMap[parentId]

                    if (parentSet != null) {
                        val isFirstChild = (currentSet.setId % 2 != 0)

                        val newParentSet = if (isFirstChild) {
                            parentSet.copy(player1 = advancingPlayer)
                        } else {
                            parentSet.copy(player2 = advancingPlayer)
                        }

                        setMap[parentId] = newParentSet
                        updatedSets[updatedSets.indexOfFirst { it.setId == parentId }] = newParentSet

                        Log.d("ByeProcessor", "Set ${parentId} actualizado con ${advancingPlayer.name}.")
                    }
                }
            }
        }

        return updatedSets.toList()
    }
}