package com.unison.binku.Models

data class ModeloPost(
    var postId: String = "",
    var uidAutor: String = "",
    var nombreAutor: String = "",
    var textoPost: String = "",
    var imagenUrlPost: String = "",
    var timestamp: Long = 0,
    var ubicacion: String = "",
    var contadorLikes: Int = 0,               // Para mostrar el número de likes
    var urlAvatarAutor: String = "",          // URL de la foto de perfil del autor
    @field:JvmField // Necesario para que Firebase pueda mapear 'isLiked'
    var isLikedPorUsuarioActual: Boolean = false, // Indica si el usuario actual le dio like (se calcula localmente)
    var contadorComentarios: Int = 0          // <-- NUEVO
) {
    // Constructor sin argumentos requerido por Firebase (actualizado)
    constructor() : this("", "", "", "", "", 0, "", 0, "", false, 0)
}