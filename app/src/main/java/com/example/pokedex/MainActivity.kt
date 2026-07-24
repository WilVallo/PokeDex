package com.example.pokedex

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pokedex.adapter.PokemonAdapter
import com.example.pokedex.data.PokemonListResponse
import com.example.pokedex.data.PokemonResponse
import com.example.pokedex.data.TypeResponse
import com.example.pokedex.databinding.ActivityMainBinding
import com.example.pokedex.network.RetrofitClient
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PokemonAdapter

    private val allPokemon = mutableListOf<PokemonResponse>()
    private var currentTypeFilter: String = "all"
    private val pendingCalls = mutableListOf<Call<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PokemonAdapter(emptyList()) { pokemon ->
            val intent = Intent(this, PokemonDetailActivity::class.java)
            intent.putExtra(PokemonDetailActivity.EXTRA_POKEMON_NAME, pokemon.name)
            startActivity(intent)
        }

        binding.rvPokemonGrid.layoutManager = GridLayoutManager(this, 2)
        binding.rvPokemonGrid.adapter = adapter

        binding.chipGroupTypes.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) }
            currentTypeFilter = (chip?.tag as? String) ?: "all"
            loadPokemonForCurrentFilter()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadPokemonForCurrentFilter()
    }

    private fun loadPokemonForCurrentFilter() {
        showLoading()
        cancelPendingCalls()
        allPokemon.clear()

        if (currentTypeFilter == "all") {
            val call = RetrofitClient.apiService.getPokemonList(DEFAULT_LIMIT, 0)
            pendingCalls.add(call)
            call.enqueue(object : Callback<PokemonListResponse> {
                override fun onResponse(
                    call: Call<PokemonListResponse>,
                    response: Response<PokemonListResponse>
                ) {
                    val names = response.body()?.results?.map { it.name } ?: emptyList()
                    fetchPokemonDetails(names)
                }

                override fun onFailure(call: Call<PokemonListResponse>, t: Throwable) {
                    if (!call.isCanceled) showError("Unable to load Pokémon list.")
                }
            })
        } else {
            val call = RetrofitClient.apiService.getPokemonByType(currentTypeFilter)
            pendingCalls.add(call)
            call.enqueue(object : Callback<TypeResponse> {
                override fun onResponse(
                    call: Call<TypeResponse>,
                    response: Response<TypeResponse>
                ) {
                    val names = response.body()?.pokemon
                        ?.map { it.pokemon.name }
                        ?.take(DEFAULT_LIMIT) ?: emptyList()
                    fetchPokemonDetails(names)
                }

                override fun onFailure(call: Call<TypeResponse>, t: Throwable) {
                    if (!call.isCanceled) showError("Unable to load Pokémon for that type.")
                }
            })
        }
    }

    private fun fetchPokemonDetails(names: List<String>) {
        if (names.isEmpty()) {
            showEmpty()
            return
        }

        var remaining = names.size
        val results = mutableListOf<PokemonResponse>()

        names.forEach { name ->
            val call = RetrofitClient.apiService.getPokemon(name)
            pendingCalls.add(call)
            call.enqueue(object : Callback<PokemonResponse> {
                override fun onResponse(call: Call<PokemonResponse>, response: Response<PokemonResponse>) {
                    response.body()?.let { results.add(it) }
                    remaining--
                    if (remaining == 0) onAllDetailsLoaded(results)
                }

                override fun onFailure(call: Call<PokemonResponse>, t: Throwable) {
                    if (!call.isCanceled) {
                        remaining--
                        if (remaining == 0) onAllDetailsLoaded(results)
                    }
                }
            })
        }
    }

    private fun onAllDetailsLoaded(results: List<PokemonResponse>) {
        allPokemon.clear()
        allPokemon.addAll(results.sortedBy { it.id })
        hideLoading()
        applySearchFilter(binding.etSearch.text?.toString().orEmpty())
    }

    private fun applySearchFilter(query: String) {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        val filtered = if (trimmed.isEmpty()) {
            allPokemon
        } else {
            allPokemon.filter { it.name.contains(trimmed) }
        }
        adapter.submitList(filtered)
        binding.tvPokemonCount.text = "${filtered.size} Pokemons"
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyState.text = "No Pokemon found."
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvPokemonGrid.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.rvPokemonGrid.visibility = View.VISIBLE
    }

    private fun showEmpty() {
        hideLoading()
        adapter.submitList(emptyList())
        binding.tvPokemonCount.text = "0 Pokemons"
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "No Pokemon found."
    }

    private fun showError(message: String) {
        hideLoading()
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = message
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
        private const val DEFAULT_LIMIT = 20
    }
}
