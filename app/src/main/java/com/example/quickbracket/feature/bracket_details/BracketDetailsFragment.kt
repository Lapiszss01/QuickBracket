package com.example.quickbracket.feature.bracket_details

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentBracketDetailsBinding

import com.example.quickbracket.databinding.ItemSetBinding // Asume que creaste un binding para match_set_template
import com.example.quickbracket.model.MatchSet

/**
 * Fragmento que muestra los detalles de una bracket y genera su estructura visual.
 */
class BracketDetailsFragment : Fragment(), OnMatchSetClickListener {

    private val args: BracketDetailsFragmentArgs by navArgs()

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
        Log.d("BracketDetails", "BracketName: ${bracket.name} \n BracketType: ${bracket.type}")
        for (set in bracket.sets){
            Log.d("BracketDetails", "$set")
        }


        setupBracketRecyclerView(bracket.sets)
    }

    private fun setupBracketRecyclerView(sets: List<MatchSet>) {

        val setsByRound = sets.groupBy { it.roundName }

        // 1. Configurar el LayoutManager como horizontal
        binding.bracketContainerRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // 2. Crear y asignar el adaptador
        val roundAdapter = RoundAdapter(setsByRound, this)
        binding.bracketContainerRecycler.adapter = roundAdapter

        // Nota: Es posible que necesites un ItemDecoration para el espaciado entre rondas
        // si el paddingEnd del item_round_layout no es suficiente o quieres separarlo del borde.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMatchSetClicked(matchSet: MatchSet) {
        // 1. Log para verificar que funciona
        Log.d("SetClick", "Set clickeado. ID: ${matchSet.setId}")

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Set")
            .setMessage("¿Deseas editar el set entre ${matchSet.player1?.name} y ${matchSet.player2?.name}?")
            .setPositiveButton("Sí") { dialog, which ->
                // Lógica para editar el set
            }
            .setNegativeButton("No", null)
            .show()
    }
}
