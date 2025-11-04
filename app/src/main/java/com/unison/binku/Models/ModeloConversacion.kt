package com.unison.binku.Models

import com.google.firebase.database.Exclude

data class ModeloConversacion(
    @get:Exclude // El ID del chat es la clave del nodo
    var chatId: String = "",

    var otroUsuarioId: String = "",
    var nombreOtroUsuario: String = "",
    var avatarOtroUsuario: String = "",
    var ultimoMensaje: String = "",
    var timestamp: Long = 0,
    var noLeido: Boolean = false
) {
    // Constructor vacío para Firebase
    constructor() : this("", "", "", "", "", 0L, false)
}