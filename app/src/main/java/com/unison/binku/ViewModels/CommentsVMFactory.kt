package com.unison.binku.ViewModels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// --- MODIFICADO: Añadido "application" al constructor ---
class CommentsVMFactory(
    private val application: Application,
    private val postId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // --- MODIFICADO: Pasa "application" al ViewModel ---
        return CommentsViewModel(application, postId) as T
    }
}