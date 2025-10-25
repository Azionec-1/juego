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
    private var tiempoRestanteMs: Long = 15_500 // valor inicial

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
        countDownTimer = object : CountDownTimer(duracionMs, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                tiempoRestanteMs = millisUntilFinished
                timeText.text = "Tiempo: ${millisUntilFinished / 1000} seg"
            }

            override fun onFinish() {
                tiempoRestanteMs = 0
                timeText.text = "Tiempo: 0 seg"
                handler.removeCallbacks(runnable)
                imageArray.forEach { it.visibility = View.INVISIBLE }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Juego terminado")
                    .setMessage("¿Reiniciar el juego?")
                    .setPositiveButton("Sí") { _, _ ->
                        val i = intent
                        finish()
                        startActivity(i)
                    }
                    .setNegativeButton("No") { _, _ ->
                        Toast.makeText(this@MainActivity, "Juego Terminado =/", Toast.LENGTH_LONG).show()
                    }
                    .show()
            }
        }.start()
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
            iniciarTemporizador(15_500)
        }
    }

    private fun hideImages() {
        runnable = object : Runnable {
            override fun run() {
                imageArray.forEach { it.visibility = View.INVISIBLE }
                val randomIndex = Random().nextInt(imageArray.size)
                imageArray[randomIndex].visibility = View.VISIBLE
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    fun increaseScore(view: View) {
        score += 1
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
            // Reanudamos el juego al volver de la tienda
            reanudarTemporizador()
        }
    }
}
