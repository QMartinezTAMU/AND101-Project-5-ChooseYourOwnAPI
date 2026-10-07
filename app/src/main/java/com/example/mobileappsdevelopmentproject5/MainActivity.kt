package com.example.mobileappsdevelopmentproject5

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.example.mobileappsdevelopmentproject5.databinding.ActivityMainBinding
import okhttp3.Headers
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val client = AsyncHttpClient()
    private var currentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen below the status bar and above the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Each tap requests a different entry. This version explores IDs 1 to 151.
        binding.nextButton.setOnClickListener {
            val nextId = if (currentId >= 151) 1 else currentId + 1
            requestPokemon(nextId)
        }

        // Reuse a locally cached response when Android recreates this screen.
        val restoredId = savedInstanceState?.getInt("current_id", 0) ?: 0
        val cached = getSharedPreferences("pokemon_cache", MODE_PRIVATE)
            .getString("pokemon_$restoredId", null)
        if (restoredId > 0 && cached != null) {
            try {
                displayPokemon(JSONObject(cached))
                binding.statusText.setText(R.string.p5_ready)
            } catch (error: JSONException) {
                requestPokemon(restoredId)
            }
        } else {
            // Load the first entry automatically when the app starts.
            requestPokemon(if (restoredId > 0) restoredId else 1)
        }
    }

    private fun requestPokemon(id: Int) {
        binding.nextButton.isEnabled = false
        binding.statusText.setText(R.string.p5_loading)
        val url = "https://pokeapi.co/api/v2/pokemon/$id/"

        // AsyncHttpClient sends the request without blocking the UI thread.
        client.get(url, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                if (isDestroyed || isFinishing) return
                try {
                    // The root response is a JSON object, not a JSON array.
                    val pokemon = json.jsonObject
                        ?: throw JSONException("Expected a JSON object")
                    displayPokemon(pokemon)

                    // Cache each retrieved resource locally, as PokéAPI requests.
                    getSharedPreferences("pokemon_cache", MODE_PRIVATE)
                        .edit().putString("pokemon_$id", pokemon.toString()).apply()
                    binding.statusText.setText(R.string.p5_ready)
                } catch (error: JSONException) {
                    binding.statusText.setText(R.string.p5_parse_error)
                } finally {
                    binding.nextButton.isEnabled = true
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String,
                throwable: Throwable?
            ) {
                if (isDestroyed || isFinishing) return
                // Leave any previously loaded data visible and allow another attempt.
                binding.statusText.setText(R.string.p5_network_error)
                binding.nextButton.isEnabled = true
            }
        })
    }

    private fun displayPokemon(pokemon: JSONObject) {
        // Read all required fields before changing any of the displayed data.
        val id = pokemon.getInt("id")
        val name = pokemon.getString("name").replaceFirstChar { it.titlecase(Locale.US) }

        // PokéAPI returns height in decimetres and weight in hectograms.
        // Divide by 10 to display metres and kilograms.
        val height = pokemon.getInt("height") / 10.0
        val weight = pokemon.getInt("weight") / 10.0
        val heightText = String.format(Locale.US, "%.1f m", height)
        val weightText = String.format(Locale.US, "%.1f kg", weight)

        binding.pokemonName.text = name
        binding.pokemonId.text = getString(R.string.p5_id_format, id)
        binding.pokemonHeight.text = heightText
        binding.pokemonWeight.text = weightText
        currentId = id
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("current_id", currentId)
        super.onSaveInstanceState(outState)
    }
}
