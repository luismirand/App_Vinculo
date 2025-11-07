package com.unison.binku.ViewModels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class ChatRoomVMFactory(
    private val application: Application, // <-- NUEVO
    private val otroUsuarioId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasamos la aplicación y el ID al ViewModel
            return ChatRoomViewModel(application, otroUsuarioId) as T // <-- MODIFICADO
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}