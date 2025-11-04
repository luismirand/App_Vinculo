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

    // Referencia al nodo de la lista de chats del usuario actual
    private val chatListRef = db.getReference("ChatList").child(currentUserId)

    private val _conversaciones = MutableLiveData<ArrayList<ModeloConversacion>>()
    val conversaciones: LiveData<ArrayList<ModeloConversacion>> get() = _conversaciones

    private val chatListListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = ArrayList<ModeloConversacion>()
            for (chatSnapshot in snapshot.children) {
                try {
                    val conversacion = chatSnapshot.getValue(ModeloConversacion::class.java)
                    if (conversacion != null) {
                        conversacion.chatId = chatSnapshot.key ?: ""
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
            _conversaciones.value = arrayListOf() // Devolver lista vacía en caso de error
        }
    }

    init {
        // Empezar a escuchar cambios en la lista de chats
        chatListRef.addValueEventListener(chatListListener)
    }

    override fun onCleared() {
        super.onCleared()
        // Detener el listener cuando el ViewModel se destruya para evitar memory leaks
        chatListRef.removeEventListener(chatListListener)
    }
}