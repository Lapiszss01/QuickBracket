package com.example.quickbracket.feature.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quickbracket.R
import com.example.quickbracket.databinding.ItemBracketBinding
import com.example.quickbracket.model.Bracket
import androidx.navigation.findNavController

// Definición de la interfaz (fuera del Adapter)
interface BracketActionListener {
    fun onEditBracket(bracket: Bracket)
    fun onDeleteBracket(bracket: Bracket)
}

// El adaptador ahora recibe el listener en el constructor
class BracketAdapter(private val listener: BracketActionListener) :
    ListAdapter<Bracket, BracketAdapter.BracketViewHolder>(BracketDiffCallback()) {

    // El ViewHolder también recibe el listener
    class BracketViewHolder(
        private val binding: ItemBracketBinding,
        private val listener: BracketActionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bracket: Bracket) {

            binding.textViewBracketName.text = bracket.name

            // 1. Listener para la navegación al detalle (al hacer clic en el item completo)
            itemView.setOnClickListener {
                val action = HomeFragmentDirections.actionHomeFragmentToBracketDetailsFragment(
                    bracket = bracket
                )
                itemView.findNavController().navigate(action)
                Log.d("Home", "Nombre: ${bracket.name}")
            }

            // 2. Lógica del botón de Configuración
            binding.settingButton.setOnClickListener {
                showPopupMenu(bracket)
            }
        }

        private fun showPopupMenu(bracket: Bracket) {
            // El menú se ancla al botón de configuración
            val popup = PopupMenu(binding.settingButton.context, binding.settingButton)
            popup.menuInflater.inflate(R.menu.bracket_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        listener.onEditBracket(bracket) // Llama al método del Fragment
                        true
                    }
                    R.id.menu_delete -> {
                        listener.onDeleteBracket(bracket) // Llama al método del Fragment
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BracketViewHolder {
        val binding = ItemBracketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Pasa el listener al ViewHolder
        return BracketViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: BracketViewHolder, position: Int) {
        val bracket = getItem(position)
        holder.bind(bracket)
    }
}

class BracketDiffCallback : DiffUtil.ItemCallback<Bracket>() {
    override fun areItemsTheSame(oldItem: Bracket, newItem: Bracket): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Bracket, newItem: Bracket): Boolean {
        return oldItem == newItem
    }
}