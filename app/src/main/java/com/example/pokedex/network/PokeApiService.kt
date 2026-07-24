package com.example.pokedex.network

import com.example.pokedex.data.EvolutionChainResponse
import com.example.pokedex.data.PokemonListResponse
import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.data.PokemonSpeciesResponse
import com.example.pokedex.data.TypeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PokeApiService {

    @GET("pokemon/{nameOrId}")
    fun getPokemon(
        @Path("nameOrId") nameOrId: String
    ): Call<PokemonResponse>

    @GET("pokemon-species/{nameOrId}")
    fun getPokemonSpecies(
        @Path("nameOrId") nameOrId: String
    ): Call<PokemonSpeciesResponse>

    @GET("pokemon")
    fun getPokemonList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<PokemonListResponse>

    @GET("type/{name}")
    fun getPokemonByType(
        @Path("name") name: String
    ): Call<TypeResponse>

    // Species responses hand back an absolute evolution-chain URL, so this
    // call takes the full URL directly rather than building one from a path.
    @GET
    fun getEvolutionChain(
        @Url url: String
    ): Call<EvolutionChainResponse>
}
