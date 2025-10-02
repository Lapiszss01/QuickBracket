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
import com.example.quickbracket.model.Bracket
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
        SingleElimination,
        //DoubleElimination,
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
            //Players
            val players = getRegisteredPlayerNames()
            val bracketSets = generateSingleEliminationBracket(players.count())

            val finalBracket = Bracket(name = bracketName, type = bracketType, sets = bracketSets)
            bracketViewModel.saveNewBracket(finalBracket)

            Log.d("BrackedCreation", "Rondas: ${bracketSets.count()}")
            for (b in bracketSets){
                Log.d("BrackedCreation", "Ronda: ${b}")
            }

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

    fun generateSingleEliminationBracket(playerCount: Int): List<MatchSet> {
        if (playerCount <= 1) return emptyList()

        // 1. Determinar el tamaño de la llave y los BYEs
        var bracketSize = 1
        while (bracketSize < playerCount) {
            bracketSize *= 2
        }
        val byes = bracketSize - playerCount

        val allSets = mutableListOf<MatchSet>()
        var nextSetId = 1

        // 2. Calcular los sets reales en la primera ronda
        // La primera ronda solo tiene partidos para los jugadores que NO tienen un BYE.
        // Ej: N=5, bracketSize=8, byes=3. Partidos reales: (5 - 3) / 2 = 1.
        var setsInCurrentRound = (playerCount - byes) / 2

        // Si la fórmula da 0 o menos, significa que hay muchos BYEs y los partidos empezarán más tarde.
        // Este caso se corrige en el bucle principal.
        if (setsInCurrentRound < 1 && playerCount > 1) {
            setsInCurrentRound = playerCount - (bracketSize / 2)
            if (setsInCurrentRound <= 0) setsInCurrentRound = 1 // Caso N=3: 1 partido real.
        }

        var roundCounter = 1

        // 3. Generar las rondas hasta que solo quede el set final
        while (setsInCurrentRound >= 1) {
            val roundName = when (setsInCurrentRound) {
                1 -> "Final"
                2 -> "Semifinales"
                4 -> "Cuartos de Final"
                else -> "Ronda $roundCounter"
            }

            val currentRoundSets = mutableListOf<MatchSet>()
            val setsStartId = nextSetId
            val setsEndId = nextSetId + setsInCurrentRound - 1

            for (i in 0 until setsInCurrentRound) {
                val currentId = nextSetId++

                // Asignación de parentSetId (dónde va el ganador)
                val parentId: Int? = if (setsInCurrentRound > 1) {
                    // Los sets se agrupan en pares que alimentan al siguiente set.
                    // El ganador va al set de la siguiente ronda.
                    setsEndId + 1 + (i / 2)
                } else {
                    null // El ganador de la final no tiene set padre (es el campeón)
                }

                currentRoundSets.add(MatchSet(
                    setId = currentId,
                    roundName = roundName,
                    parentSetId = parentId
                ))
            }

            allSets.addAll(currentRoundSets)

            // La siguiente ronda siempre tendrá la mitad de partidos.
            setsInCurrentRound /= 2
            roundCounter++
        }

        return allSets.toList()
    }

}