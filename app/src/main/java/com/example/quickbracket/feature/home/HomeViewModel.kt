package com.example.quickbracket.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.data.repository.BracketRepository
import com.example.quickbracket.model.Bracket
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = BracketRepository(application)
    val allBracketsLiveData: LiveData<List<Bracket>> = repository.allBrackets.asLiveData()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    fun deleteBracket(bracket: Bracket) {
        viewModelScope.launch {
            try {
                repository.deleteBracket(bracket)
                _statusMessage.postValue("${bracket.name} deleted")
            } catch (e: Exception) {
                _statusMessage.postValue("Error deleting")
            }
        }
    }
}