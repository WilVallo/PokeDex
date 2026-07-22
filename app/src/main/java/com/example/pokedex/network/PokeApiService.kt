package com.example.pokedex.network

import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.data.PokemonSpeciesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface PokeApiService {

    @GET("pokemon/{nameOrId}")
    fun getPokemon(
        @Path("nameOrId") nameOrId: String
    ): Call<PokemonResponse>

    @GET("pokemon-species/{nameOrId}")
    fun getPokemonSpecies(
        @Path("nameOrId") nameOrId: String
    ): Call<PokemonSpeciesResponse>
}