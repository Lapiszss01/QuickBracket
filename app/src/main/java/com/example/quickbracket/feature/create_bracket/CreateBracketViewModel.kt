package com.example.quickbracket.feature.create_bracket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CreateBracketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BracketRepository(application)

    val allBracketsLiveData: LiveData<List<Bracket>> = repository.allBrackets.asLiveData()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _bracketCreated = MutableLiveData<Boolean>()
    val bracketCreated: LiveData<Boolean> = _bracketCreated

    fun saveNewBracket(bracket: Bracket) {
        if (bracket.name.isBlank() || bracket.type.isBlank()) {
            _statusMessage.value = "Must not leave any field blank."
            return
        }

        viewModelScope.launch {
            try {
                // El repositorio se encarga de leer, añadir y guardar la lista completa.
                repository.addBracket(bracket)
                _statusMessage.postValue("Bracket '${bracket.name}' created.")
                _bracketCreated.postValue(true)
            } catch (e: Exception) {
                _statusMessage.postValue("Error saving: ${e.message}")
                _bracketCreated.postValue(false)

            }
        }
    }
}