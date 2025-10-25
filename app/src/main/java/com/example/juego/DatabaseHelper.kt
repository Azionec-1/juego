package com.example.juego

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "PokemonClicker.db"
        private const val DATABASE_VERSION = 3   // ⬅️ Subida para forzar migración si había una BD vieja

        // Tabla de Pokémon
        private const val TABLE_POKEMON = "pokemons"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_PRECIO = "precio"
        private const val COLUMN_IMAGEN = "imagen_nombre"

        // Tabla de Compras
        private const val TABLE_COMPRADOS = "pokemons_comprados"
        private const val COLUMN_COMPRA_ID = "compra_id"
        private const val COLUMN_POKEMON_ID = "pokemon_id"
        private const val COLUMN_FECHA_COMPRA = "fecha_compra"

        // Tabla de Jugadores
        private const val TABLE_JUGADORES = "jugadores"
        private const val COLUMN_JUGADOR_ID = "jugador_id"
        private const val COLUMN_NOMBRE_USUARIO = "nombre_usuario"
        private const val COLUMN_CORREO = "correo"
        private const val COLUMN_CLAVE = "clave"
        private const val COLUMN_PUNTAJE = "puntaje"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Habilita claves foráneas en SQLite Android
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        if (db == null) return
        ensureSchema(db)
        insertarPokemonsIniciales(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Autorreparación por si existía una BD previa sin tablas
        ensureSchema(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) return
        // Estrategia simple para desarrollo: drop + recreate
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPRADOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POKEMON")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_JUGADORES")
        onCreate(db)
    }

    /** Crea el esquema si falta (idempotente) */
    private fun ensureSchema(db: SQLiteDatabase) {
        val createJugadoresTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_JUGADORES (
                $COLUMN_JUGADOR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE_USUARIO TEXT NOT NULL UNIQUE,
                $COLUMN_CORREO TEXT NOT NULL UNIQUE,
                $COLUMN_CLAVE TEXT NOT NULL,
                $COLUMN_PUNTAJE INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createPokemonTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_POKEMON (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_NOMBRE TEXT NOT NULL,
                $COLUMN_PRECIO INTEGER NOT NULL,
                $COLUMN_IMAGEN TEXT NOT NULL
            )
        """.trimIndent()

        val createComprasTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_COMPRADOS (
                $COLUMN_COMPRA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_JUGADOR_ID INTEGER NOT NULL,
                $COLUMN_POKEMON_ID INTEGER NOT NULL,
                $COLUMN_FECHA_COMPRA TEXT NOT NULL,
                FOREIGN KEY($COLUMN_JUGADOR_ID) REFERENCES $TABLE_JUGADORES($COLUMN_JUGADOR_ID),
                FOREIGN KEY($COLUMN_POKEMON_ID) REFERENCES $TABLE_POKEMON($COLUMN_ID)
            )
        """.trimIndent()

        db.execSQL(createJugadoresTable)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_jugadores_usuario ON $TABLE_JUGADORES($COLUMN_NOMBRE_USUARIO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_jugadores_correo  ON $TABLE_JUGADORES($COLUMN_CORREO)")

        db.execSQL(createPokemonTable)
        db.execSQL(createComprasTable)
    }

    /** Inserta 9 pokémon iniciales de forma segura (no duplica si ya existen) */
    private fun insertarPokemonsIniciales(db: SQLiteDatabase) {
        val pokemons = listOf(
            Pokemon(1, "Bulbasaur", 5, "poke1"),
            Pokemon(2, "Charmander", 10, "poke2"),
            Pokemon(3, "Squirtle", 15, "poke3"),
            Pokemon(4, "Pikachu", 20, "poke4"),
            Pokemon(5, "Jigglypuff", 25, "poke5"),
            Pokemon(6, "Psyduck", 30, "poke6"),
            Pokemon(7, "Eevee", 40, "poke7"),
            Pokemon(8, "Snorlax", 50, "poke8"),
            Pokemon(9, "Charizard", 100, "poke9")
        )

        db.beginTransaction()
        try {
            for (pokemon in pokemons) {
                val values = ContentValues().apply {
                    put(COLUMN_ID, pokemon.id)
                    put(COLUMN_NOMBRE, pokemon.nombre)
                    put(COLUMN_PRECIO, pokemon.precio)
                    put(COLUMN_IMAGEN, pokemon.imagenNombre)
                }
                // INSERT OR IGNORE evita fallar si ya existe el ID
                db.insertWithOnConflict(
                    TABLE_POKEMON,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==================== QUERIES DE POKÉMON ====================

    fun obtenerTodosLosPokemons(): List<Pokemon> {
        val lista = mutableListOf<Pokemon>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_POKEMON,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_PRECIO ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val p = Pokemon(
                    id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_NOMBRE)),
                    precio = it.getInt(it.getColumnIndexOrThrow(COLUMN_PRECIO)),
                    imagenNombre = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGEN))
                )
                lista.add(p)
            }
        }
        return lista
    }

    // IDs de pokémon comprados por jugador
    fun obtenerPokemonsComprados(jugadorId: Int): List<Int> {
        val ids = mutableListOf<Int>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COMPRADOS,
            arrayOf(COLUMN_POKEMON_ID),
            "$COLUMN_JUGADOR_ID = ?",
            arrayOf(jugadorId.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                ids.add(it.getInt(it.getColumnIndexOrThrow(COLUMN_POKEMON_ID)))
            }
        }
        return ids
    }

    // Comprar un pokémon (registra jugador y pokémon)
    fun comprarPokemon(jugadorId: Int, pokemonId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_JUGADOR_ID, jugadorId)
            put(COLUMN_POKEMON_ID, pokemonId)
            put(COLUMN_FECHA_COMPRA, System.currentTimeMillis().toString())
        }
        val res = db.insert(TABLE_COMPRADOS, null, values)
        return res != -1L
    }

    // ¿Ya fue comprado este pokémon por este jugador?
    fun estaComprado(jugadorId: Int, pokemonId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COMPRADOS,
            arrayOf(COLUMN_POKEMON_ID),
            "$COLUMN_JUGADOR_ID = ? AND $COLUMN_POKEMON_ID = ?",
            arrayOf(jugadorId.toString(), pokemonId.toString()),
            null, null, null
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    // ==================== QUERIES DE JUGADORES ====================

    fun registrarJugador(nombreUsuario: String, correo: String, clave: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOMBRE_USUARIO, nombreUsuario)
            put(COLUMN_CORREO, correo)
            put(COLUMN_CLAVE, clave)
            put(COLUMN_PUNTAJE, 0)
        }
        return db.insert(TABLE_JUGADORES, null, values)
    }

    fun existeNombreUsuario(nombreUsuario: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JUGADORES,
            arrayOf(COLUMN_JUGADOR_ID),
            "$COLUMN_NOMBRE_USUARIO = ?",
            arrayOf(nombreUsuario),
            null, null, null
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    fun existeCorreo(correo: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JUGADORES,
            arrayOf(COLUMN_JUGADOR_ID),
            "$COLUMN_CORREO = ?",
            arrayOf(correo),
            null, null, null
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    // Login: devuelve jugador_id si coincide usuario/correo + clave
    fun login(usuarioOCorreo: String, clave: String): Int? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JUGADORES,
            arrayOf(COLUMN_JUGADOR_ID),
            "($COLUMN_NOMBRE_USUARIO = ? OR $COLUMN_CORREO = ?) AND $COLUMN_CLAVE = ?",
            arrayOf(usuarioOCorreo, usuarioOCorreo, clave),
            null, null, null
        )
        var id: Int? = null
        cursor.use {
            if (it.moveToFirst()) {
                id = it.getInt(it.getColumnIndexOrThrow(COLUMN_JUGADOR_ID))
            }
        }
        return id
    }

    fun obtenerPuntajeJugador(jugadorId: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JUGADORES,
            arrayOf(COLUMN_PUNTAJE),
            "$COLUMN_JUGADOR_ID = ?",
            arrayOf(jugadorId.toString()),
            null, null, null
        )
        var puntaje = 0
        cursor.use {
            if (it.moveToFirst()) {
                puntaje = it.getInt(it.getColumnIndexOrThrow(COLUMN_PUNTAJE))
            }
        }
        return puntaje
    }

    fun actualizarPuntajeJugador(jugadorId: Int, nuevoPuntaje: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put(COLUMN_PUNTAJE, nuevoPuntaje) }
        val filas = db.update(
            TABLE_JUGADORES,
            values,
            "$COLUMN_JUGADOR_ID = ?",
            arrayOf(jugadorId.toString())
        )
        return filas > 0
    }

    fun obtenerNombreUsuario(jugadorId: Int): String {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JUGADORES,
            arrayOf(COLUMN_NOMBRE_USUARIO),
            "$COLUMN_JUGADOR_ID = ?",
            arrayOf(jugadorId.toString()),
            null, null, null
        )
        var nombre = ""
        cursor.use {
            if (it.moveToFirst()) {
                nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_NOMBRE_USUARIO))
            }
        }
        return nombre
    }
}
