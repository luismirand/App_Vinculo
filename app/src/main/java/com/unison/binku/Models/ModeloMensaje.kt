package com.unison.binku.Models

import com.google.firebase.database.Exclude

data class ModeloMensaje(
    @get:Exclude
    var mensajeId: String = "",
    var emisorId: String = "",
    var texto: String = "",
    var imagenUrl: String = "", // (Por si quieres añadir imágenes después)
    var timestamp: Long = 0
) {
    // Constructor vacío para Firebase
    constructor() : this("", "", "", "", 0L)
}