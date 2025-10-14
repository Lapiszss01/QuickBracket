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

    private val bracketViewModel: CreateBracketViewModel by viewModels()

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
            val bracketSets = generateSingleEliminationBracket(players)

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

    fun generateSingleEliminationBracket(players: List<Player>): List<MatchSet> {
        val playerCount = players.size
        if (playerCount <= 1) return emptyList()

        // 1. Cálculo de Bracket Size y Byes
        var bracketSize = 1
        while (bracketSize < playerCount) {
            bracketSize *= 2
        }
        val byes = bracketSize - playerCount

        val allSetsAsc = mutableListOf<MatchSet>()
        var nextSetIdAsc = 1

        // 2. PREPARACIÓN DE PARTIDOS DE R1 (Incluye Byes Explícitos)
        // Usamos el orden de siembra estándar para asignar los byes de forma simétrica.
        val actualSeedingOrder = generateStandardSeedingOrder(playerCount, bracketSize)

        // Lista que contendrá a P1, P2, P1, P2, ... en el orden de los sets de R1,
        // incluyendo 'null' para los byes.
        val r1Matches = mutableListOf<Player?>()

        // Llenar r1Matches en el orden del bracket de R1
        // Iteramos de dos en dos (slots P1 y P2)
        for (i in 0 until bracketSize step 2) {
            val p1Seed = actualSeedingOrder[i]
            val p2Seed = actualSeedingOrder[i + 1]

            // Asignación con chequeo de siembra > 0 para evitar el error del '0'
            // Si la siembra es 0, o está fuera del rango (aunque la siembra estándar ya lo filtra), es un bye (null).
            val p1 = if (p1Seed > 0 && p1Seed <= playerCount) players[p1Seed - 1] else null
            val p2 = if (p2Seed > 0 && p2Seed <= playerCount) players[p2Seed - 1] else null

            // CLAVE: Solo agregamos el set si al menos un jugador o un bye existe.
            // En una siembra correcta, solo tendremos un set vacío si playerCount=0,
            // pero esta condición asegura que los sets con bye (jugador vs null) se incluyan.
            if (p1 != null || p2 != null) {
                r1Matches.add(p1) // Puede ser Player o null (bye)
                r1Matches.add(p2) // Puede ser Player o null (bye)
            }
        }

        var playerIndexR1 = 0 // Índice para recorrer r1Matches

        // 3. GENERACIÓN DE SETS CON ASIGNACIÓN DE JUGADORES (Modificación de R1)

        // El número de sets de R1 ahora es el tamaño de r1Matches / 2, que incluye los sets con bye.
        var setsInCurrentRoundAsc = r1Matches.size / 2
        var roundCounterAsc = 1

        // NOTA: Se elimina la variable `remainingByesAsc` ya que los byes se manejan con sets explícitos.

        while (setsInCurrentRoundAsc >= 1) {
            val currentRoundSetsAsc = mutableListOf<MatchSet>()
            val setsEndIdAsc = nextSetIdAsc + setsInCurrentRoundAsc - 1

            // Cálculo del número de sets de la siguiente ronda: mitad del número actual.
            // Esta simplificación funciona porque los sets con bye son tratados como sets que avanzan.
            val nextRoundSetsAsc = setsInCurrentRoundAsc / 2

            val isFinalRound = (nextRoundSetsAsc == 0)
            val roundNameAsc = if (isFinalRound) "Final" else "Ronda $roundCounterAsc"

            for (i in 0 until setsInCurrentRoundAsc) {
                val currentIdAsc = nextSetIdAsc++

                var p1: Player? = null
                var p2: Player? = null

                if (roundCounterAsc == 1) {
                    // ASIGNACIÓN CLAVE: Usamos la lista r1Matches, que contiene los jugadores y los 'null' (byes)
                    if (playerIndexR1 < r1Matches.size) {
                        p1 = r1Matches[playerIndexR1++]
                    }
                    if (playerIndexR1 < r1Matches.size) {
                        p2 = r1Matches[playerIndexR1++]
                    }
                }


                val parentIdAsc: Int? = if (!isFinalRound) {
                    setsEndIdAsc + 1 + (i / 2)
                } else {
                    null
                }

                currentRoundSetsAsc.add(MatchSet(
                    setId = currentIdAsc,
                    roundName = roundNameAsc,
                    parentSetId = parentIdAsc,
                    player1 = p1,
                    player2 = p2
                ))
            }

            allSetsAsc.addAll(currentRoundSetsAsc)

            // Transición
            setsInCurrentRoundAsc = nextRoundSetsAsc

            roundCounterAsc++
        }

        // 4. Renombrado Semántico (De atrás hacia adelante)

        val totalSetsGenerated = allSetsAsc.size

        var setsInRoundToRename = 1
        var setsProcessed = 0
        var setsRemaining = totalSetsGenerated

        // Usamos roundCounterAsc que es el total de rondas generadas + 1
        val totalRounds = roundCounterAsc - 1

        while (setsRemaining > 0) {

            val numSetsInThisRound = minOf(setsInRoundToRename, setsRemaining)

            // Calculamos el índice de ronda real de forma descendente (totalRounds, totalRounds-1, ...)
            val currentRoundIndex = totalRounds - (setsProcessed / setsInRoundToRename)

            val newRoundName = when (setsInRoundToRename) {
                1 -> "Final"
                2 -> "Semifinal"
                4 -> "Quarters"
                else -> "Ronda $currentRoundIndex"
            }

            val startIndex = setsRemaining - numSetsInThisRound

            for (i in startIndex until setsRemaining) {
                val currentSet = allSetsAsc[i]
                allSetsAsc[i] = currentSet.copy(roundName = newRoundName)
            }

            setsRemaining = startIndex
            setsProcessed += numSetsInThisRound
            setsInRoundToRename *= 2
        }

        return allSetsAsc.toList()
    }

    // Mantenemos la función auxiliar sin cambios, ya que ahora la usamos correctamente:
    fun generateStandardSeedingOrder(playerCount: Int, bracketSize: Int): List<Int> {
        if (playerCount <= 0) return emptyList()

        val bracket = IntArray(bracketSize) { 0 }
        val players = (1..playerCount).toList()

        var low = 0
        var high = bracketSize - 1
        var nextSeed = 1

        // Llenar el bracket de forma simétrica (1 contra N, 2 contra N-1, etc.)
        while (low <= high) {
            if (nextSeed <= playerCount) {
                bracket[low] = nextSeed
            }
            nextSeed++

            if (low != high) {
                if (nextSeed <= playerCount) {
                    bracket[high] = nextSeed
                }
                nextSeed++
            }

            low++
            high--
        }

        // La lista final contiene los números de siembra (1-based) en la posición del bracket.
        return bracket.toList()
    }




}