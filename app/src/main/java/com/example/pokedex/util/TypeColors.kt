package com.example.pokedex.util

import com.example.pokedex.R

object TypeColors {

    fun colorFor(typeName: String): Int {
        return when (typeName.lowercase()) {
            "fire" -> R.color.type_fire
            "electric" -> R.color.type_electric
            "water" -> R.color.type_water
            "grass" -> R.color.type_grass
            "normal" -> R.color.type_normal
            "poison" -> R.color.type_poison
            "ground" -> R.color.type_ground
            "rock" -> R.color.type_rock
            "bug" -> R.color.type_bug
            "ghost" -> R.color.type_ghost
            "steel" -> R.color.type_steel
            "psychic" -> R.color.type_psychic
            "ice" -> R.color.type_ice
            "dragon" -> R.color.type_dragon
            "dark" -> R.color.type_dark
            "fairy" -> R.color.type_fairy
            "fighting" -> R.color.type_fighting
            "flying" -> R.color.type_flying
            else -> R.color.type_normal
        }
    }
}
