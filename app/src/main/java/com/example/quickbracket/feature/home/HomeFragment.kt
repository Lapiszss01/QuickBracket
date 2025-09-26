package com.example.quickbracket.feature.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.quickbracket.databinding.FragmentHomeBinding
import com.example.quickbracket.feature.createbracket.BracketViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickbracket.R

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private val bracketViewModel: BracketViewModel by activityViewModels()

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

        binding.fab.setOnClickListener { view ->
            Log.d("Home","fab button")
            findNavController().navigate(R.id.action_HomeFragment_to_CreateBracketFragment)
        }

    }

    private fun setupRecyclerView() {
        bracketAdapter = BracketAdapter()

        binding.recyclerViewBrackets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bracketAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeBrackets() {

        bracketViewModel.allBracketsLiveData.observe(viewLifecycleOwner) { bracketsList ->

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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBrackets.adapter = null
        _binding = null
    }
}