package com.example.quickbracket.feature.bracket_details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentBracketDetailsBinding

import com.example.quickbracket.databinding.ItemSetBinding // Asume que creaste un binding para match_set_template
import com.example.quickbracket.model.MatchSet

/**
 * Fragmento que muestra los detalles de una bracket y genera su estructura visual.
 */
class BracketDetailsFragment : Fragment() {

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
        Log.d("BracketDetails", "BracketName: ${bracket.name} \n BracketType: ${bracket.type} \n Sets Count: ${bracket.sets.size}")

        // Lógica principal para dibujar el bracket
        buildBracketView(bracket.sets)
    }

    /**
     * Construye dinámicamente la vista de la bracket (tablero del torneo)
     * basándose en la lista de Sets agrupados por ronda.
     */
    private fun buildBracketView(sets: List<MatchSet>) {

        val setsByRound = sets.groupBy { it.roundName }
        val inflater = LayoutInflater.from(context)
        val bracketContainer = binding.bracketContainer

        setsByRound.keys.forEachIndexed { index, roundName ->
            val roundSets = setsByRound[roundName] ?: emptyList()

            val roundContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.round_spacing) // Define round_spacing en dimens.xml (ej. 40dp)
                }
                orientation = LinearLayout.VERTICAL
                gravity = if (roundName == "Final") android.view.Gravity.CENTER else android.view.Gravity.TOP
            }

            val roundTitle = TextView(context).apply {
                text = roundName
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.round_title_padding))
            }
            roundContainer.addView(roundTitle)

            // 2. Inflar cada SET (partido) dentro de la columna de la ronda
            roundSets.forEach { matchSet ->
                // Inflar la plantilla de un solo set (match_set_template)
                val setBinding = ItemSetBinding.inflate(inflater, roundContainer, false)

                // Rellenar la información básica (puedes expandir esto)
                setBinding.player1Name.text = "Set ${matchSet.setId} P1" // Placeholder
                setBinding.player2Name.text = "Set ${matchSet.setId} P2" // Placeholder

                // Agregar el set inflado al contenedor de la ronda
                roundContainer.addView(setBinding.root)

                // 3. Añadir el espacio vertical (Separador/Espaciador)
                // El tamaño del espacio debe ser inversamente proporcional al número de sets
                // y alinearse con las conexiones de la siguiente ronda.

                val setsInNextRound = if (index < setsByRound.size - 1) {
                    setsByRound.values.toList()[index + 1].size
                } else {
                    0
                }

                // Cálculo básico del espaciado: Duplicar el espacio por cada salto de ronda
                // Esto ayuda a que los sets se alineen correctamente
                val baseSpacing = resources.getDimensionPixelSize(R.dimen.set_base_spacing) // Ej: 20dp
                val roundFactor = if (setsInNextRound > 0) roundSets.size / setsInNextRound else 1
                val verticalSpace = baseSpacing * roundFactor

                // Asegurar que no se añada espacio después del último set de la final
                if (matchSet.setId != roundSets.last().setId || roundName != "Final") {
                    val spacer = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            verticalSpace
                        )
                    }
                    roundContainer.addView(spacer)
                }
            }

            // 4. Agregar la columna de la ronda al contenedor principal
            bracketContainer.addView(roundContainer)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
