package com.example.quickbracket.feature.bracket_details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BracketDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BracketRepository(application)

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _bracketUpdated = MutableLiveData<Boolean>()
    val bracketUpdated: LiveData<Boolean> = _bracketUpdated

    fun updateBracketSets(bracket: Bracket) {
        viewModelScope.launch {
            try {
                repository.editBracket(bracket)
                _statusMessage.postValue("Bracket '${bracket.name}' created.")
                _bracketUpdated.postValue(true)
            } catch (e: Exception) {
                _statusMessage.postValue("Error saving: ${e.message}")
                _bracketUpdated.postValue(false)

            }
        }
    }
}