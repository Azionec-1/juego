package com.example.juego

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar vistas
        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)

        // Inicializar base de datos
        databaseHelper = DatabaseHelper(this)

        // Verificar si ya hay sesión iniciada
        verificarSesion()

        // Botón Login
        btnLogin.setOnClickListener {
            iniciarSesion()
        }

        // Botón Registro
        btnRegistro.setOnClickListener {
            mostrarDialogoRegistro()
        }
    }

    private fun verificarSesion() {
        val prefs = getSharedPreferences("PokemonPrefs", Context.MODE_PRIVATE)
        val jugadorId = prefs.getInt("jugador_id", -1)

        if (jugadorId != -1) {
            // Ya hay sesión iniciada, ir directo al juego
            irAlJuego()
        }
    }

    private fun iniciarSesion() {
        val usuario = etUsuario.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones
        if (usuario.isEmpty()) {
            Toast.makeText(this, "Ingresa tu usuario o correo", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Intentar login
        val jugadorId = databaseHelper.login(usuario, password)

        if (jugadorId != null) {
            // Login exitoso
            guardarSesion(jugadorId)
            Toast.makeText(this, "¡Bienvenido! 🎮", Toast.LENGTH_SHORT).show()
            irAlJuego()
        } else {
            // Login fallido
            Toast.makeText(this, "Usuario o contraseña incorrectos ❌", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoRegistro() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_registro, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreRegistro)
        val etCorreo = dialogView.findViewById<EditText>(R.id.etCorreoRegistro)
        val etPasswordRegistro = dialogView.findViewById<EditText>(R.id.etPasswordRegistro)
        val etPasswordConfirm = dialogView.findViewById<EditText>(R.id.etPasswordConfirm)

        AlertDialog.Builder(this)
            .setTitle("Crear Cuenta Nueva")
            .setView(dialogView)
            .setPositiveButton("Registrar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val correo = etCorreo.text.toString().trim()
                val password = etPasswordRegistro.text.toString().trim()
                val passwordConfirm = etPasswordConfirm.text.toString().trim()

                registrarUsuario(nombre, correo, password, passwordConfirm)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarUsuario(nombre: String, correo: String, password: String, passwordConfirm: String) {
        // Validaciones
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (nombre.length < 3) {
            Toast.makeText(this, "El nombre debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 4) {
            Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el usuario ya existe
        if (databaseHelper.existeNombreUsuario(nombre)) {
            Toast.makeText(this, "El nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
            return
        }

        if (databaseHelper.existeCorreo(correo)) {
            Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Registrar usuario
        val resultado = databaseHelper.registrarJugador(nombre, correo, password)

        if (resultado != -1L) {
            Toast.makeText(this, "¡Cuenta creada exitosamente! ✅", Toast.LENGTH_SHORT).show()
            etUsuario.setText(nombre)
            etPassword.setText(password)
        } else {
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarSesion(jugadorId: Int) {
        val prefs = getSharedPreferences("PokemonPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("jugador_id", jugadorId)
            apply()
        }
    }

    private fun irAlJuego() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}