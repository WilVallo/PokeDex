package com.example.pokedex

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.bumptech.glide.Glide
import com.example.pokedex.adapter.EvolutionAdapter
import com.example.pokedex.data.ChainLink
import com.example.pokedex.data.EvolutionChainResponse
import com.example.pokedex.data.EvolutionStage
import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.data.PokemonSpeciesResponse
import com.example.pokedex.data.PokemonTypeEntry
import com.example.pokedex.databinding.ActivityPokemonDetailBinding
import com.example.pokedex.network.RetrofitClient
import com.example.pokedex.util.TypeColors
import com.example.pokedex.util.TypeIcons
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale


class PokemonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPokemonDetailBinding
    private lateinit var evolutionAdapter: EvolutionAdapter
    private var currentEvolutionIndex = 0

    private val pendingCalls = mutableListOf<Call<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPokemonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        evolutionAdapter = EvolutionAdapter(emptyList()) { stage ->
            loadPokemon(stage.name)
        }
        binding.rvEvolutions.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEvolutions.adapter = evolutionAdapter
        LinearSnapHelper().attachToRecyclerView(binding.rvEvolutions)

        binding.btnEvolutionPrev.setOnClickListener {
            if (currentEvolutionIndex > 0) {
                currentEvolutionIndex--
                binding.rvEvolutions.smoothScrollToPosition(currentEvolutionIndex)
            }
        }
        binding.btnEvolutionNext.setOnClickListener {
            if (currentEvolutionIndex < evolutionAdapter.itemCount - 1) {
                currentEvolutionIndex++
                binding.rvEvolutions.smoothScrollToPosition(currentEvolutionIndex)
            }
        }

        val name = intent.getStringExtra(EXTRA_POKEMON_NAME)
        if (name.isNullOrBlank()) {
            finish()
            return
        }

        loadPokemon(name)
    }

    private fun loadPokemon(nameOrId: String) {
        showLoading()
        cancelPendingCalls()

        val call = RetrofitClient.apiService.getPokemon(nameOrId.trim().lowercase(Locale.ROOT))
        pendingCalls.add(call)
        call.enqueue(object : Callback<PokemonResponse> {
            override fun onResponse(call: Call<PokemonResponse>, response: Response<PokemonResponse>) {
                val pokemon = response.body()
                if (!response.isSuccessful || pokemon == null) {
                    showError("Unable to load Pokémon.")
                    return
                }
                loadSpecies(pokemon)
            }

            override fun onFailure(call: Call<PokemonResponse>, t: Throwable) {
                if (!call.isCanceled) showError("Unable to connect to PokeAPI.")
            }
        })
    }

    private fun loadSpecies(pokemon: PokemonResponse) {
        val call = RetrofitClient.apiService.getPokemonSpecies(pokemon.id.toString())
        pendingCalls.add(call)
        call.enqueue(object : Callback<PokemonSpeciesResponse> {
            override fun onResponse(call: Call<PokemonSpeciesResponse>, response: Response<PokemonSpeciesResponse>) {
                val species = response.body()
                if (!response.isSuccessful || species == null) {
                    showError("Species information could not be loaded.")
                    return
                }
                displayPokemon(pokemon, species)
                loadEvolutionChain(species.evolutionChain.url, pokemon.name)
            }

            override fun onFailure(call: Call<PokemonSpeciesResponse>, t: Throwable) {
                if (!call.isCanceled) showError("Unable to retrieve species information.")
            }
        })
    }

    private fun loadEvolutionChain(url: String, currentName: String) {
        val call = RetrofitClient.apiService.getEvolutionChain(url)
        pendingCalls.add(call)
        call.enqueue(object : Callback<EvolutionChainResponse> {
            override fun onResponse(call: Call<EvolutionChainResponse>, response: Response<EvolutionChainResponse>) {
                val chain = response.body()?.chain ?: return
                fetchEvolutionSprites(flattenChain(chain), currentName)
            }

            override fun onFailure(call: Call<EvolutionChainResponse>, t: Throwable) {
                // Non-critical: leave the evolutions section hidden on failure.
            }
        })
    }


    private fun flattenChain(chain: ChainLink): List<String> {
        val names = mutableListOf(chain.species.name)
        var next = chain.evolvesTo.firstOrNull()
        while (next != null) {
            names.add(next.species.name)
            next = next.evolvesTo.firstOrNull()
        }
        return names
    }

    private fun fetchEvolutionSprites(names: List<String>, currentName: String) {
        var remaining = names.size
        val stages = arrayOfNulls<EvolutionStage>(names.size)

        names.forEachIndexed { index, name ->
            val call = RetrofitClient.apiService.getPokemon(name)
            pendingCalls.add(call)
            call.enqueue(object : Callback<PokemonResponse> {
                override fun onResponse(call: Call<PokemonResponse>, response: Response<PokemonResponse>) {
                    response.body()?.let {
                        stages[index] = EvolutionStage(
                            name = it.name,
                            spriteUrl = it.sprites.other?.officialArtwork?.frontDefault
                                ?: it.sprites.frontDefault
                        )
                    }
                    remaining--
                    if (remaining == 0) onEvolutionStagesLoaded(stages.filterNotNull(), currentName)
                }

                override fun onFailure(call: Call<PokemonResponse>, t: Throwable) {
                    remaining--
                    if (remaining == 0) onEvolutionStagesLoaded(stages.filterNotNull(), currentName)
                }
            })
        }
    }

    private fun onEvolutionStagesLoaded(stages: List<EvolutionStage>, currentName: String) {
        evolutionAdapter.submitList(stages)
        currentEvolutionIndex = stages.indexOfFirst { it.name == currentName }.coerceAtLeast(0)
        binding.rvEvolutions.scrollToPosition(currentEvolutionIndex)
        binding.evolutionsSection.visibility = if (stages.size > 1) View.VISIBLE else View.GONE
    }

    private fun displayPokemon(pokemon: PokemonResponse, species: PokemonSpeciesResponse) {
        val imageUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
            ?: pokemon.sprites.frontDefault

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(binding.ivPokemon)

        val primaryType = pokemon.types.minByOrNull { it.slot }?.type?.name ?: "normal"
        binding.imageBackground.backgroundTintList =
            ColorStateList.valueOf(getColor(TypeColors.colorFor(primaryType)))

        binding.tvPokemonName.text = formatName(pokemon.name)
        binding.tvPokemonId.text = "#" + pokemon.id.toString().padStart(4, '0')

        displayTypeBadges(pokemon.types)

        binding.tvDescription.text = species.flavorTextEntries
            .firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace("\n", " ")
            ?.replace("\u000C", " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: "No English description available."

        val heightMeters = pokemon.height / 10.0
        val weightKilograms = pokemon.weight / 10.0
        binding.tvHeight.text = "${formatDecimal(heightMeters)} m"
        binding.tvWeight.text = "${formatDecimal(weightKilograms)} kg"

        binding.tvCategory.text = species.genera
            .firstOrNull { it.language.name == "en" }
            ?.genus ?: "Unknown"

        val ability = pokemon.abilities.minByOrNull { it.slot }
        binding.tvAbility.text = ability?.let { formatName(it.ability.name) } ?: "Unknown"

        val genderRate = species.genderRate
        binding.tvGender.text = if (genderRate == -1) {
            "Genderless"
        } else {
            val female = genderRate / 8.0 * 100.0
            val male = 100.0 - female
            "Male ${formatDecimal(male)}%, Female ${formatDecimal(female)}%"
        }

        setStatPips(binding.hpPipsRow, binding.tvHpValue, pokemon, "hp")
        setStatPips(binding.attackPipsRow, binding.tvAttackValue, pokemon, "attack")
        setStatPips(binding.defensePipsRow, binding.tvDefenseValue, pokemon, "defense")
        setStatPips(binding.spAtkPipsRow, binding.tvSpAtkValue, pokemon, "special-attack")
        setStatPips(binding.spDefPipsRow, binding.tvSpDefValue, pokemon, "special-defense")
        setStatPips(binding.speedPipsRow, binding.tvSpeedValue, pokemon, "speed")

        hideLoading()
    }

    private fun displayTypeBadges(types: List<PokemonTypeEntry>) {
        val container = binding.typeBadgeContainer
        container.removeAllViews()

        val context = this
        val density = resources.displayMetrics.density
        val circleSize = (48 * density).toInt()
        val iconSize = (24 * density).toInt()
        val badgeMargin = (10 * density).toInt()

        types.sortedBy { it.slot }.forEach { entry ->
            val badge = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginStart = badgeMargin
                params.marginEnd = badgeMargin
                layoutParams = params
            }

            val typeColor = context.getColor(TypeColors.colorFor(entry.type.name))

            val circle = buildTypeBadgeCircle(context, circleSize, typeColor, TypeIcons.iconFor(entry.type.name), iconSize)

            val label = TextView(context).apply {
                text = entry.type.name.uppercase(Locale.ROOT)
                setTextColor(typeColor)
                textSize = 9f
                gravity = android.view.Gravity.CENTER
                setPadding(0, (4 * density).toInt(), 0, 0)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.press_start_2p)
            }

            badge.addView(circle)
            badge.addView(label)
            container.addView(badge)
        }
    }

    private fun buildTypeBadgeCircle(
        context: android.content.Context,
        size: Int,
        tintColor: Int,
        iconRes: Int,
        iconSize: Int
    ): android.widget.FrameLayout {
        val frame = android.widget.FrameLayout(context)
        frame.layoutParams = LinearLayout.LayoutParams(size, size)

        val bg = android.widget.ImageView(context)
        bg.layoutParams = ViewGroup.LayoutParams(size, size)
        bg.setImageResource(R.drawable.bg_type_badge_circle)
        bg.imageTintList = ColorStateList.valueOf(tintColor)
        frame.addView(bg)

        val icon = android.widget.ImageView(context)
        val iconParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize)
        iconParams.gravity = android.view.Gravity.CENTER
        icon.layoutParams = iconParams
        icon.setImageResource(iconRes)
        frame.addView(icon)

        return frame
    }

    private fun setStatPips(
        pipsRow: LinearLayout,
        valueView: TextView,
        pokemon: PokemonResponse,
        statName: String
    ) {
        val baseStat = pokemon.stats.firstOrNull { it.stat.name == statName }?.baseStat ?: 0
        val filledCount = Math.round(baseStat / 16.0).toInt().coerceIn(0, PIP_COUNT)
        valueView.text = "$filledCount/$PIP_COUNT"

        val color = when (statName) {
            "hp" -> getColor(R.color.stat_hp)
            "attack" -> getColor(R.color.stat_attack)
            "defense" -> getColor(R.color.stat_defense)
            "special-attack" -> getColor(R.color.stat_special_attack)
            "special-defense" -> getColor(R.color.stat_special_defense)
            "speed" -> getColor(R.color.stat_speed)
            else -> getColor(R.color.text_secondary)
        }

        pipsRow.removeAllViews()
        val density = resources.displayMetrics.density
        val pipMargin = (2 * density).toInt()

        for (i in 0 until PIP_COUNT) {
            val pip = View(this)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            params.marginEnd = if (i == PIP_COUNT - 1) 0 else pipMargin
            pip.layoutParams = params

            if (i < filledCount) {
                pip.setBackgroundResource(R.drawable.pip_filled)
                pip.backgroundTintList = ColorStateList.valueOf(color)
            } else {
                pip.setBackgroundResource(R.drawable.pip_empty)
            }
            pipsRow.addView(pip)
        }
    }

    private fun formatName(value: String): String {
        return value.split("-", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollContent.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun cancelPendingCalls() {
        pendingCalls.forEach { it.cancel() }
        pendingCalls.clear()
    }

    override fun onDestroy() {
        cancelPendingCalls()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_POKEMON_NAME = "extra_pokemon_name"
        private const val PIP_COUNT = 15
    }
}
