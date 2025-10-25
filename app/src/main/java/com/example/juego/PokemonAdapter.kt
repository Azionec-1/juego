package com.example.juego

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PokemonAdapter(
    private var pokemons: List<Pokemon>,
    private var pokemonsComprados: Set<Int>,
    private var puntajeActual: Int,
    private val onComprarClick: (Pokemon) -> Unit
) : RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder>() {

    class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardPokemon: CardView = itemView.findViewById(R.id.cardPokemon)
        val ivPokemon: ImageView = itemView.findViewById(R.id.ivPokemon)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePokemon)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioPokemon)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        val btnComprar: Button = itemView.findViewById(R.id.btnComprar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pokemon, parent, false)
        return PokemonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        val pokemon = pokemons[position]
        val yaComprado = pokemonsComprados.contains(pokemon.id)

        // Establecer datos
        holder.tvNombre.text = pokemon.nombre
        holder.tvPrecio.text = "Precio: ${pokemon.precio} puntos"

        // Cargar imagen
        val resourceId = holder.itemView.context.resources.getIdentifier(
            pokemon.imagenNombre,
            "drawable",
            holder.itemView.context.packageName
        )
        if (resourceId != 0) {
            holder.ivPokemon.setImageResource(resourceId)
        } else {
            holder.ivPokemon.setImageResource(R.drawable.pikachu)
        }

        // Estado del botón y card
        when {
            yaComprado -> {
                // Ya comprado
                holder.btnComprar.isEnabled = false
                holder.btnComprar.text = "Comprado"
                holder.btnComprar.setBackgroundColor(Color.parseColor("#CCCCCC"))
                holder.tvEstado.visibility = View.VISIBLE
                holder.tvEstado.text = "✓ COMPRADO"
                holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                holder.cardPokemon.alpha = 0.6f
            }
            puntajeActual >= pokemon.precio -> {
                // Puede comprar
                holder.btnComprar.isEnabled = true
                holder.btnComprar.text = "Comprar"
                holder.btnComprar.setBackgroundColor(Color.parseColor("#FF6B35"))
                holder.tvEstado.visibility = View.GONE
                holder.cardPokemon.alpha = 1f
            }
            else -> {
                // No tiene suficientes puntos
                holder.btnComprar.isEnabled = false
                holder.btnComprar.text = "Sin puntos"
                holder.btnComprar.setBackgroundColor(Color.parseColor("#CCCCCC"))
                holder.tvEstado.visibility = View.VISIBLE
                holder.tvEstado.text = "Faltan ${pokemon.precio - puntajeActual} pts"
                holder.tvEstado.setTextColor(Color.parseColor("#FF5722"))
                holder.cardPokemon.alpha = 0.8f
            }
        }

        holder.btnComprar.setOnClickListener {
            onComprarClick(pokemon)
        }
    }

    override fun getItemCount() = pokemons.size

    fun actualizarDatos(
        nuevosPokemons: List<Pokemon>,
        nuevosComprados: Set<Int>,
        nuevoPuntaje: Int
    ) {
        pokemons = nuevosPokemons
        pokemonsComprados = nuevosComprados
        puntajeActual = nuevoPuntaje
        notifyDataSetChanged()
    }
}