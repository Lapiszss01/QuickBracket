package com.example.quickbracket.feature.bracket_details

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentBracketDetailsBinding

import com.example.quickbracket.databinding.ItemSetBinding // Asume que creaste un binding para match_set_template
import com.example.quickbracket.feature.create_bracket.CreateBracketViewModel
import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player

/**
 * Fragmento que muestra los detalles de una bracket y genera su estructura visual.
 */
class BracketDetailsFragment : Fragment(), OnMatchSetClickListener {

    private val args: BracketDetailsFragmentArgs by navArgs()

    private val bracketDetailsViewModel: BracketDetailsViewModel by viewModels()

    private var _binding: FragmentBracketDetailsBinding? = null
    // Esto asume que el contenedor principal en fragment_bracket_details.xml se llama bracket_container
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBracketDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bracket = args.bracket

        binding.tournamentNameTv.text = bracket.name

        Log.d("BracketDetails", "BracketName: ${bracket.name} \n BracketType: ${bracket.type}")
        for (set in bracket.sets){
            Log.d("BracketDetails", "$set")
        }
        setupBracketRecyclerView(bracket.sets)

    }

    private fun setupBracketRecyclerView(sets: List<MatchSet>) {
        val setsByRound = sets.groupBy { it.roundName }
        binding.bracketContainerRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val roundAdapter = RoundAdapter(setsByRound, this)
        binding.bracketContainerRecycler.adapter = roundAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMatchSetClicked(matchSet: MatchSet) {

        Log.d("SetClick", "Set clickeado. ID: ${matchSet.setId}")

        val player1Name = matchSet.player1?.name ?: "Jugador 1"
        val player2Name = matchSet.player2?.name ?: "Jugador 2"
        var winnerName = ""

        AlertDialog.Builder(requireContext())
            .setTitle("Registrar Ganador del Set")
            .setMessage("¿Quién ganó el set entre $player1Name y $player2Name?")

            .setPositiveButton(player2Name) { dialog, which ->
                playerWinsRound(matchSet,matchSet.player2!!)
            }
            .setNegativeButton(player1Name) { dialog, which ->
                playerWinsRound(matchSet,matchSet.player1!!)
            }
            .setNeutralButton("Cancelar", null)
            .show()

    }

    fun playerWinsRound(matchSet: MatchSet, winnerPlayer: Player){

        val bracket = args.bracket
        val bracketSets = bracket.sets
        Log.d("SetResult", "${winnerPlayer.name} ganó el set ${matchSet.setId}")
        Log.d("SetResult", "Set: \n $matchSet")
        Log.d("SetResult", "Bracket: \n ${bracketSets}")

        if (matchSet.roundName == "Final") {
            winnerToast(requireContext(), winnerPlayer.name)
            Log.d("SetResult", "¡Ganador de la Bracket: ${winnerPlayer.name}!")
            return
        }

        val parentSetId = matchSet.nextMatchId
        if (parentSetId == null) {
            Log.d("SetResult", "Set ${matchSet.setId} es el último. Fin del recorrido.")
            return
        }

        val currentSet = bracketSets.find { it.setId == matchSet.setId }
        val parentSet = bracketSets.find { it.setId == parentSetId }
        if (parentSet == null) {
            Log.e("SetResult", "Error: No se encontró el set padre con ID $parentSetId")
            return
        }

        when {
            parentSet.player1 == null -> {
                parentSet.player1 = winnerPlayer.copy()

            }
            parentSet.player2 == null -> {
                parentSet.player2 = winnerPlayer.copy()
            }
            else -> {
                Log.w("SetResult", "Error: Ambos jugadores ya están asignados en el set padre ${parentSetId}.")
            }
        }

        currentSet?.winner = winnerPlayer
        currentSet?.isFinished = true

        val roundAdapter = binding.bracketContainerRecycler.adapter as? RoundAdapter
        roundAdapter?.notifyDataSetChanged()

        bracket.sets = bracketSets
        bracketDetailsViewModel.updateBracketSets(bracket)

    }

    fun winnerToast(context: Context, nombreJugador: String) {
        val mensaje = "¡Felicidades! ${nombreJugador} ha ganado la bracket."
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
    }

}
