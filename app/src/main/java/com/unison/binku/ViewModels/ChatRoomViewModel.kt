package com.unison.binku.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloConversacion
import com.unison.binku.Models.ModeloMensaje
import com.unison.binku.Models.ModeloUsuario

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

    // El ID del chat actual. Se busca o se crea.
    private var currentChatId: String? = null
    private var mensajesListener: ChildEventListener? = null

    private val _mensajes = MutableLiveData<ModeloMensaje>()
    val mensajes: LiveData<ModeloMensaje> get() = _mensajes // Se emite UN mensaje a la vez

    private val _nombreOtroUsuario = MutableLiveData<String>()
    val nombreOtroUsuario: LiveData<String> get() = _nombreOtroUsuario

    init {
        // 1. Cargar la info de ambos usuarios
        cargarDatosUsuarios()
        // 2. Encontrar o crear el chat
        encontrarOCrearChat()
    }

    private fun cargarDatosUsuarios() {
        usuariosRef.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(ModeloUsuario::class.java)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        usuariosRef.child(otroUsuarioId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                otroUser = snapshot.getValue(ModeloUsuario::class.java)
                _nombreOtroUsuario.value = otroUser?.nombres ?: "Usuario"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun encontrarOCrearChat() {
        // 1. Buscar en mi lista de chats si ya tengo uno con este usuario
        chatListRef.child(currentUserId).child(otroUsuarioId).child("chatId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // 2. Si existe, lo usamos
                        currentChatId = snapshot.getValue(String::class.java)
                        Log.d("ChatRoomVM", "Chat encontrado: $currentChatId")
                        escucharMensajes()
                    } else {
                        // 3. Si no existe, creamos uno nuevo
                        currentChatId = mensajesRef.push().key // Genera un ID único
                        Log.d("ChatRoomVM", "Chat no existe. Creando uno nuevo: $currentChatId")
                        // No es necesario escribir nada aquí, se escribirá al enviar el primer mensaje
                        escucharMensajes()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun escucharMensajes() {
        if (currentChatId == null) return

        mensajesListener = mensajesRef.child(currentChatId!!)
            .orderByChild("timestamp")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    // Se dispara por cada mensaje existente Y por cada mensaje nuevo
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
                // (Omitir onChildChanged, onChildRemoved, etc. por simplicidad)
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun enviarMensaje(texto: String) {
        if (currentChatId == null || currentUser == null || otroUser == null) {
            Log.w("ChatRoomVM", "No se puede enviar mensaje, datos no cargados")
            return
        }

        val timestamp = System.currentTimeMillis()
        val mensajeId = mensajesRef.child(currentChatId!!).push().key ?: return

        val mensaje = ModeloMensaje(
            mensajeId = mensajeId,
            emisorId = currentUserId,
            texto = texto,
            imagenUrl = "",
            timestamp = timestamp
        )

        // 1. Guardar el mensaje
        mensajesRef.child(currentChatId!!).child(mensajeId).setValue(mensaje)

        // 2. Actualizar la lista de chats para AMBOS usuarios
        actualizarChatList(currentUserId, otroUser!!, mensaje)
        actualizarChatList(otroUsuarioId, currentUser!!, mensaje)
    }

    private fun actualizarChatList(paraUsuario: String, conUsuario: ModeloUsuario, ultimoMensaje: ModeloMensaje) {
        val ref = chatListRef.child(paraUsuario).child(conUsuario.uid)

        val conversacion = ModeloConversacion(
            chatId = currentChatId!!,
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
        // Detener el listener de mensajes
        mensajesListener?.let {
            currentChatId?.let { cid ->
                mensajesRef.child(cid).removeEventListener(it)
            }
        }
    }
}

// Factory para poder pasar el "otroUsuarioId" al ViewModel
class ChatRoomVMFactory(private val otroUsuarioId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(otroUsuarioId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}