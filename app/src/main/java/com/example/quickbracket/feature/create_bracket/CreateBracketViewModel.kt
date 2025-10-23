package com.example.quickbracket.feature.create_bracket

import android.app.Application
import android.util.Log
import androidx.fragment.app.viewModels
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
        if (bracket.name.isBlank() || bracket.type.isBlank()) {
            _statusMessage.value = "Must not leave any field blank."
            return
        } else if(playerCount.toInt() <= 1){
            _statusMessage.value = "Must have at least 2 players"
            return
        } else if(playerCount.toInt() != bracket.entrants.size){
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
                val initialBracketSets = generateSingleEliminationBracket(players)
                val finalBracketSets = processByes(initialBracketSets)
                _generatedBracketSets.postValue(finalBracketSets)
            } catch (e: IllegalArgumentException) {
                Log.e("Bracket", "Logic error: ${e.message}")
                _statusMessage.postValue("Error in bracket creation: ${e.message}")
            } catch (e: OutOfMemoryError) {
                Log.e("Bracket", "Out of memory: ${e.message}")
                _statusMessage.postValue("Memory error, lower de number of players.")
            }
        }
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

    fun generateSingleEliminationBracket(players: List<Player>): List<MatchSet> {

        val playerCount = players.size
        if (playerCount <= 1) return emptyList()

        val MAX_PLAYERS = 4096
        if (playerCount > MAX_PLAYERS) {
            throw IllegalArgumentException("Máx of $MAX_PLAYERS players allowed.")
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

        // Insers players with BYE
        for (i in 0 until numByes) {
            val playerWithBye = playersWithBye[i]
            val pairIndex = getByePosition(i, bracketSize)
            r1Order[pairIndex] = playerWithBye
            r1Order[pairIndex + 1] = null
        }

        // Generace R1 marchups
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

        // Assign players without bye to not fully sets
        var r1MatchupIndex = 0
        for (i in 0 until bracketSize step 2) {
            if (r1Order[i] == null && r1Order[i + 1] == null) {
                if (r1MatchupIndex < r1MatchupOrder.size) {
                    r1Order[i] = r1MatchupOrder[r1MatchupIndex++]
                }
                if (r1MatchupIndex < r1MatchupOrder.size) {
                    r1Order[i + 1] = r1MatchupOrder[r1MatchupIndex++]
                }
            }
        }

        val r1PlayersInOrder = mutableListOf<Player?>()
        for (i in 0 until bracketSize step 2) {
            r1PlayersInOrder.add(r1Order[i])
            r1PlayersInOrder.add(r1Order[i + 1])
        }

        val allSets = mutableListOf<MatchSet>()
        val roundSetIdsInSeedingOrder = mutableMapOf<Int, List<Int>>()

        var nextSetIdAsc = 1
        var playerIndexR1 = 0
        var setsInCurrentRound = bracketSize / 2
        var roundCounter = 1

        var currentRoundSetIds = mutableListOf<Int>()

        var previousRoundSetIds = (1..setsInCurrentRound).toList()

        while (setsInCurrentRound >= 1) {
            val nextRoundSets = setsInCurrentRound / 2
            val isFinalRound = (nextRoundSets == 0)
            val roundName = if (isFinalRound) "Final" else "Ronda $roundCounter"

            currentRoundSetIds.clear()
            for (i in 0 until setsInCurrentRound) {
                val currentId = nextSetIdAsc++
                currentRoundSetIds.add(currentId)

                var p1: Player? = null
                var p2: Player? = null

                if (roundCounter == 1) {
                    if (playerIndexR1 < r1PlayersInOrder.size) {
                        p1 = r1PlayersInOrder[playerIndexR1++]
                    }
                    if (playerIndexR1 < r1PlayersInOrder.size) {
                        p2 = r1PlayersInOrder[playerIndexR1++]
                    }
                }

                allSets.add(MatchSet(
                    setId = currentId,
                    roundName = roundName,
                    parentSetId = null,
                    player1 = p1,
                    player2 = p2
                ))
            }

            if (roundCounter > 1) {
                roundSetIdsInSeedingOrder[roundCounter - 1] = previousRoundSetIds.toList()
            } else {
                roundSetIdsInSeedingOrder[1] = currentRoundSetIds.toList()
            }

            val previousSetsMap = allSets.associateBy { it.setId }

            for (i in 0 until setsInCurrentRound) {
                val parentId = currentRoundSetIds[i]

                val prevSet1Id = previousRoundSetIds.getOrNull(i * 2)
                if (prevSet1Id != null) {
                    val prevSet1Index = allSets.indexOfFirst { it.setId == prevSet1Id }
                    if (prevSet1Index != -1) {
                        val currentSet = allSets[prevSet1Index]
                        allSets[prevSet1Index] = currentSet.copy(parentSetId = parentId)
                    }
                }

                val prevSet2Id = previousRoundSetIds.getOrNull(i * 2 + 1)
                if (prevSet2Id != null) {
                    val prevSet2Index = allSets.indexOfFirst { it.setId == prevSet2Id }
                    if (prevSet2Index != -1) {
                        val currentSet = allSets[prevSet2Index]
                        allSets[prevSet2Index] = currentSet.copy(parentSetId = parentId)
                    }
                }
            }

            val nextRoundSetIdsInOrder = mutableListOf<Int>()

            if (nextRoundSets == 0) {
                previousRoundSetIds = currentRoundSetIds.toList()
            } else if (roundCounter == 1) {
                if (currentRoundSetIds.size >= 4) {
                    val numMatches = currentRoundSetIds.size
                    val lowQuarter = currentRoundSetIds.take(numMatches / 4)
                    val highQuarter = currentRoundSetIds.drop(numMatches / 4 * 3)
                    val midLowQuarter = currentRoundSetIds.drop(numMatches / 4).take(numMatches / 4)
                    val midHighQuarter = currentRoundSetIds.drop(numMatches / 4 * 2).take(numMatches / 4)

                    val reordered = mutableListOf<Int>()
                    for (i in 0 until lowQuarter.size) {
                        reordered.add(lowQuarter[i])
                        reordered.add(midHighQuarter[i])
                        reordered.add(midLowQuarter[i])
                        reordered.add(highQuarter[i])
                    }
                    previousRoundSetIds = reordered
                } else {
                    previousRoundSetIds = currentRoundSetIds.toList()
                }

            } else {

                var lowIdIndex = 0
                var highIdIndex = currentRoundSetIds.size - 1

                while (lowIdIndex <= highIdIndex) {
                    if (lowIdIndex < highIdIndex) {
                        nextRoundSetIdsInOrder.add(currentRoundSetIds[lowIdIndex])
                        nextRoundSetIdsInOrder.add(currentRoundSetIds[highIdIndex])
                    } else if (lowIdIndex == highIdIndex) {
                        nextRoundSetIdsInOrder.add(currentRoundSetIds[lowIdIndex])
                    }
                    lowIdIndex++
                    highIdIndex--
                }
                previousRoundSetIds = nextRoundSetIdsInOrder
            }

            setsInCurrentRound = nextRoundSets
            roundCounter++
        }

        roundSetIdsInSeedingOrder[roundCounter - 1] = previousRoundSetIds.toList()

        val totalSetsGenerated = allSets.size
        var setsInRoundToRename = 1
        var setsProcessed = 0
        var setsRemaining = totalSetsGenerated
        val totalRounds = roundCounter - 1

        val setsInRoundMap = allSets.groupBy { it.roundName }
        val roundNamesByCount = setsInRoundMap.keys.sortedBy { setsInRoundMap[it]!!.size }

        val renamedSets = allSets.toMutableList()

        while (setsRemaining > 0) {
            val numSetsInThisRound = minOf(setsInRoundToRename, setsRemaining)
            val currentRoundIndex = totalRounds - (setsProcessed / setsInRoundToRename)

            val newRoundName = when (setsInRoundToRename) {
                1 -> "Final"
                2 -> "Semifinal"
                4 -> "Cuartos de Final"
                else -> "Ronda ${currentRoundIndex}"
            }

            val startIndex = setsRemaining - numSetsInThisRound

            for (i in startIndex until setsRemaining) {
                val currentSet = renamedSets[i]
                renamedSets[i] = currentSet.copy(roundName = newRoundName)
            }

            setsRemaining = startIndex
            setsProcessed += numSetsInThisRound
            setsInRoundToRename *= 2
        }


        val finalOrderedSets = mutableListOf<MatchSet>()
        val allSetsMap = renamedSets.associateBy { it.setId }

        for (roundIndex in 1 until roundCounter) {
            val orderedIds = roundSetIdsInSeedingOrder[roundIndex] ?: emptyList()
            orderedIds.forEach { setId ->
                allSetsMap[setId]?.let {
                    finalOrderedSets.add(it)
                }
            }
        }

        return finalOrderedSets.toList()
    }

    fun processByes(generatedSets: List<MatchSet>): List<MatchSet> {

        val setsWithInitialLetters = generatedSets.mapIndexed { index, matchSet ->
            matchSet
        }

        val setMap = setsWithInitialLetters.associateBy { it.setId }.toMutableMap()
        val setsToRemoveIds = mutableSetOf<Int>()

        for (currentSet in setsWithInitialLetters) {

            if (setsToRemoveIds.contains(currentSet.setId)) continue

            val isByeSet = (currentSet.player1 != null && currentSet.player2 == null) ||
                    (currentSet.player1 == null && currentSet.player2 != null)

            if (!isByeSet) continue
            val advancingPlayer: Player? = currentSet.player1 ?: currentSet.player2

            if (advancingPlayer != null) {
                val parentId = currentSet.parentSetId

                if (parentId != null) {
                    val parentSet = setMap[parentId]

                    if (parentSet != null) {
                        val newParentSet = when {
                            parentSet.player1 == null -> parentSet.copy(player1 = advancingPlayer)
                            parentSet.player2 == null -> parentSet.copy(player2 = advancingPlayer)
                            else -> parentSet
                        }

                        setMap[parentId] = newParentSet
                        setsToRemoveIds.add(currentSet.setId)
                    }
                }
            }
        }

        val finalSetsWithoutByes = setMap.values
            .filter { !setsToRemoveIds.contains(it.setId) }
        return finalSetsWithoutByes.mapIndexed { index, matchSet ->
            val newSetLetter = ('A' + index).toString()
            matchSet.copy(setLetter = newSetLetter)
        }
    }


}