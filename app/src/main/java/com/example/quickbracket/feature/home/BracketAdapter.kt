package com.example.quickbracket.feature.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quickbracket.R
import com.example.quickbracket.databinding.ItemBracketBinding
import com.example.quickbracket.model.Bracket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.findNavController

class BracketAdapter : ListAdapter<Bracket, BracketAdapter.BracketViewHolder>(BracketDiffCallback()) {

    class BracketViewHolder(private val binding: ItemBracketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bracket: Bracket) {

            binding.textViewBracketName.text = bracket.name

            // TODO: Configura el listener de clic aquí si lo necesitas
            itemView.setOnClickListener {

                val action = HomeFragmentDirections.actionHomeFragmentToBracketDetailsFragment(
                    bracket = bracket
                )
                // 2. Navega con la acción generada
                itemView.findNavController().navigate(action)
                Log.d("Home","Nombre: ${bracket.name} Tipo:${bracket.type} Sets:${bracket.sets}")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BracketViewHolder {
        val binding = ItemBracketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BracketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BracketViewHolder, position: Int) {
        val bracket = getItem(position)
        holder.bind(bracket)
    }
}

class BracketDiffCallback : DiffUtil.ItemCallback<Bracket>() {
    override fun areItemsTheSame(oldItem: Bracket, newItem: Bracket): Boolean {
        // Comprueba si son el mismo Bracket (usando el ID único)
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Bracket, newItem: Bracket): Boolean {
        // Comprueba si el contenido es idéntico
        return oldItem == newItem
    }
}