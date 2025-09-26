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

class BracketAdapter : ListAdapter<Bracket, BracketAdapter.BracketViewHolder>(BracketDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class BracketViewHolder(private val binding: ItemBracketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bracket: Bracket) {

            binding.textViewBracketName.text = bracket.name

            // TODO: Configura el listener de clic aquí si lo necesitas
            itemView.setOnClickListener {
                // Navegar a la vista de detalle del Bracket usando bracket.id
                Log.d("Home","Nombre: ${bracket.name} Tipo:${bracket.type} Sets:${bracket.sets}")
            }
        }
    }

    // 2. onCreateViewHolder: Infla el layout del elemento de lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BracketViewHolder {
        val binding = ItemBracketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BracketViewHolder(binding)
    }

    // 3. onBindViewHolder: Une los datos a las vistas
    override fun onBindViewHolder(holder: BracketViewHolder, position: Int) {
        val bracket = getItem(position)
        holder.bind(bracket)
    }
}

// 4. DiffUtil: Mejora el rendimiento del RecyclerView
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