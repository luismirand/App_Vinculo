package com.unison.binku.Models

import com.google.firebase.database.Exclude

data class ModeloUsuario(
    @get:Exclude // Excluir de Firebase, ya que es la clave
    var uid: String = "",

    var nombres: String = "",
    var email: String = "", // Útil para buscar
    var urlImagenPerfil: String = ""
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "")
}