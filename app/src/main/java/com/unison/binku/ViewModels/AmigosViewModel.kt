package com.unison.binku.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloUsuario
import java.util.concurrent.atomic.AtomicInteger

class AmigosViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    // Referencias de la base de datos
    private val usuariosRef = db.getReference("Usuarios")
    private val amigosRef = db.getReference("Amigos")
    private val solicitudesRef = db.getReference("SolicitudesPendientes")

    // --- LiveData para la UI ---
    private val _amigos = MutableLiveData<List<ModeloUsuario>>()
    val amigos: LiveData<List<ModeloUsuario>> get() = _amigos

    private val _solicitudes = MutableLiveData<List<ModeloUsuario>>()
    val solicitudes: LiveData<List<ModeloUsuario>> get() = _solicitudes

    private val _busqueda = MutableLiveData<List<ModeloUsuario>>()
    val busqueda: LiveData<List<ModeloUsuario>> get() = _busqueda

    // --- Listeners de Firebase ---
    private var amigosListener: ValueEventListener? = null
    private var solicitudesListener: ValueEventListener? = null

    init {
        if (currentUserId.isNotEmpty()) {
            cargarAmigos()
            cargarSolicitudes()
        }
    }

    private fun cargarAmigos() {
        val ref = amigosRef.child(currentUserId)
        amigosListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendIds = snapshot.children.mapNotNull { it.key }
                fetchUserDetails(friendIds, _amigos)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AmigosViewModel", "Error cargando amigos: ${error.message}")
            }
        })
    }

    private fun cargarSolicitudes() {
        val ref = solicitudesRef.child(currentUserId)
        solicitudesListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senderIds = snapshot.children.mapNotNull { it.key }
                fetchUserDetails(senderIds, _solicitudes)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AmigosViewModel", "Error cargando solicitudes: ${error.message}")
            }
        })
    }

    /**
     * Helper para "unir" los IDs con los datos de perfil de usuario.
     */
    private fun fetchUserDetails(ids: List<String>, liveData: MutableLiveData<List<ModeloUsuario>>) {
        if (ids.isEmpty()) {
            liveData.postValue(emptyList())
            return
        }

        val userList = mutableListOf<ModeloUsuario>()
        val fetchCount = AtomicInteger(ids.size) // Contador para N consultas

        ids.forEach { userId ->
            usuariosRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(ModeloUsuario::class.java)?.let { user ->
                        user.uid = snapshot.key ?: ""
                        userList.add(user)
                    }
                    if (fetchCount.decrementAndGet() == 0) {
                        liveData.postValue(userList)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    if (fetchCount.decrementAndGet() == 0) {
                        liveData.postValue(userList)
                    }
                }
            })
        }
    }

    // --- Lógica de Acciones ---

    fun buscarUsuarios(query: String) {
        if (query.isBlank()) {
            _busqueda.postValue(emptyList())
            return
        }

        // Búsqueda en RTDB (busca por "nombres" que "empiezan con" la query)
        val dbQuery = usuariosRef.orderByChild("nombres")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limitToFirst(20)

        dbQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = snapshot.children.mapNotNull {
                    it.getValue(ModeloUsuario::class.java)?.apply {
                        uid = it.key ?: ""
                    }
                }.filter { it.uid != currentUserId } // No mostrarse a sí mismo
                _busqueda.postValue(results)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AmigosViewModel", "Error búsqueda: ${error.message}")
            }
        })
    }

    fun enviarSolicitud(recipientId: String) {
        if (recipientId == currentUserId) return
        // Escribe en: /SolicitudesPendientes/{recipientId}/{currentUserId} = true
        solicitudesRef.child(recipientId).child(currentUserId).setValue(true)
    }

    fun aceptarSolicitud(senderId: String) {
        // 1. Escribir en Amigos/{currentUserId}/{senderId} = true
        amigosRef.child(currentUserId).child(senderId).setValue(true)
        // 2. Escribir en Amigos/{senderId}/{currentUserId} = true (bidireccional)
        amigosRef.child(senderId).child(currentUserId).setValue(true)
        // 3. Borrar la solicitud pendiente
        rechazarSolicitud(senderId)
    }

    fun rechazarSolicitud(senderId: String) {
        // Borra de: /SolicitudesPendientes/{currentUserId}/{senderId}
        solicitudesRef.child(currentUserId).child(senderId).removeValue()
    }

    fun eliminarAmigo(friendId: String) {
        // Borrar de ambos lados
        amigosRef.child(currentUserId).child(friendId).removeValue()
        amigosRef.child(friendId).child(currentUserId).removeValue()
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar listeners para evitar memory leaks
        amigosListener?.let { amigosRef.child(currentUserId).removeEventListener(it) }
        solicitudesListener?.let { solicitudesRef.child(currentUserId).removeEventListener(it) }
    }
}