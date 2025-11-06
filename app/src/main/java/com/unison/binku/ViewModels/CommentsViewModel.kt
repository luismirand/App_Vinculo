package com.unison.binku.ViewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.unison.binku.ImageCompressor
import com.unison.binku.Models.ModeloComentario

// --- MODIFICADO: ViewModel -> AndroidViewModel y constructor ---
class CommentsViewModel(application: Application, private val postId: String) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val currentUid = auth.currentUser?.uid

    // --- MODIFICADO: Añadido contexto ---
    private val context = application.applicationContext

    private val usuariosRef = db.getReference("Usuarios")
    private val commentsRef = db.getReference("Posts").child(postId).child("Comments")

    private val _comments = MutableLiveData<ArrayList<ModeloComentario>>(arrayListOf())
    val comments: LiveData<ArrayList<ModeloComentario>> get() = _comments

    private val commentsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = ArrayList<ModeloComentario>()
            for (cs in snapshot.children) {
                try {
                    val model = cs.getValue(ModeloComentario::class.java)
                    if (model != null) {
                        model.commentId = cs.key ?: ""
                        model.contadorLikes = cs.child("contadorLikes").getValue(Int::class.java) ?: 0
                        model.urlAvatarAutor = cs.child("urlAvatarAutor").getValue(String::class.java)
                            ?.takeIf { it.isNotBlank() && it != "null" } ?: "" // Si es null o vacío, usar ""
                        model.isLikedPorUsuarioActual = currentUid?.let { cs.child("Likes").hasChild(it) } ?: false
                        list.add(model)
                    }
                } catch (e: Exception) {
                    Log.e("CommentsVM", "Error parsing comment", e)
                }
            }
            _comments.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("CommentsVM", "comments listener cancelled: ${error.message}")
            _comments.value = arrayListOf()
        }
    }

    init {
        commentsRef.orderByChild("timestamp").addValueEventListener(commentsListener)
    }

    fun toggleLike(commentId: String) {
        val uid = currentUid ?: return
        val ref = commentsRef.child(commentId)

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val c = mutableData.getValue(ModeloComentario::class.java)
                    ?: return Transaction.success(mutableData)

                val likesNode = mutableData.child("Likes")
                val liked = likesNode.hasChild(uid)

                if (liked) {
                    c.contadorLikes = (c.contadorLikes - 1).coerceAtLeast(0)
                    likesNode.child(uid).value = null
                } else {
                    c.contadorLikes = c.contadorLikes + 1
                    likesNode.child(uid).value = true
                }

                mutableData.child("contadorLikes").value = c.contadorLikes
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) Log.e("CommentsVM", "Error like comment: ${error.message}")
            }
        })
    }

    fun agregarComentario(texto: String, imageUriStr: String?) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        usuariosRef.child(uid).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombres").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() } ?: (user.displayName ?: "Usuario Binku")

                val avatarUrl = snapshot.child("urlImagenPerfil").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() && it != "null" } ?: "" // Si es null o vacío, usar ""

                val commentId = commentsRef.push().key ?: return

                fun save(url: String) {
                    val c = ModeloComentario(
                        commentId = commentId,
                        uidAutor = uid,
                        nombreAutor = nombre,
                        texto = texto,
                        imagenUrl = url,
                        timestamp = System.currentTimeMillis(),
                        contadorLikes = 0,
                        isLikedPorUsuarioActual = false,
                        urlAvatarAutor = avatarUrl
                    )
                    commentsRef.child(commentId).setValue(c)
                }

                if (imageUriStr.isNullOrBlank()) {
                    save("")
                    return
                }

                val localUri = Uri.parse(imageUriStr)

                // --- >>> ¡CÓDIGO MODIFICADO! <<< ---
                // Comprimir la imagen ANTES de subirla
                val imagenComprimida = ImageCompressor.compressImage(context, localUri, quality = 80, maxSizeKb = 400) // 400KB para comentarios

                if (imagenComprimida == null) {
                    Log.w("CommentsVM", "No se pudo comprimir, guardo sin imagen")
                    save("")
                    return
                }
                // --- >>> FIN DE MODIFICACIÓN <<< ---


                val storageRef = FirebaseStorage.getInstance()
                    .getReference("CommentImages/$postId/$commentId.jpg") // Ruta de las reglas

                // --- >>> ¡CÓDIGO MODIFICADO! <<< ---
                // En lugar de .putFile(localUri), usamos .putBytes(imagenComprimida)
                storageRef.putBytes(imagenComprimida)
                    .addOnSuccessListener {
                        storageRef.downloadUrl
                            .addOnSuccessListener { downloadUri -> save(downloadUri.toString()) }
                            .addOnFailureListener { e ->
                                Log.w("CommentsVM", "No se pudo obtener downloadUrl: ${e.message}; guardo sin imagen")
                                save("")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.w("CommentsVM", "Fallo Storage: ${e.message}; guardo sin imagen")
                        save("")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CommentsVM", "error leyendo usuario: ${error.message}")
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        commentsRef.removeEventListener(commentsListener)
    }
}