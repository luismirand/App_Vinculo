package com.unison.binku.Models

import com.google.firebase.database.Exclude

data class ModeloComentario(
    @get:Exclude
    var commentId: String = "",

    var uidAutor: String = "",
    var nombreAutor: String = "",
    var texto: String = "",
    var imagenUrl: String = "",     // opcional
    var timestamp: Long = 0L,
    var contadorLikes: Int = 0,

    // para UI
    @get:Exclude
    var isLikedPorUsuarioActual: Boolean = false,

    // ayuda para avatar sin tener que re-consultar cada vez
    var urlAvatarAutor: String = ""
) {
    constructor(): this("", "", "", "", "", 0L, 0, false, "")
}
