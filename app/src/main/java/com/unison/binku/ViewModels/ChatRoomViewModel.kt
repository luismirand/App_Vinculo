package com.unison.binku.ViewModels

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloConversacion
import com.unison.binku.Models.ModeloMensaje
import com.unison.binku.Models.ModeloUsuario
import java.lang.IllegalArgumentException

class ChatRoomViewModel(private val otroUsuarioId: String) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private var currentUser: ModeloUsuario? = null
    private var otroUser: ModeloUsuario? = null

    // Referencias
    private val chatListRef = db.getReference("ChatList")
    private val mensajesRef = db.getReference("Mensajes")
    private val usuariosRef = db.getReference("Usuarios")

    // --- >>> ¡LA CORRECCIÓN CLAVE ESTÁ AQUÍ! <<< ---
    // El ID del chat ahora es predecible, no aleatorio.
    private val currentChatId: String
    // --- >>> FIN DE LA CORRECCIÓN <<< ---

    private var mensajesListener: ChildEventListener? = null

    private val _mensajes = MutableLiveData<ModeloMensaje>()
    val mensajes: LiveData<ModeloMensaje> get() = _mensajes

    private val _nombreOtroUsuario = MutableLiveData<String>()
    val nombreOtroUsuario: LiveData<String> get() = _nombreOtroUsuario

    init {
        // --- >>> CORRECCIÓN: Calcular el Chat ID Determinista <<< ---
        currentChatId = if (currentUserId < otroUsuarioId) {
            "${currentUserId}_${otroUsuarioId}"
        } else {
            "${otroUsuarioId}_${currentUserId}"
        }
        Log.d("ChatRoomVM", "Chat ID determinado: $currentChatId")
        // --- >>> FIN DE LA CORRECCIÓN <<< ---

        // 1. Cargar la info de ambos usuarios
        cargarDatosUsuarios()
        // 2. Escuchar mensajes (ya no necesitamos "encontrar o crear")
        escucharMensajes()
    }

    private fun cargarDatosUsuarios() {
        usuariosRef.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(ModeloUsuario::class.java)?.apply { uid = snapshot.key ?: "" }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        usuariosRef.child(otroUsuarioId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                otroUser = snapshot.getValue(ModeloUsuario::class.java)?.apply { uid = snapshot.key ?: "" }
                _nombreOtroUsuario.value = otroUser?.nombres ?: "Usuario"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- (Función 'encontrarOCrearChat' eliminada, ya no es necesaria) ---

    private fun escucharMensajes() {
        mensajesListener = mensajesRef.child(currentChatId) // Escuchar en el ID determinista
            .orderByChild("timestamp")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    try {
                        val mensaje = snapshot.getValue(ModeloMensaje::class.java)
                        if (mensaje != null) {
                            mensaje.mensajeId = snapshot.key ?: ""
                            _mensajes.value = mensaje // Emitir el mensaje
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRoomVM", "Error al parsear mensaje", e)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun enviarMensaje(texto: String) {
        if (currentUser == null || otroUser == null) {
            Log.w("ChatRoomVM", "No se puede enviar mensaje, datos de usuario no cargados")
            // Opcional: Re-intentar cargar usuarios si son nulos
            if(currentUser == null) cargarDatosUsuarios()
            Toast.makeText(null, "Error al enviar, reintentando...", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val mensajeId = mensajesRef.child(currentChatId).push().key ?: return

        val mensaje = ModeloMensaje(
            mensajeId = mensajeId,
            emisorId = currentUserId,
            texto = texto,
            imagenUrl = "",
            timestamp = timestamp
        )

        // 1. Guardar el mensaje en el ID determinista
        mensajesRef.child(currentChatId).child(mensajeId).setValue(mensaje)

        // 2. Actualizar la lista de chats para AMBOS usuarios
        // (Esta parte estaba correcta en tu código)
        actualizarChatList(currentUserId, otroUser!!, mensaje)
        actualizarChatList(otroUsuarioId, currentUser!!, mensaje)
    }

    // --- Esta función está CORREGIDA para que coincida con la estructura de ChatListViewModel ---
    private fun actualizarChatList(paraUsuario: String, conUsuario: ModeloUsuario, ultimoMensaje: ModeloMensaje) {
        val ref = chatListRef.child(paraUsuario).child(conUsuario.uid)

        val conversacion = ModeloConversacion(
            chatId = currentChatId, // El ID determinista
            otroUsuarioId = conUsuario.uid,
            nombreOtroUsuario = conUsuario.nombres,
            avatarOtroUsuario = conUsuario.urlImagenPerfil,
            ultimoMensaje = ultimoMensaje.texto,
            timestamp = ultimoMensaje.timestamp,
            noLeido = (paraUsuario != currentUserId) // Marcar como no leído solo para el receptor
        )

        ref.setValue(conversacion)
    }

    override fun onCleared() {
        super.onCleared()
        mensajesListener?.let {
            mensajesRef.child(currentChatId).removeEventListener(it)
        }
    }
}

// Factory (Sin cambios, pero la incluyo por si acaso)
class ChatRoomVMFactory(private val otroUsuarioId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(otroUsuarioId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}