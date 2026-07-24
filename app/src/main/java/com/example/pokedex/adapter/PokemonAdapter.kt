package com.example.pokedex.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokedex.R
import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.databinding.ItemPokemonCardBinding
import com.example.pokedex.util.TypeColors
import java.util.Locale

class PokemonAdapter(
    private var items: List<PokemonResponse>,
    private val onClick: (PokemonResponse) -> Unit
) : RecyclerView.Adapter<PokemonAdapter.ViewHolder>() {

    fun submitList(newItems: List<PokemonResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPokemonCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemPokemonCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pokemon: PokemonResponse, onClick: (PokemonResponse) -> Unit) {
            val context = binding.root.context

            binding.tvCardId.text = "#" + pokemon.id.toString().padStart(4, '0')
            binding.tvCardName.text = pokemon.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }

            val imageUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault

            Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivCardSprite)

            binding.typeDotsContainer.removeAllViews()
            val density = context.resources.displayMetrics.density
            val dotSize = (10 * density).toInt()
            val margin = (4 * density).toInt()

            pokemon.types.sortedBy { it.slot }.take(2).forEach { entry ->
                val dot = View(context)
                val params = LinearLayout.LayoutParams(dotSize, dotSize)
                params.marginEnd = margin
                dot.layoutParams = params
                dot.setBackgroundResource(R.drawable.circle_type_dot)
                dot.backgroundTintList = ColorStateList.valueOf(
                    context.getColor(TypeColors.colorFor(entry.type.name))
                )
                binding.typeDotsContainer.addView(dot)
            }

            binding.root.setOnClickListener { onClick(pokemon) }
        }
    }
}
