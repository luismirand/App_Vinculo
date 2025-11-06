package com.unison.binku.ViewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloPost
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.storage.FirebaseStorage
import com.unison.binku.ImageCompressor

// --- MODIFICADO: ViewModel -> AndroidViewModel ---
class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val _posts = MutableLiveData<ArrayList<ModeloPost>>(arrayListOf())
    val posts: LiveData<ArrayList<ModeloPost>> get() = _posts

    // Firebase references
    private val postsRef = FirebaseDatabase.getInstance().getReference("Posts")
    private val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios")
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = firebaseAuth.currentUser?.uid

    // --- MODIFICADO: Añadido contexto ---
    private val context = application.applicationContext

    // Listener en tiempo real
    private val postListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val tempList = ArrayList<ModeloPost>()
            for (ds in snapshot.children) {
                try {
                    val post = ds.getValue(ModeloPost::class.java)
                    if (post != null) {
                        post.postId = ds.key ?: ""
                        post.contadorLikes = ds.child("contadorLikes").getValue(Int::class.java) ?: 0
                        post.urlAvatarAutor = ds.child("urlAvatarAutor").getValue(String::class.java) ?: ""
                        if (currentUserId != null) {
                            post.isLikedPorUsuarioActual = ds.child("Likes").hasChild(currentUserId)
                        } else {
                            post.isLikedPorUsuarioActual = false
                        }
                        tempList.add(post)
                    }
                } catch (e: Exception) {
                    Log.e("FeedViewModel", "Error converting post data", e)
                }
            }
            tempList.reverse()
            _posts.value = tempList
            Log.d("FeedViewModel", "Posts loaded/updated: ${tempList.size}")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FeedViewModel", "Firebase post read error: ${error.message}")
            _posts.value = arrayListOf()
        }
    }

    init {
        cargarPostsDesdeFirebase()
    }

    fun toggleLikePost(postId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val postRef = postsRef.child(postId)

        postRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val post = mutableData.getValue(ModeloPost::class.java)
                    ?: return Transaction.success(mutableData)

                val likesNode = mutableData.child("Likes")
                val isLiked = likesNode.hasChild(currentUserId)

                if (isLiked) {
                    post.contadorLikes = (post.contadorLikes - 1).coerceAtLeast(0)
                    likesNode.child(currentUserId).value = null
                } else {
                    post.contadorLikes = post.contadorLikes + 1
                    likesNode.child(currentUserId).value = true
                }

                mutableData.child("contadorLikes").value = post.contadorLikes
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("FeedViewModel", "Error en transacción de like: ${error.message}")
                }
            }
        })
    }

    /**
     * Agrega un post.
     */
    fun agregarPostFirebase(postText: String, imageUriString: String?, location: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val currentUserUid = currentUser.uid

        usuariosRef.child(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val avatarUrl = snapshot.child("urlImagenPerfil").getValue(String::class.java) ?: ""
                    val nombreUsuario = snapshot.child("nombres").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                        ?: (currentUser.displayName ?: "Usuario Binku")

                    val postId = postsRef.push().key
                    if (postId == null) {
                        Log.e("FeedViewModel", "No se pudo generar postId")
                        return
                    }

                    // 1) Sin imagen -> guardar directo
                    if (imageUriString.isNullOrBlank()) {
                        val post = ModeloPost(
                            uidAutor = currentUserUid,
                            nombreAutor = nombreUsuario,
                            textoPost = postText,
                            imagenUrlPost = "",
                            timestamp = System.currentTimeMillis(),
                            ubicacion = location,
                            contadorLikes = 0,
                            urlAvatarAutor = avatarUrl
                        )
                        guardarPost(postId, post)
                        return
                    }

                    // 2) Con imagen -> intentar subir a Storage
                    val localUri = Uri.parse(imageUriString)

                    // --- >>> ¡CÓDIGO MODIFICADO! <<< ---
                    // Comprimir la imagen ANTES de subirla
                    val imagenComprimida = ImageCompressor.compressImage(context, localUri, quality = 80, maxSizeKb = 500)

                    if (imagenComprimida == null) {
                        Log.w("FeedViewModel", "Error al comprimir imagen, guardo URI local")
                        guardarPostConLocalUri(postId, currentUserUid, nombreUsuario, postText, imageUriString, location, avatarUrl)
                        return
                    }
                    // --- >>> FIN DE MODIFICACIÓN <<< ---


                    try {
                        val storageRef = FirebaseStorage.getInstance()
                            .getReference("PostImages/$currentUserUid/$postId.jpg") // Ruta de las reglas

                        // --- >>> ¡CÓDIGO MODIFICADO! <<< ---
                        // En lugar de .putFile(localUri), usamos .putBytes(imagenComprimida)
                        storageRef.putBytes(imagenComprimida)
                            .addOnSuccessListener {
                                storageRef.downloadUrl
                                    .addOnSuccessListener { downloadUri ->
                                        val post = ModeloPost(
                                            uidAutor = currentUserUid,
                                            nombreAutor = nombreUsuario,
                                            textoPost = postText,
                                            imagenUrlPost = downloadUri.toString(), // URL pública
                                            timestamp = System.currentTimeMillis(),
                                            ubicacion = location,
                                            contadorLikes = 0,
                                            urlAvatarAutor = avatarUrl
                                        )
                                        guardarPost(postId, post)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("FeedViewModel", "No se pudo obtener downloadUrl, guardo URI local: ${e.message}")
                                        guardarPostConLocalUri(postId, currentUserUid, nombreUsuario, postText, imageUriString, location, avatarUrl)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.w("FeedViewModel", "Fallo putBytes en Storage, guardo URI local: ${e.message}")
                                guardarPostConLocalUri(postId, currentUserUid, nombreUsuario, postText, imageUriString, location, avatarUrl)
                            }
                    } catch (e: Exception) {
                        // Por ejemplo: Storage no configurado en el proyecto
                        Log.w("FeedViewModel", "Storage no disponible, guardo URI local: ${e.message}")
                        guardarPostConLocalUri(postId, currentUserUid, nombreUsuario, postText, imageUriString, location, avatarUrl)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedViewModel", "Error leyendo datos de usuario: ${error.message}")
                }
            })
    }

    private fun guardarPostConLocalUri(
        postId: String,
        uid: String,
        nombre: String,
        texto: String,
        localUriString: String,
        location: String,
        avatarUrl: String
    ) {
        val post = ModeloPost(
            uidAutor = uid,
            nombreAutor = nombre,
            textoPost = texto,
            imagenUrlPost = localUriString, // content:// o file:// (solo visible localmente)
            timestamp = System.currentTimeMillis(),
            ubicacion = location,
            contadorLikes = 0,
            urlAvatarAutor = avatarUrl
        )
        guardarPost(postId, post)
    }

    /** <-- MÉTODO PÚBLICO USADO EN EL FRAGMENT */
    fun eliminarPostFirebase(postId: String) {
        if (postId.isEmpty()) {
            Log.w("FeedViewModel", "Attempted to delete post with empty ID")
            return
        }
        postsRef.child(postId).removeValue()
            .addOnSuccessListener {
                Log.d("FeedViewModel", "Post deleted successfully: $postId")
            }
            .addOnFailureListener { e ->
                Log.e("FeedViewModel", "Error deleting post: ${e.message}")
            }
    }

    private fun guardarPost(postId: String, post: ModeloPost) {
        postsRef.child(postId).setValue(post)
            .addOnSuccessListener {
                Log.d("FeedViewModel", "Post guardado OK (id=$postId)")
            }
            .addOnFailureListener { e ->
                Log.e("FeedViewModel", "Error guardando post: ${e.message}")
            }
    }

    private fun cargarPostsDesdeFirebase() {
        Log.d("FeedViewModel", "Starting Firebase post listener (orderByChild timestamp)")
        postsRef.orderByChild("timestamp").addValueEventListener(postListener)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("FeedViewModel", "Stopping Firebase post listener")
        postsRef.removeEventListener(postListener)
    }
}