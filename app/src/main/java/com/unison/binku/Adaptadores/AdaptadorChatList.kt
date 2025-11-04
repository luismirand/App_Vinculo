package com.unison.binku.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unison.binku.Constantes
import com.unison.binku.Models.ModeloConversacion
import com.unison.binku.R
import com.unison.binku.databinding.ItemChatConversacionBinding

class AdaptadorChatList(
    private val context: Context,
    private var listaConversaciones: ArrayList<ModeloConversacion>,
    private val onItemClick: (ModeloConversacion) -> Unit
) : RecyclerView.Adapter<AdaptadorChatList.ChatVH>() {

    inner class ChatVH(val binding: ItemChatConversacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
        val binding = ItemChatConversacionBinding.inflate(LayoutInflater.from(context), parent, false)
        return ChatVH(binding)
    }

    override fun getItemCount(): Int = listaConversaciones.size

    override fun onBindViewHolder(holder: ChatVH, position: Int) {
        val conversacion = listaConversaciones[position]
        val b = holder.binding

        b.tvNombreUsuario.text = conversacion.nombreOtroUsuario
        b.tvUltimoMensaje.text = conversacion.ultimoMensaje
        b.tvTimestamp.text = Constantes.obtenerFecha(conversacion.timestamp) // Reutilizamos tu función!

        // Mostrar/Ocultar el punto de "No Leído"
        b.viewNoLeido.visibility = if (conversacion.noLeido) View.VISIBLE else View.GONE

        // Cargar avatar
        if (conversacion.avatarOtroUsuario.isNotBlank()) {
            Glide.with(context)
                .load(conversacion.avatarOtroUsuario)
                .placeholder(R.drawable.ic_perfil_black)
                .error(R.drawable.ic_perfil_black)
                .circleCrop()
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.ic_perfil_black)
        }

        // Listener para abrir la sala de chat
        holder.itemView.setOnClickListener {
            onItemClick(conversacion)
        }
    }

    fun updateList(newList: ArrayList<ModeloConversacion>) {
        listaConversaciones = newList
        notifyDataSetChanged()
    }
}