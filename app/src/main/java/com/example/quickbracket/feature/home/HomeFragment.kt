package com.example.quickbracket.feature.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.quickbracket.databinding.FragmentHomeBinding
import com.example.quickbracket.feature.create_bracket.CreateBracketViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickbracket.R
import com.example.quickbracket.model.Bracket
import com.example.quickbracket.model.Player

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */



class HomeFragment : Fragment(), BracketActionListener {

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bracketAdapter: BracketAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeBrackets()
        observeStatusMessage()

        binding.fab.setOnClickListener { view ->
            findNavController().navigate(R.id.action_HomeFragment_to_CreateBracketFragment)
        }

    }

    private fun setupRecyclerView() {
        bracketAdapter = BracketAdapter(this)

        binding.recyclerViewBrackets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bracketAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeBrackets() {
        homeViewModel.allBracketsLiveData.observe(viewLifecycleOwner) { bracketsList ->
            bracketAdapter.submitList(bracketsList)
            if (bracketsList.isEmpty()) {
                binding.textViewEmptyList.visibility = View.VISIBLE
                binding.recyclerViewBrackets.visibility = View.GONE
            } else {
                binding.textViewEmptyList.visibility = View.GONE
                binding.recyclerViewBrackets.visibility = View.VISIBLE
            }
        }
    }

    private fun observeStatusMessage() {
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                homeViewModel.clearStatusMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBrackets.adapter = null
        _binding = null
    }

    override fun onEditBracket(bracket: Bracket) {
        //TODO Edit logic
    }

    override fun onDeleteBracket(bracket: Bracket) {
        homeViewModel.deleteBracket(bracket)
    }


}