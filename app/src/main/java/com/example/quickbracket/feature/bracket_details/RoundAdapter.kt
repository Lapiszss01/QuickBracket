package com.example.quickbracket.feature.bracket_details

import android.content.res.Resources
import android.graphics.Typeface
import com.example.quickbracket.databinding.ItemRoundLayoutBinding
import com.example.quickbracket.model.MatchSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.quickbracket.R
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

        // Obtenemos la cantidad de sets en la ronda anterior (la que alimenta a esta)
        // Usamos esta información para calcular el espaciado y centrado.
        val setsInPreviousRound = if (position > 0) {
            setsByRound[roundNames[position - 1]]?.size ?: 0
        } else {
            0 // Primera ronda, no hay sets anteriores
        }

        holder.bind(
            roundName,
            roundSets,
            setsInPreviousRound, // Usamos setsInPreviousRound para el cálculo de centrado
            inflater,
            resources,
            roundNames.size - 1
        )
    }

    override fun getItemCount(): Int = roundNames.size

    inner class RoundViewHolder(private val binding: ItemRoundLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            roundName: String,
            roundSets: List<MatchSet>,
            setsInPreviousRound: Int,
            inflater: LayoutInflater,
            resources: Resources,
            lastRoundIndex: Int
        ) {
            binding.roundTitle.text = roundName
            binding.setsVerticalContainer.removeAllViews()

            val baseSpacing = resources.getDimensionPixelSize(R.dimen.set_base_spacing)

            val setsInCurrentRound = roundSets.size

            val roundFactor = if (setsInCurrentRound > 0 && setsInPreviousRound > 0) {
                setsInPreviousRound / setsInCurrentRound
            } else {
                1
            }

            val verticalSpace = baseSpacing * roundFactor

            val initialOffset = if (layoutPosition > 0) verticalSpace / 2 else 0

            if (initialOffset > 0) {
                val initialSpacer = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        initialOffset
                    )
                }
                binding.setsVerticalContainer.addView(initialSpacer)
            }

            roundSets.forEachIndexed { index, matchSet ->
                val setBinding = ItemSetBinding.inflate(inflater, binding.setsVerticalContainer, false)

                val isFinished = matchSet.isFinished == true
                val winner = if (isFinished) matchSet.winner else null

                val player1NameText = if (matchSet.player1?.name.isNullOrBlank()) {
                    "TBD"
                } else {
                    "${matchSet.player1?.seed ?: ""} - ${matchSet.player1?.name}"
                }
                setBinding.player1Name.text = player1NameText

                if (isFinished && winner?.id == matchSet.player1?.id) {
                    setBinding.player1Name.setTypeface(null, Typeface.BOLD)
                } else {
                    setBinding.player1Name.setTypeface(null, Typeface.NORMAL)
                }

                val player2NameText = if (matchSet.player2?.name.isNullOrBlank()) {
                    "TBD"
                } else {
                    "${matchSet.player2?.seed ?: ""} - ${matchSet.player2?.name}"
                }
                setBinding.player2Name.text = player2NameText

                if (isFinished && winner?.id == matchSet.player2?.id) {
                    setBinding.player2Name.setTypeface(null, Typeface.BOLD)
                } else {
                    setBinding.player2Name.setTypeface(null, Typeface.NORMAL)
                }

                setBinding.root.setOnClickListener {
                    Log.d("SetCLick",matchSet.toString())
                    listener.onMatchSetClicked(matchSet)
                }

                binding.setsVerticalContainer.addView(setBinding.root)

                val isLastSet = index == roundSets.lastIndex
                val isFinalRound = layoutPosition == lastRoundIndex

                if (!isLastSet) {
                    val spacer = View(itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            verticalSpace
                        )
                    }
                    binding.setsVerticalContainer.addView(spacer)
                }
            }

            if (initialOffset > 0) {
                val finalSpacer = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        initialOffset
                    )
                }
                binding.setsVerticalContainer.addView(finalSpacer)
            }

            binding.root.gravity = android.view.Gravity.CENTER
        }
    }
}