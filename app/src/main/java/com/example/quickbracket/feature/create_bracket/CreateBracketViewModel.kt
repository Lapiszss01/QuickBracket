package com.example.quickbracket.feature.create_bracket

import DoubleEliminationGenerator
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

    //Repositorio de brackets local
    private val repository = BracketRepository(application)

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

    fun createBracket(players: List<Player>, type: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Decidir qué generador usar
                val generator: BracketGenerator = when (type) {
                    "DoubleElimination" -> DoubleEliminationGenerator()
                    else -> SingleEliminationGenerator()
                }

                // 2. Generar
                val finalBracketSets = generator.generate(players)

                _generatedBracketSets.postValue(finalBracketSets)
            } catch (e: Exception) {
                _statusMessage.postValue("Error: ${e.message}")
            }
        }
    }


}