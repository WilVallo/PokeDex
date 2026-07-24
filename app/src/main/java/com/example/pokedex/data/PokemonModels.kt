package com.example.pokedex.data

import com.google.gson.annotations.SerializedName

data class PokemonResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: PokemonSprites,
    val types: List<PokemonTypeEntry>,
    val abilities: List<PokemonAbilityEntry>,
    val stats: List<PokemonStatEntry>
)

data class PokemonSprites(
    @SerializedName("front_default")
    val frontDefault: String?,

    val other: OtherSprites?
)

data class OtherSprites(
    @SerializedName("official-artwork")
    val officialArtwork: OfficialArtwork?
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String?
)

data class PokemonTypeEntry(
    val slot: Int,
    val type: NamedApiResource
)

data class PokemonAbilityEntry(
    val ability: NamedApiResource,

    @SerializedName("is_hidden")
    val isHidden: Boolean,

    val slot: Int
)

data class PokemonStatEntry(
    @SerializedName("base_stat")
    val baseStat: Int,
    val effort: Int,
    val stat: NamedApiResource
)

data class NamedApiResource(
    val name: String,
    val url: String
)

data class PokemonSpeciesResponse(
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntry>,
    val genera: List<GenusEntry>,

    @SerializedName("gender_rate")
    val genderRate: Int,

    @SerializedName("evolution_chain")
    val evolutionChain: EvolutionChainRef
)

data class EvolutionChainRef(
    val url: String
)

data class FlavorTextEntry(
    @SerializedName("flavor_text")
    val flavorText: String,
    val language: NamedApiResource,
    val version: NamedApiResource
)

data class GenusEntry(
    val genus: String,
    val language: NamedApiResource
)

// ---- Home / list screen ----

data class PokemonListResponse(
    val count: Int,
    val results: List<NamedApiResource>
)

// ---- Type filter ----

data class TypeResponse(
    val pokemon: List<TypePokemonEntry>
)

data class TypePokemonEntry(
    val pokemon: NamedApiResource
)

// ---- Evolution chain ----

data class EvolutionChainResponse(
    val chain: ChainLink
)

data class ChainLink(
    val species: NamedApiResource,

    @SerializedName("evolves_to")
    val evolvesTo: List<ChainLink>
)

// UI-only model (not from the network) used to populate the evolution carousel.
data class EvolutionStage(
    val name: String,
    val spriteUrl: String?
)
