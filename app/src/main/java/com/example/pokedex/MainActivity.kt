package com.example.pokedex

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.data.PokemonSpeciesResponse
import com.example.pokedex.databinding.ActivityMainBinding
import com.example.pokedex.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var pokemonCall: Call<PokemonResponse>? = null
    private var speciesCall: Call<PokemonSpeciesResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSearch.setOnClickListener {
            searchPokemon()
        }

        binding.etPokemonSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPokemon()
                true
            } else {
                false
            }
        }
    }

    private fun searchPokemon() {
        val searchValue = binding.etPokemonSearch.text
            .toString()
            .trim()
            .lowercase(Locale.ROOT)

        if (searchValue.isEmpty()) {
            binding.etPokemonSearch.error = "Enter a Pokémon name or ID."
            return
        }

        showLoading()

        pokemonCall?.cancel()
        speciesCall?.cancel()

        pokemonCall = RetrofitClient.apiService.getPokemon(searchValue)

        pokemonCall?.enqueue(object : Callback<PokemonResponse> {

            override fun onResponse(
                call: Call<PokemonResponse>,
                response: Response<PokemonResponse>
            ) {
                if (!response.isSuccessful) {
                    val message = if (response.code() == 404) {
                        "Pokémon not found."
                    } else {
                        "Unable to retrieve Pokémon. Error ${response.code()}."
                    }

                    showError(message)
                    return
                }

                val pokemon = response.body()

                if (pokemon == null) {
                    showError("The Pokémon information was empty.")
                    return
                }

                loadSpeciesInformation(pokemon)
            }

            override fun onFailure(
                call: Call<PokemonResponse>,
                throwable: Throwable
            ) {
                if (!call.isCanceled) {
                    showError(
                        throwable.localizedMessage
                            ?: "Unable to connect to PokeAPI."
                    )
                }
            }
        })
    }

    private fun loadSpeciesInformation(pokemon: PokemonResponse) {
        speciesCall = RetrofitClient.apiService
            .getPokemonSpecies(pokemon.id.toString())

        speciesCall?.enqueue(object : Callback<PokemonSpeciesResponse> {

            override fun onResponse(
                call: Call<PokemonSpeciesResponse>,
                response: Response<PokemonSpeciesResponse>
            ) {
                if (!response.isSuccessful) {
                    showError(
                        "Pokémon found, but species information could not be loaded."
                    )
                    return
                }

                val species = response.body()

                if (species == null) {
                    showError("The Pokémon species information was empty.")
                    return
                }

                displayPokemon(pokemon, species)
            }

            override fun onFailure(
                call: Call<PokemonSpeciesResponse>,
                throwable: Throwable
            ) {
                if (!call.isCanceled) {
                    showError(
                        throwable.localizedMessage
                            ?: "Unable to retrieve species information."
                    )
                }
            }
        })
    }

    private fun displayPokemon(
        pokemon: PokemonResponse,
        species: PokemonSpeciesResponse
    ) {
        val imageUrl =
            pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(binding.ivPokemon)

        val types = pokemon.types
            .sortedBy { it.slot }
            .joinToString(", ") {
                formatName(it.type.name)
            }

        val abilities = pokemon.abilities
            .sortedBy { it.slot }
            .joinToString(", ") {
                val abilityName = formatName(it.ability.name)

                if (it.isHidden) {
                    "$abilityName (Hidden)"
                } else {
                    abilityName
                }
            }

        val description = species.flavorTextEntries
            .firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace("\n", " ")
            ?.replace("\u000C", " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: "No English description available."

        val category = species.genera
            .firstOrNull { it.language.name == "en" }
            ?.genus
            ?: "Unknown"

        val heightMeters = pokemon.height / 10.0
        val weightKilograms = pokemon.weight / 10.0

        binding.tvPokemonId.text = "ID: #${pokemon.id}"
        binding.tvPokemonName.text = "Name: ${formatName(pokemon.name)}"
        binding.tvPokemonTypes.text = "Types: $types"
        binding.tvPokemonDescription.text = "Description: $description"

        binding.tvPokemonHeight.text =
            "Height: ${formatDecimal(heightMeters)} m"

        binding.tvPokemonWeight.text =
            "Weight: ${formatDecimal(weightKilograms)} kg"

        binding.tvPokemonCategory.text = "Category: $category"
        binding.tvPokemonAbilities.text = "Abilities: $abilities"

        binding.tvPokemonGender.text =
            "Gender: ${formatGender(species.genderRate)}"

        binding.tvPokemonHealth.text =
            createStatText(pokemon, "hp", "Health")

        binding.tvPokemonAttack.text =
            createStatText(pokemon, "attack", "Attack")

        binding.tvPokemonDefense.text =
            createStatText(pokemon, "defense", "Defense")

        binding.tvPokemonSpecialAttack.text =
            createStatText(
                pokemon,
                "special-attack",
                "Special Attack"
            )

        binding.tvPokemonSpecialDefense.text =
            createStatText(
                pokemon,
                "special-defense",
                "Special Defense"
            )

        binding.tvPokemonSpeed.text =
            createStatText(pokemon, "speed", "Speed")

        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.resultContainer.visibility = View.VISIBLE
        binding.btnSearch.isEnabled = true
    }

    private fun createStatText(
        pokemon: PokemonResponse,
        statName: String,
        displayName: String
    ): String {
        val stat = pokemon.stats.firstOrNull {
            it.stat.name == statName
        }

        return if (stat != null) {
            "$displayName: ${stat.baseStat}"
        } else {
            "$displayName: Unknown"
        }
    }

    private fun formatGender(genderRate: Int): String {
        if (genderRate == -1) {
            return "Genderless"
        }

        val femalePercentage = genderRate / 8.0 * 100.0
        val malePercentage = 100.0 - femalePercentage

        return "Male ${formatDecimal(malePercentage)}%, " +
                "Female ${formatDecimal(femalePercentage)}%"
    }

    private fun formatName(value: String): String {
        return value
            .split("-", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { character ->
                    if (character.isLowerCase()) {
                        character.titlecase(Locale.ROOT)
                    } else {
                        character.toString()
                    }
                }
            }
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.resultContainer.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.btnSearch.isEnabled = false
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.resultContainer.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.btnSearch.isEnabled = true

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        pokemonCall?.cancel()
        speciesCall?.cancel()

        super.onDestroy()
    }
}