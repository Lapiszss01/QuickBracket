package com.example.quickbracket.feature.bracket_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.quickbracket.R
import com.example.quickbracket.databinding.FragmentCreateBracketBinding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.quickbracket.databinding.FragmentBracketDetailsBinding
import com.example.quickbracket.feature.create_bracket.BracketViewModel


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class BracketDetailsFragment : Fragment() {

    private val args: BracketDetailsFragmentArgs by navArgs()

    private var _binding: FragmentBracketDetailsBinding? = null
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

        binding.tvTest.text = args.bracketId

    }

}