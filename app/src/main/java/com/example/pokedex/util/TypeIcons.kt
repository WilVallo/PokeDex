package com.example.pokedex.util

import com.example.pokedex.R


object TypeIcons {

    fun iconFor(typeName: String): Int {
        return when (typeName.lowercase()) {
            "fire" -> R.drawable.ic_type_fire
            "water" -> R.drawable.ic_type_water
            "grass" -> R.drawable.ic_type_grass
            "electric" -> R.drawable.ic_type_electric
            "normal" -> R.drawable.ic_type_normal
            "poison" -> R.drawable.ic_type_poison
            "ground" -> R.drawable.ic_type_ground
            "rock" -> R.drawable.ic_type_rock
            "bug" -> R.drawable.ic_type_bug
            "ghost" -> R.drawable.ic_type_ghost
            "steel" -> R.drawable.ic_type_steel
            "psychic" -> R.drawable.ic_type_psychic
            "ice" -> R.drawable.ic_type_ice
            "dragon" -> R.drawable.ic_type_dragon
            "dark" -> R.drawable.ic_type_dark
            "fairy" -> R.drawable.ic_type_fairy
            "fighting" -> R.drawable.ic_type_fighting
            "flying" -> R.drawable.ic_type_flying
            else -> R.drawable.ic_type_normal
        }
    }
}
