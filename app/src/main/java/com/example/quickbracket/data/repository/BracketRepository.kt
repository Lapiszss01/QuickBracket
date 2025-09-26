package com.example.quickbracket.data.repository

import android.content.Context
import com.example.quickbracket.data.source.BracketDataStoreManager
import com.example.quickbracket.model.Bracket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BracketRepository(context: Context) {

    private val dataStoreManager = BracketDataStoreManager(context)
    val allBrackets: Flow<List<Bracket>> = dataStoreManager.getBracketList

    suspend fun addBracket(newBracketName: String) {

        val newBracket = Bracket(name = newBracketName)
        val currentList = dataStoreManager.getBracketList.first()

        val updatedList = currentList.toMutableList().apply {
            add(newBracket)
        }

        dataStoreManager.saveBracketList(updatedList)
    }
}