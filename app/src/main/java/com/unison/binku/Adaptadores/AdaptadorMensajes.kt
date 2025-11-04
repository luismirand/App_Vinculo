package com.unison.binku.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Constantes
import com.unison.binku.Models.ModeloMensaje
import com.unison.binku.R
import java.text.SimpleDateFormat
import java.util.*

class AdaptadorMensajes(
    private val context: Context,
    private var listaMensajes: ArrayList<ModeloMensaje>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Constantes para los tipos de vista
    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    // ViewHolder para mensajes ENVIADOS
    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMensaje: TextView = itemView.findViewById(R.id.tvMensajeTexto)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    // ViewHolder para mensajes RECIBIDOS
    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMensaje: TextView = itemView.findViewById(R.id.tvMensajeTexto)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    /**
     * Esta es la función clave. Decide qué layout usar.
     */
    override fun getItemViewType(position: Int): Int {
        val mensaje = listaMensajes[position]
        return if (mensaje.emisorId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun getItemCount(): Int = listaMensajes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = listaMensajes[position]

        // Formatear la hora (ej: "11:32 PM")
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(mensaje.timestamp))

        if (holder.itemViewType == VIEW_TYPE_SENT) {
            // Es un ViewHolder de ENVIADO
            val sentHolder = holder as SentViewHolder
            sentHolder.tvMensaje.text = mensaje.texto
            sentHolder.tvTimestamp.text = timeStr
        } else {
            // Es un ViewHolder de RECIBIDO
            val receivedHolder = holder as ReceivedViewHolder
            receivedHolder.tvMensaje.text = mensaje.texto
            receivedHolder.tvTimestamp.text = timeStr
        }
    }

    fun addMensaje(mensaje: ModeloMensaje) {
        listaMensajes.add(mensaje)
        notifyItemInserted(listaMensajes.size - 1)
    }
}