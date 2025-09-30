package com.example.quickbracket.feature.create_bracket

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import android.util.Log
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentCreateBracketBinding
import androidx.navigation.fragment.findNavController
import com.example.quickbracket.model.BracketPath
import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player
import com.google.android.material.textfield.TextInputEditText


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CreateBracketFragment : Fragment() {

    private val bracketViewModel: BracketViewModel by viewModels()

    private var _binding: FragmentCreateBracketBinding? = null
    private val binding get() = _binding!!

    private val playerViews = mutableListOf<View>()

    enum class BracketType {
        //SingleElimination,
        DoubleElimination,
        //RoundRobin
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBracketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Assing bracket types
        val bracketTypes = BracketType.entries.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            bracketTypes
        )
        binding.bracketTypeAutoCompleteTextView.setAdapter(adapter)

        //Assign player count and edit text
        val playerCountEditText = binding.etPlayerCount as? TextInputEditText
        playerCountEditText?.doAfterTextChanged { editable ->
            val playerCountText = editable?.toString()
            val count = playerCountText.orEmpty().toIntOrNull() ?: 0
            updatePlayerTextViews(count)
        }

        //Create button
        binding.createBracketButton.setOnClickListener {
            //Data
            val bracketName = binding.bracketNameEditText.text.toString()
            val bracketType = binding.bracketTypeAutoCompleteTextView.text.toString()
            val playerCount = binding.etPlayerCount.text
            //Players
            val players = getRegisteredPlayerNames()
            val winnersSets = generateWinnersBracket(players.count())
            val losetsSet = generateLosersBracket(players.count())


            Log.d("BracketCreation", "Winnerbracket:${losetsSet.count()}")


            //bracketViewModel.saveNewBracket(bracketName,bracketType)
        }

        // Observes messages
        bracketViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                binding.bracketNameEditText.text?.clear()
            }
        }

        bracketViewModel.bracketCreated.observe(viewLifecycleOwner){ created ->
            if(created){
                findNavController().navigate(R.id.action_CreateBracketFragment_to_HomeFragment)
            }
        }
    }

    private fun updatePlayerTextViews(count: Int) {
        val container = binding.playerListContainer

        container.removeAllViews()
        playerViews.clear()

        val inflater = LayoutInflater.from(requireContext())

        for (i in 1..count) {

            val playerItemView = inflater.inflate(
                R.layout.item_player_registration,
                container,
                false
            )
            val tvSeeding = TextView(requireContext()).apply {
                text = "$i"
                setPadding(0, 8, 0, 8)
            }
            container.addView(tvSeeding)
            container.addView(playerItemView)

            playerViews.add(playerItemView)
        }
    }

    private fun getRegisteredPlayerNames(): List<Player> {

        val players = mutableListOf<Player>()
        for (itemView in playerViews) {
            val editText = itemView.findViewById<TextInputEditText>(R.id.player_name_edit_text)
            val playerName = editText?.text?.toString()?.trim()
            if (!playerName.isNullOrEmpty()) {
                val player = Player(name = playerName)
                players.add(player)
            } else {
                // TODO: Control de campo vacío.
            }
        }
        return players
    }

    fun generateWinnersBracket(playerCount: Int): List<MatchSet> {
        if (playerCount <= 1) return emptyList()

        var bracketSize = 1
        while (bracketSize < playerCount) {
            bracketSize *= 2
        }

        var setsInCurrentRound = bracketSize / 2
        val wbSets = mutableListOf<MatchSet>()
        var nextSetId = 1
        var nextParentSetId = setsInCurrentRound + 1
        var currentRound = 1

        while (setsInCurrentRound >= 1) {
            val roundName = when (setsInCurrentRound) {
                1 -> "Final WB"
                2 -> "Semifinales WB"
                4 -> "Cuartos de Final WB"
                else -> "Ronda WB $currentRound"
            }

            var parentSetId = nextParentSetId

            for (i in 0 until setsInCurrentRound) {
                val currentParentId: Int? = if (setsInCurrentRound > 1) {
                    if (i % 2 == 0) {
                        val id = parentSetId
                        parentSetId++
                        id
                    } else {
                        parentSetId - 1
                    }
                } else {
                    null
                }

                val newSet = MatchSet(
                    setId = nextSetId,
                    parentSetId = currentParentId,
                    roundName = roundName,
                    path = BracketPath.WINNERS,
                    // loserDropSetId se asignará después
                )

                wbSets.add(newSet)
                nextSetId++
            }

            nextParentSetId = nextSetId
            setsInCurrentRound /= 2
            currentRound++
        }
        return wbSets.toList()
    }

    fun generateLosersBracket(wbSetCount: Int): List<MatchSet> {
        if (wbSetCount <= 2) return emptyList() // Necesita al menos R1 y R2 de WB

        // El número de jugadores que caen es el mismo que el de sets en el WB
        val setsInWBRound1 = (wbSetCount + 1) / 2
        val totalLBSets = wbSetCount - 2 // Total de partidos del LB sin la Gran Final

        var setsInCurrentRound = setsInWBRound1 / 2 // La primera ronda del LB (LB Round 1)
        val lbSets = mutableListOf<MatchSet>()

        // El set ID debe continuar a partir del último ID usado en el WB
        var nextSetId = wbSetCount + 1

        var currentRound = 1
        var currentPhase = 1 // 1: Fase de Caída (Drop-in), 2: Fase de Emparejamiento

        while (lbSets.size < totalLBSets) {

            val phaseName = if (currentPhase == 1) "Caída" else "Emparejamiento"
            val roundName = "Ronda LB $currentRound ($phaseName)"

            // Número de sets a crear en esta ronda
            val numSets = if (currentPhase == 1) setsInCurrentRound else setsInCurrentRound / 2

            // Asignación de IDs de set padre para esta fase (muy complejo, simplificamos aquí)
            var parentSetId = nextSetId + numSets // El ID del set que le sigue

            for (i in 0 until numSets) {

                // Lógica de ID de Padre simplificada: cada set avanza al set inmediatamente posterior
                // en la lista, si no es el último.
                val currentParentId: Int? = if (lbSets.size < totalLBSets - 1) {
                    // Si esta fase es de Caída, avanza al set de Emparejamiento de la misma ronda
                    if (currentPhase == 1) nextSetId + numSets else parentSetId++
                } else {
                    null // El último set del LB avanza a la Gran Final
                }

                val newSet = MatchSet(
                    setId = nextSetId,
                    parentSetId = currentParentId,
                    roundName = roundName,
                    path = BracketPath.LOSERS
                )

                lbSets.add(newSet)
                nextSetId++
            }

            if (currentPhase == 2) {
                // Después de la fase de emparejamiento, el número de sets se reduce a la mitad
                setsInCurrentRound /= 2
                currentRound++
            }

            // Alternar entre las dos fases
            currentPhase = if (currentPhase == 1) 2 else 1
        }

        return lbSets.toList()
    }

}