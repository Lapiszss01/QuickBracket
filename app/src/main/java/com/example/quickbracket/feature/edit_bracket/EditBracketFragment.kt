package com.example.quickbracket.feature.edit_bracket

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
import com.example.quickbracket.databinding.FragmentEditBracketBinding
import com.example.quickbracket.model.Bracket
import com.example.quickbracket.model.BracketPath
import com.example.quickbracket.model.MatchSet
import com.example.quickbracket.model.Player
import com.google.android.material.textfield.TextInputEditText


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class EditBracketFragment : Fragment() {

    private var _binding: FragmentEditBracketBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBracketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}