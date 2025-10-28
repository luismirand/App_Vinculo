package com.unison.binku.Models

// Data class para representar una publicación
data class ModeloPost(
    var postId: String = "",         // ID único del post
    var uidAutor: String = "",       // ID del usuario que lo creó
    var nombreAutor: String = "",    // Nombre del usuario que lo creó
    var textoPost: String = "",      // El contenido de texto del post
    var imagenUrlPost: String = "",  // URL de la imagen (si la hay)
    var timestamp: Long = 0          // Marca de tiempo de creación
)
