package com.example.quickbracket.feature.createbracket

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickbracket.model.Bracket

class BracketViewModel : ViewModel() {

    private val _createdBracket = MutableLiveData<Bracket?>()
    val createdBracket: LiveData<Bracket?> = _createdBracket

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    fun createNewBracket(name: String) {
        if (name.isBlank()) {
            _statusMessage.value = "El nombre del Bracket no puede estar vacío."
            _createdBracket.value = null
            return
        }

        val newBracket = Bracket(name = name.trim())

        // PlaceHolder, must save the bracket locally
        Log.d("CreateBracket", newBracket.toString())


        _createdBracket.value = newBracket
        _statusMessage.value = "Bracket '${newBracket.name}' creado con éxito."
    }

    /**
     * Resetea el estado de creación después de haber sido manejado.
     */
    fun doneCreatingBracket() {
        _createdBracket.value = null
        _statusMessage.value = ""
    }
}