package com.example.juego

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private val pokemonsDisponibles = mutableListOf<Int>() // IDs de drawables de pokémon comprados
        private val imagenMeowth = R.drawable.meowth
        private const val REQUEST_CODE_TIENDA = 100
        private const val PREFS_NAME = "PokemonPrefs"
        private const val PREF_JUGADOR_ID = "jugador_id"
    }

    private var score = 0
    private val imageArray = ArrayList<ImageView>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var timeText: TextView
    private lateinit var scoreText: TextView

    // Temporizador
    private var countDownTimer: CountDownTimer? = null
    private var tiempoRestanteMs: Long = 75_000 // valor inicial

    // BD y sesión
    private lateinit var databaseHelper: DatabaseHelper
    private var jugadorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Inicialización de BD y sesión ---
        databaseHelper = DatabaseHelper(this)
        // fuerza inicialización de esquema por si venías de una BD vieja
        databaseHelper.writableDatabase.close()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        jugadorId = prefs.getInt(PREF_JUGADOR_ID, -1) // debe haberse guardado en Login
        cargarPokemonsComprados()
        timeText = findViewById(R.id.timeText)
        scoreText = findViewById(R.id.scoreText)

        val imageView: ImageView = findViewById(R.id.imageView)
        val imageView2: ImageView = findViewById(R.id.imageView2)
        val imageView3: ImageView = findViewById(R.id.imageView3)
        val imageView4: ImageView = findViewById(R.id.imageView4)
        val imageView5: ImageView = findViewById(R.id.imageView5)
        val imageView6: ImageView = findViewById(R.id.imageView6)
        val imageView7: ImageView = findViewById(R.id.imageView7)
        val imageView8: ImageView = findViewById(R.id.imageView8)
        val imageView9: ImageView = findViewById(R.id.imageView9)

        val allImages = listOf(
            imageView, imageView2, imageView3,
            imageView4, imageView5, imageView6,
            imageView7, imageView8, imageView9
        )
        allImages.forEach { it.setImageResource(R.drawable.pikachu) }
        imageArray.addAll(allImages)

        // === Botón Tienda ===
        val btnTienda = findViewById<android.widget.Button>(R.id.btnTienda)
        btnTienda.setOnClickListener {
            pausarTemporizador()
            val intent = Intent(this, TiendaActivity::class.java)
            intent.putExtra("PUNTAJE_ACTUAL", score)
            startActivityForResult(intent, REQUEST_CODE_TIENDA)
        }

        // === Botón Cerrar Sesión (con pausa/reanudación) ===
        val btnCerrarSesion = findViewById<android.widget.Button>(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        hideImages()
        iniciarTemporizador(tiempoRestanteMs)
    }

    //==================== Temporizador & animación ====================

    private fun iniciarTemporizador(duracionMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duracionMs, 100) { // Cambiado a 100ms para mostrar décimas
            override fun onTick(millisUntilFinished: Long) {
                tiempoRestanteMs = millisUntilFinished
                val minutos = (millisUntilFinished / 60000).toInt()
                val segundos = ((millisUntilFinished % 60000) / 1000).toInt()
                val decimas = ((millisUntilFinished % 1000) / 100).toInt()
                timeText.text = String.format("Tiempo: %02d:%02d.%d", minutos, segundos, decimas)
            }

            override fun onFinish() {
                tiempoRestanteMs = 0
                timeText.text = "Tiempo: 00:00.0"
                handler.removeCallbacks(runnable)
                imageArray.forEach { it.visibility = View.INVISIBLE }

                // Verificar si ganó (30 o más puntos)
                if (score >= 30) {
                    // ¡VICTORIA!
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("¡GANASTE! 🎉")
                        .setMessage("¡Felicidades! Obtuviste $score puntos.\n\n¿Qué deseas hacer?")
                        .setPositiveButton("Reiniciar") { _, _ ->
                            val i = intent
                            finish()
                            startActivity(i)
                        }
                        .setNegativeButton("Salir") { _, _ ->
                            Toast.makeText(this@MainActivity, "¡Bien jugado! 🏆", Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    // Juego terminado (no alcanzó 30 puntos)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Juego terminado")
                        .setMessage("Obtuviste $score puntos.\nNecesitas 30 puntos para ganar.\n\n¿Reiniciar el juego?")
                        .setPositiveButton("Sí") { _, _ ->
                            val i = intent
                            finish()
                            startActivity(i)
                        }
                        .setNegativeButton("No") { _, _ ->
                            Toast.makeText(
                                this@MainActivity,
                                "Juego Terminado =/",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }
    private fun cargarPokemonsComprados() {
        pokemonsDisponibles.clear()

        if (jugadorId > 0) {
            val idsComprados = databaseHelper.obtenerPokemonsComprados(jugadorId)
            val todosLosPokemons = databaseHelper.obtenerTodosLosPokemons()

            for (pokemon in todosLosPokemons) {
                if (idsComprados.contains(pokemon.id)) {
                    val resourceId = resources.getIdentifier(
                        pokemon.imagenNombre,
                        "drawable",
                        packageName
                    )
                    if (resourceId != 0) {
                        pokemonsDisponibles.add(resourceId)
                    }
                }
            }
        }

        // Si no tiene pokémon comprados, usar Pikachu por defecto
        if (pokemonsDisponibles.isEmpty()) {
            pokemonsDisponibles.add(R.drawable.pikachu)
        }
    }
    private fun pausarTemporizador() {
        countDownTimer?.cancel()
        handler.removeCallbacks(runnable)
    }

    private fun reanudarTemporizador() {
        hideImages() // vuelve a mostrar/ocultar aleatoriamente
        if (tiempoRestanteMs > 0) {
            iniciarTemporizador(tiempoRestanteMs)
        } else {
            iniciarTemporizador(76_000)
        }
    }

    private fun hideImages() {
        runnable = object : Runnable {
            override fun run() {
                imageArray.forEach { it.visibility = View.INVISIBLE }
                val randomIndex = Random().nextInt(imageArray.size)

                // Determinar si aparece Meowth (10%) o un pokémon comprado (90%)
                val esMeowth = Random().nextInt(100) < 10

                if (esMeowth) {
                    // Mostrar Meowth
                    imageArray[randomIndex].setImageResource(imagenMeowth)
                    imageArray[randomIndex].tag = "meowth" // Marcar como enemigo
                } else {
                    // Mostrar pokémon aleatorio de los comprados
                    val pokemonAleatorio = pokemonsDisponibles[Random().nextInt(pokemonsDisponibles.size)]
                    imageArray[randomIndex].setImageResource(pokemonAleatorio)
                    imageArray[randomIndex].tag = "pokemon" // Marcar como amigo
                }

                imageArray[randomIndex].visibility = View.VISIBLE
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    fun increaseScore(view: View) {
        val imageView = view as ImageView
        val tag = imageView.tag

        if (tag == "meowth") {
            // Es Meowth (enemigo) - restar 2 puntos
            score -= 2
            if (score < 0) {
                score = 0 // No permitir puntaje negativo
            }
            Toast.makeText(this, "¡Meowth! -2 puntos 😾", Toast.LENGTH_SHORT).show()
        } else {
            // Es un pokémon amigo - sumar 1 punto
            score += 1
        }

        scoreText.text = "Puntaje: $score"
    }
    //==================== Cerrar sesión (tu bloque integrado) ====================

    private fun mostrarDialogoCerrarSesion() {
        // Pausar el juego
        pausarTemporizador()

        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?\n\nTu progreso se guardará automáticamente.")
            .setPositiveButton("Sí, Salir") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                // Reanudar el juego si cancela
                reanudarTemporizador()
            }
            .setOnCancelListener {
                // Reanudar el juego si cierra el diálogo
                reanudarTemporizador()
            }
            .show()
    }

    private fun cerrarSesion() {
        // Guardar puntaje final (si hay sesión válida)
        if (jugadorId > 0) {
            databaseHelper.actualizarPuntajeJugador(jugadorId, score)
        }

        // Limpiar sesión de SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Detener timer y animaciones
        countDownTimer?.cancel()
        handler.removeCallbacks(runnable)

        // Mostrar mensaje
        Toast.makeText(this, "Sesión cerrada. ¡Hasta pronto! 👋", Toast.LENGTH_SHORT).show()

        // Ir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    //==================== Resultados de la Tienda ====================

    @Deprecated("startActivityForResult está deprecado, se mantiene por compatibilidad")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_TIENDA && resultCode == RESULT_OK) {
            data?.let {
                score = it.getIntExtra("PUNTAJE_ACTUALIZADO", score)
                scoreText.text = "Puntaje: $score"
            }

            // Recargar pokémon comprados (por si compró uno nuevo)
            cargarPokemonsComprados()

            // Reanudamos el juego al volver de la tienda
            reanudarTemporizador()
        }
    }

}
