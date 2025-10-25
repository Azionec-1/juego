package com.example.juego

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TiendaActivity : AppCompatActivity() {
    private var jugadorId: Int = -1
    private lateinit var tvPuntajeDisponible: TextView
    private lateinit var rvPokemons: RecyclerView
    private lateinit var btnVolver: Button
    private lateinit var adapter: PokemonAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private var puntajeActual: Int = 0
    private var pokemonsComprados = setOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tienda)

        // Inicializar vistas
        tvPuntajeDisponible = findViewById(R.id.tvPuntajeDisponible)
        rvPokemons = findViewById(R.id.rvPokemons)
        btnVolver = findViewById(R.id.btnVolver)

        // Inicializar base de datos
        // Inicializar base de datos
        databaseHelper = DatabaseHelper(this)

// Obtener jugadorId desde SharedPreferences
        val prefs = getSharedPreferences("PokemonPrefs", MODE_PRIVATE)
        jugadorId = prefs.getInt("jugador_id", -1)

// Validar que haya una sesión activa
        if (jugadorId == -1) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            finish()
            return
        }

// Obtener puntaje actual desde MainActivity
        puntajeActual = intent.getIntExtra("PUNTAJE_ACTUAL", 0)
        // Configurar RecyclerView
        configurarRecyclerView()

        // Cargar datos
        cargarDatos()

        // Botón volver
        btnVolver.setOnClickListener {
            devolverPuntaje()
        }
    }

    private fun configurarRecyclerView() {
        adapter = PokemonAdapter(
            pokemons = emptyList(),
            pokemonsComprados = setOf(),
            puntajeActual = puntajeActual
        ) { pokemon ->
            comprarPokemon(pokemon)
        }

        rvPokemons.layoutManager = LinearLayoutManager(this)
        rvPokemons.adapter = adapter
    }

    private fun cargarDatos() {
        // Cargar todos los pokémon
        val todosLosPokemons = databaseHelper.obtenerTodosLosPokemons()

        // Cargar pokémon ya comprados POR ESTE JUGADOR
        val idsComprados = databaseHelper.obtenerPokemonsComprados(jugadorId)
        pokemonsComprados = idsComprados.toSet()

        // Actualizar UI
        tvPuntajeDisponible.text = "Puntos disponibles: $puntajeActual"
        adapter.actualizarDatos(todosLosPokemons, pokemonsComprados, puntajeActual)
    }
    private fun comprarPokemon(pokemon: Pokemon) {
        // Verificar si ya está comprado
        if (databaseHelper.estaComprado(jugadorId, pokemon.id)) {
            Toast.makeText(this, "Ya compraste este Pokémon", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar puntos suficientes
        if (puntajeActual < pokemon.precio) {
            Toast.makeText(this, "No tienes suficientes puntos", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Comprar ${pokemon.nombre}")
            .setMessage("¿Deseas comprar a ${pokemon.nombre} por ${pokemon.precio} puntos?")
            .setPositiveButton("Sí") { _, _ ->
                realizarCompra(pokemon)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun realizarCompra(pokemon: Pokemon) {
        // Restar puntos
        puntajeActual -= pokemon.precio

        // Guardar compra en base de datos
        val exito = databaseHelper.comprarPokemon(jugadorId, pokemon.id)

        if (exito) {
            Toast.makeText(this, "¡Compraste a ${pokemon.nombre}! 🎉", Toast.LENGTH_SHORT).show()

            // Recargar datos
            cargarDatos()
        } else {
            Toast.makeText(this, "Error al comprar", Toast.LENGTH_SHORT).show()
            puntajeActual += pokemon.precio // Devolver puntos
            // Actualizar puntaje en la base de datos
            databaseHelper.actualizarPuntajeJugador(jugadorId, puntajeActual)
        }
    }

    private fun devolverPuntaje() {
        val intent = Intent()
        intent.putExtra("PUNTAJE_ACTUALIZADO", puntajeActual)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        devolverPuntaje()
    }
}