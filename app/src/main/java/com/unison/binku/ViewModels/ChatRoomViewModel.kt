package com.unison.binku.ViewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.unison.binku.ImageCompressor
import com.unison.binku.Models.ModeloConversacion
import com.unison.binku.Models.ModeloMensaje
import com.unison.binku.Models.ModeloUsuario

// --- MODIFICADO: ViewModel -> AndroidViewModel ---
class ChatRoomViewModel(application: Application, private val otroUsuarioId: String) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance() // <-- NUEVO
    private val context = application.applicationContext // <-- NUEVO

    private val currentUserId = auth.currentUser?.uid ?: ""
    private var currentUser: ModeloUsuario? = null
    private var otroUser: ModeloUsuario? = null

    // Referencias
    private val chatListRef = db.getReference("ChatList")
    private val mensajesRef = db.getReference("Mensajes")
    private val usuariosRef = db.getReference("Usuarios")

    private val currentChatId: String
    private var mensajesListener: ChildEventListener? = null

    private val _mensajes = MutableLiveData<ModeloMensaje>()
    val mensajes: LiveData<ModeloMensaje> get() = _mensajes

    private val _nombreOtroUsuario = MutableLiveData<String>()
    val nombreOtroUsuario: LiveData<String> get() = _nombreOtroUsuario

    // --- NUEVO ---
    private val _avatarOtroUsuario = MutableLiveData<String>()
    val avatarOtroUsuario: LiveData<String> get() = _avatarOtroUsuario
    // --- FIN NUEVO ---


    init {
        currentChatId = if (currentUserId < otroUsuarioId) {
            "${currentUserId}_${otroUsuarioId}"
        } else {
            "${otroUsuarioId}_${currentUserId}"
        }
        Log.d("ChatRoomVM", "Chat ID determinado: $currentChatId")

        cargarDatosUsuarios()
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
                _avatarOtroUsuario.value = otroUser?.urlImagenPerfil ?: "" // <-- NUEVO
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

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

    // --- MODIFICADO: Acepta imageUriString ---
    fun enviarMensaje(texto: String, imageUriString: String?) {
        if (currentUser == null || otroUser == null) {
            Log.w("ChatRoomVM", "No se puede enviar mensaje, datos de usuario no cargados")
            if(currentUser == null) cargarDatosUsuarios()
            Toast.makeText(context, "Error al enviar, reintentando...", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val mensajeId = mensajesRef.child(currentChatId).push().key ?: return

        // Función interna para guardar el mensaje final
        fun guardarMensajeEnDB(textoFinal: String, imagenUrlFinal: String) {
            val mensaje = ModeloMensaje(
                mensajeId = mensajeId,
                emisorId = currentUserId,
                texto = textoFinal,
                imagenUrl = imagenUrlFinal,
                timestamp = timestamp
            )

            // 1. Guardar el mensaje en el ID determinista
            mensajesRef.child(currentChatId).child(mensajeId).setValue(mensaje)

            // 2. Actualizar la lista de chats para AMBOS usuarios
            actualizarChatList(currentUserId, otroUser!!, mensaje)
            actualizarChatList(otroUsuarioId, currentUser!!, mensaje)
        }

        // Si NO hay imagen, guardar solo texto
        if (imageUriString.isNullOrBlank()) {
            guardarMensajeEnDB(texto, "")
            return
        }

        // Si SÍ hay imagen, comprimir y subir
        val localUri = Uri.parse(imageUriString)
        val imagenComprimida = ImageCompressor.compressImage(context, localUri, quality = 80, maxSizeKb = 500)

        if (imagenComprimida == null) {
            Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.getReference("ChatImages/$currentChatId/$mensajeId.jpg")

        storageRef.putBytes(imagenComprimida)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        // Éxito: guardar texto Y la URL de la imagen
                        guardarMensajeEnDB(texto, downloadUri.toString())
                    }
                    .addOnFailureListener {
                        // Fallo al obtener URL: guardar solo texto
                        Toast.makeText(context, "Error al obtener URL de imagen", Toast.LENGTH_SHORT).show()
                        guardarMensajeEnDB(texto, "")
                    }
            }
            .addOnFailureListener {
                // Fallo al subir: guardar solo texto
                Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                guardarMensajeEnDB(texto, "")
            }
    }


    private fun actualizarChatList(paraUsuario: String, conUsuario: ModeloUsuario, ultimoMensaje: ModeloMensaje) {
        val ref = chatListRef.child(paraUsuario).child(conUsuario.uid)

        // --- MODIFICADO: Muestra "📷 Imagen" si el mensaje es una foto ---
        val textoUltimoMensaje = if (ultimoMensaje.imagenUrl.isNotBlank()) {
            "📷 Imagen" + if (ultimoMensaje.texto.isNotBlank()) ": ${ultimoMensaje.texto}" else ""
        } else {
            ultimoMensaje.texto
        }

        val conversacion = ModeloConversacion(
            chatId = currentChatId, // El ID determinista
            otroUsuarioId = conUsuario.uid,
            nombreOtroUsuario = conUsuario.nombres,
            avatarOtroUsuario = conUsuario.urlImagenPerfil,
            ultimoMensaje = textoUltimoMensaje, // <-- MODIFICADO
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