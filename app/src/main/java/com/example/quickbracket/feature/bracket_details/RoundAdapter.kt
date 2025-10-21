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
        val setsInPreviousRound = if (position > 0) {
            setsByRound[roundNames[position - 1]]?.size ?: 0
        } else {
            0 // Primera ronda, no hay sets anteriores
        }

        holder.bind(
            roundName,
            roundSets,
            setsInPreviousRound,
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

            val isFirstRound = layoutPosition == 0
            val isLastRound = layoutPosition == lastRoundIndex
            val initialOffset = if (!isLastRound && layoutPosition > 0) verticalSpace / 2 else 0

            //Filter rounds 1 so it doest show blank sets
            val visibleSets = roundSets
            /*val visibleSets = roundSets.filterNot { matchSet ->
                if (isFirstRound) {
                    // Condición de Bye: Un jugador está presente (no nulo) y el otro está ausente (nulo)
                    val isBye = (matchSet.player1 != null && matchSet.player2 == null) ||
                            (matchSet.player1 == null && matchSet.player2 != null)

                    if (isBye) {
                        Log.d("RoundAdapter", "Skipping first-round bye for set: ${matchSet.setId}")
                    }
                    isBye
                } else {
                    false
                }
            }*/

            // Usamos una lista temporal para gestionar las vistas y espaciadores a añadir
            val viewsToAdd = mutableListOf<View>()

            if (initialOffset > 0) {
                val initialSpacer = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        initialOffset
                    )
                }
                viewsToAdd.add(initialSpacer)
            }

            visibleSets.forEachIndexed { index, matchSet ->
                val setBinding = ItemSetBinding.inflate(inflater, binding.setsVerticalContainer, false)

                val isFinished = matchSet.isFinished == true
                val winner = if (isFinished) matchSet.winner else null

                // Player 1 setup
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

                // Player 2 setup
                val player2NameText = if (matchSet.player2?.name.isNullOrBlank()) {
                    "TBD"
                } else {
                    "${matchSet.player2?.seed ?: ""} - ${matchSet.player2?.name}"
                }
                setBinding.player2Name.text = player2NameText

                setBinding.tvsetId.text = matchSet.setLetter

                if (isFinished && winner?.id == matchSet.player2?.id) {
                    setBinding.player2Name.setTypeface(null, Typeface.BOLD)
                } else {
                    setBinding.player2Name.setTypeface(null, Typeface.NORMAL)
                }

                setBinding.root.setOnClickListener {
                    Log.d("SetCLick",matchSet.toString())
                    listener.onMatchSetClicked(matchSet)
                }

                viewsToAdd.add(setBinding.root) // Añadimos el set card

                val isLastVisibleSet = index == visibleSets.lastIndex

                // Solo añadimos espaciador entre sets si NO es el último set visible
                if (!isLastVisibleSet) {
                    val spacer = View(itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            verticalSpace
                        )
                    }
                    viewsToAdd.add(spacer)
                }
            }

            // *** CAMBIO CLAVE 2: Anular el offset final para la Final ***
            if (initialOffset > 0) { // Si hay offset inicial, también debe haber uno final (si no es la Final)
                val finalSpacer = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        initialOffset
                    )
                }
                viewsToAdd.add(finalSpacer)
            }

            // Añadimos todas las vistas recolectadas al contenedor
            viewsToAdd.forEach {
                binding.setsVerticalContainer.addView(it)
            }

            // Asegura que el contenedor se centre verticalmente dentro de su celda
            binding.root.gravity = android.view.Gravity.CENTER

            // Si es la Final, y solo hay 1 set, le damos al LinearLayout una gravedad de CENTRO también
            if (isLastRound && visibleSets.size == 1) {
                binding.setsVerticalContainer.gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }
    }
}