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
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentCreateBracketBinding
import androidx.navigation.fragment.findNavController
import com.example.quickbracket.model.Bracket
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
        //TODO: Next bracket types
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

        val bracketTypes = BracketType.entries.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            bracketTypes
        )
        binding.bracketTypeAutoCompleteTextView.setAdapter(adapter)

        val playerCountEditText = binding.etPlayerCount as? TextInputEditText
        playerCountEditText?.doAfterTextChanged { editable ->
            val playerCountText = editable?.toString()
            val count = playerCountText.orEmpty().toIntOrNull() ?: 0
            updatePlayerTextViews(count)
        }

        //Create button
        binding.createBracketButton.setOnClickListener {
            val players = getRegisteredPlayerNames()
            bracketViewModel.createBracket(players)
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        bracketViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        bracketViewModel.bracketCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                binding.bracketNameEditText.text?.clear()
                findNavController().navigate(R.id.action_CreateBracketFragment_to_HomeFragment)
            }
        }

        bracketViewModel.generatedBracketSets.observe(viewLifecycleOwner) { bracketSets ->
            val bracketName = binding.bracketNameEditText.text.toString()
            val bracketType = binding.bracketTypeAutoCompleteTextView.text.toString()
            val players = getRegisteredPlayerNames()

            val finalBracket = Bracket(
                name = bracketName,
                type = bracketType,
                sets = bracketSets,
                entrants = players
            )
            bracketViewModel.saveNewBracket(finalBracket, binding.etPlayerCount.text.toString())
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
        for ((index, itemView) in playerViews.withIndex()) {
            val seed = index + 1

            val editText = itemView.findViewById<TextInputEditText>(R.id.player_name_edit_text)
            val playerName = editText?.text?.toString()?.trim()

            if (!playerName.isNullOrEmpty()) {
                val player = Player(name = playerName, seed = seed)
                players.add(player)
            }
        }
        return players
    }
}