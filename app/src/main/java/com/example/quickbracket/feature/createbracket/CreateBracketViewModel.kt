package com.example.quickbracket.feature.createbracket

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BracketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BracketRepository(application)

    val allBracketsLiveData: LiveData<List<Bracket>> = repository.allBrackets.asLiveData()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _bracketCreated = MutableLiveData<Boolean>()
    val bracketCreated: LiveData<Boolean> = _bracketCreated

    fun saveNewBracket(name: String) {
        if (name.isBlank()) {
            _statusMessage.value = "El nombre del Bracket no puede estar vacío."
            return
        }

        viewModelScope.launch {
            try {
                // El repositorio se encarga de leer, añadir y guardar la lista completa.
                repository.addBracket(name)
                _statusMessage.postValue("Bracket '$name' creado y lista actualizada.")
                _bracketCreated.postValue(true)
            } catch (e: Exception) {
                _statusMessage.postValue("Error al guardar: ${e.message}")
                _bracketCreated.postValue(false)

            }
        }
    }
}