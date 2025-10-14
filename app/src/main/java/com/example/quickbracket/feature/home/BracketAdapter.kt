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

interface BracketActionListener {
    fun onEditBracket(bracket: Bracket)
    fun onDeleteBracket(bracket: Bracket)
}

class BracketAdapter(private val listener: BracketActionListener) :
    ListAdapter<Bracket, BracketAdapter.BracketViewHolder>(BracketDiffCallback()) {

    class BracketViewHolder(
        private val binding: ItemBracketBinding,
        private val listener: BracketActionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bracket: Bracket) {
            binding.textViewBracketName.text = bracket.name

            //Listener click on bracket item
            itemView.setOnClickListener {
                val action = HomeFragmentDirections.actionHomeFragmentToBracketDetailsFragment(
                    bracket = bracket
                )
                itemView.findNavController().navigate(action)
            }

            //Listener click on bracket's settings
            binding.settingButton.setOnClickListener {
                showPopupMenu(bracket)
            }
        }

        private fun showPopupMenu(bracket: Bracket) {
            val popup = PopupMenu(binding.settingButton.context, binding.settingButton)
            popup.menuInflater.inflate(R.menu.bracket_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    //TODO: Edit functions on brackets
                    /*R.id.menu_edit -> {
                        listener.onEditBracket(bracket)
                        true
                    }*/
                    R.id.menu_delete -> {
                        listener.onDeleteBracket(bracket)
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