package com.example.quickbracket.feature.createbracket

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentCreateBracketBinding
import androidx.navigation.fragment.findNavController


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CreateBracketFragment : Fragment() {

    private val bracketViewModel: BracketViewModel by viewModels()

    private var _binding: FragmentCreateBracketBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBracketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createBracketButton.setOnClickListener {
            val bracketName = binding.bracketNameEditText.text.toString()
            bracketViewModel.saveNewBracket(bracketName)
        }

        // Observa el mensaje de estado para retroalimentación
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

}