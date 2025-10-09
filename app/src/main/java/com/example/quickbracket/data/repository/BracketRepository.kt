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

    suspend fun addBracket(bracket: Bracket) {

        val currentList = dataStoreManager.getBracketList.first()
        val updatedList = currentList.toMutableList().apply {
            add(bracket)
        }
        dataStoreManager.saveBracketList(updatedList)
    }

    suspend fun deleteBracket(bracket: Bracket) {
        val currentList = dataStoreManager.getBracketList.first()
        val updatedList = currentList.filterNot { it.id == bracket.id }
        dataStoreManager.saveBracketList(updatedList)
    }

    suspend fun editBracket(bracket: Bracket) {
        val currentList = dataStoreManager.getBracketList.first()

        val updatedList = currentList.map { existingBracket ->
            if (existingBracket.id == bracket.id) {
                bracket
            } else {
                existingBracket
            }
        }
        dataStoreManager.saveBracketList(updatedList)
    }

}