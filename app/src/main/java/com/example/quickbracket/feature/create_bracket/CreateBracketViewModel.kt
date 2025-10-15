package com.example.quickbracket.feature.create_bracket

import android.app.Application
import android.text.Editable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.quickbracket.R
import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        viewModelScope.launch(Dispatchers.Default) { // Usamos Dispatchers.Default para trabajo intensivo de CPU
            try {
                // Ejecutar la lógica pesada fuera del hilo principal
                val bracketSets = generateSingleEliminationBracket(players)

                // Asignar el resultado al LiveData. Esto lo hace internamente en el hilo principal.
                _generatedBracketSets.postValue(bracketSets)

            } catch (e: IllegalArgumentException) {
                // Manejar el límite de jugadores o cualquier error de lógica
                Log.e("Bracket", "Error de límite o lógica: ${e.message}")
                _statusMessage.postValue("Error en la creación del bracket: ${e.message}")
            } catch (e: OutOfMemoryError) {
                // Manejo de OutOfMemory
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

        val allSetsAsc = mutableListOf<MatchSet>()
        val sortedPlayers = players.sortedBy { it.seed }

        val r1Order = MutableList<Player?>(bracketSize) { null }
        var low = 0
        var high = bracketSize - 1
        var playerIndex = 0

        while (low <= high && playerIndex < sortedPlayers.size) {
            r1Order[low] = sortedPlayers[playerIndex++]

            if (low != high && playerIndex < sortedPlayers.size) {
                r1Order[high] = sortedPlayers[playerIndex++]
            }

            low++
            high--
        }

        val r1Matches = mutableListOf<Player?>()
        for (i in 0 until bracketSize step 2) {
            val p1 = r1Order[i]
            val p2 = r1Order[i + 1]

            if (p1 != null || p2 != null) {
                r1Matches.add(p1)
                r1Matches.add(p2)
            }
        }

        var nextSetIdAsc = 1
        var playerIndexR1 = 0
        var setsInCurrentRoundAsc = r1Matches.size / 2
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
                4 -> "Quarters"
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


}