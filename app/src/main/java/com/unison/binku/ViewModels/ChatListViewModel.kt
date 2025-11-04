package com.unison.binku.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloConversacion

class ChatListViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val chatListRef = db.getReference("ChatList").child(currentUserId)

    private val _conversaciones = MutableLiveData<ArrayList<ModeloConversacion>>()
    val conversaciones: LiveData<ArrayList<ModeloConversacion>> get() = _conversaciones

    // --- >>> ¡AQUÍ ESTÁ LA CORRECCIÓN! <<< ---
    // Esta es la lógica correcta para leer la estructura ChatList/{myId}/{otherId}
    private val chatListListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) { // snapshot está en "ChatList/{myId}"
            val list = ArrayList<ModeloConversacion>()
            for (userSnapshot in snapshot.children) { // userSnapshot es "{otherId}"
                try {
                    // Obtenemos el objeto ModeloConversacion que está en "{otherId}"
                    val conversacion = userSnapshot.getValue(ModeloConversacion::class.java)
                    if (conversacion != null) {

                        // El `otroUsuarioId` es la clave del nodo, lo asignamos manualmente
                        // (Aunque ya está guardado adentro, esto es más seguro)
                        conversacion.otroUsuarioId = userSnapshot.key ?: ""

                        // (El `chatId` ya viene dentro del objeto `conversacion`)

                        list.add(conversacion)
                    }
                } catch (e: Exception) {
                    Log.e("ChatListVM", "Error al parsear conversación", e)
                }
            }
            // Ordenar por el timestamp más reciente primero
            list.sortByDescending { it.timestamp }
            _conversaciones.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("ChatListVM", "Error al cargar lista de chats", error.toException())
            _conversaciones.value = arrayListOf()
        }
    }
    // --- >>> FIN DE LA CORRECCIÓN <<< ---

    init {
        // La consulta aquí está bien
        chatListRef.addValueEventListener(chatListListener)
    }

    override fun onCleared() {
        super.onCleared()
        chatListRef.removeEventListener(chatListListener)
    }
}