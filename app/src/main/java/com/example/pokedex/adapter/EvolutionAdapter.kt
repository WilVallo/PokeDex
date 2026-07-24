package com.example.pokedex.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokedex.data.EvolutionStage
import com.example.pokedex.databinding.ItemEvolutionBinding
import java.util.Locale

class EvolutionAdapter(
    private var items: List<EvolutionStage>,
    private val onClick: (EvolutionStage) -> Unit
) : RecyclerView.Adapter<EvolutionAdapter.ViewHolder>() {

    fun submitList(newItems: List<EvolutionStage>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEvolutionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemEvolutionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stage: EvolutionStage, onClick: (EvolutionStage) -> Unit) {
            binding.tvEvolutionName.text = stage.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }

            Glide.with(binding.root.context)
                .load(stage.spriteUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivEvolutionSprite)

            binding.root.setOnClickListener { onClick(stage) }
        }
    }
}
