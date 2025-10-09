package com.example.quickbracket.feature.bracket_details

import android.content.res.Resources
import com.example.quickbracket.databinding.ItemRoundLayoutBinding
import com.example.quickbracket.model.MatchSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quickbracket.R
import com.example.quickbracket.databinding.ItemBracketBinding
import com.example.quickbracket.model.Bracket
import androidx.navigation.findNavController
import com.example.quickbracket.databinding.ItemSetBinding

interface OnMatchSetClickListener {
    fun onMatchSetClicked(matchSet: MatchSet)
}

class RoundAdapter(
    private val setsByRound: Map<String, List<MatchSet>>,
    private val listener: OnMatchSetClickListener
) : RecyclerView.Adapter<RoundAdapter.RoundViewHolder>() {

    // Necesitas el contexto para inflar y usar recursos, lo obtenemos del parent
    private lateinit var inflater: LayoutInflater
    private lateinit var resources: Resources

    // Lista de nombres de rondas para acceder ordenadamente
    private val roundNames = setsByRound.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoundViewHolder {
        if (!::inflater.isInitialized) {
            inflater = LayoutInflater.from(parent.context)
            resources = parent.resources
        }
        val binding = ItemRoundLayoutBinding.inflate(inflater, parent, false)
        return RoundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoundViewHolder, position: Int) {
        val roundName = roundNames[position]
        val roundSets = setsByRound[roundName] ?: emptyList()

        // Obtenemos la cantidad de sets en la siguiente ronda para el cálculo de espaciado
        val setsInNextRound = if (position < roundNames.size - 1) {
            setsByRound[roundNames[position + 1]]?.size ?: 0
        } else {
            0
        }

        holder.bind(roundName, roundSets, setsInNextRound, inflater, resources, roundNames.size - 1)
    }

    override fun getItemCount(): Int = roundNames.size

    inner class RoundViewHolder(private val binding: ItemRoundLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            roundName: String,
            roundSets: List<MatchSet>,
            setsInNextRound: Int,
            inflater: LayoutInflater,
            resources: Resources,

            lastRoundIndex: Int
        ) {
            binding.roundTitle.text = roundName

            // Limpia el contenedor para evitar vistas duplicadas al reciclarse
            binding.setsVerticalContainer.removeAllViews()

            // Re-implementa la lógica de creación de sets y espaciadores
            roundSets.forEachIndexed { index, matchSet ->
                val setBinding = ItemSetBinding.inflate(inflater, binding.setsVerticalContainer, false)

                setBinding.player1Name.text = "Set ${matchSet.player1?.name} P1"
                setBinding.player2Name.text = "Set ${matchSet.player2?.name} P2"

                setBinding.root.setOnClickListener {
                    listener.onMatchSetClicked(matchSet)
                }

                binding.setsVerticalContainer.addView(setBinding.root)

                // Lógica de espaciado (similar a la que tenías)
                val baseSpacing = resources.getDimensionPixelSize(R.dimen.set_base_spacing)
                val roundFactor = if (setsInNextRound > 0) roundSets.size / setsInNextRound else 1
                val verticalSpace = baseSpacing * roundFactor

                val isLastSet = index == roundSets.lastIndex
                val isFinalRound = layoutPosition == lastRoundIndex

                if (!isLastSet || !isFinalRound) { // Mantenemos el espaciador si no es el último set de la ronda final
                    val spacer = View(itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            verticalSpace
                        )
                    }
                    binding.setsVerticalContainer.addView(spacer)
                }
            }
            binding.root.gravity = android.view.Gravity.CENTER


        }
    }
}