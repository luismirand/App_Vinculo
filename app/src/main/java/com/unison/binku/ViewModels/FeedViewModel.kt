package com.unison.binku.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloPost
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class FeedViewModel : ViewModel() {

    private val _posts = MutableLiveData<ArrayList<ModeloPost>>(arrayListOf())
    val posts: LiveData<ArrayList<ModeloPost>> get() = _posts

    // Firebase references
    private val postsRef = FirebaseDatabase.getInstance().getReference("Posts")
    private val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios") // Reference to Users node
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = firebaseAuth.currentUser?.uid

    // Real-time listener for changes in the "Posts" node
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

                        // --- Esta lógica es la clave ---
                        // Revisa si el ID del usuario actual está en la lista de "Likes"
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
        val currentUserId = firebaseAuth.currentUser?.uid ?: return // No hacer nada si no hay usuario
        val postRef = postsRef.child(postId)

        postRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                // 1. Obtener el estado actual del post
                val post = mutableData.getValue(ModeloPost::class.java)
                    ?: return Transaction.success(mutableData) // El post fue borrado, abortar.

                // 2. Revisar si el usuario ya dio like
                val likesNode = mutableData.child("Likes")
                val isLiked = likesNode.hasChild(currentUserId)

                // 3. Aplicar la lógica
                if (isLiked) {
                    // QUITAR LIKE
                    post.contadorLikes = (post.contadorLikes - 1).coerceAtLeast(0) // Restar 1, mínimo 0
                    likesNode.child(currentUserId).value = null // Borrar el ID del usuario de "Likes"
                } else {
                    // DAR LIKE
                    post.contadorLikes = post.contadorLikes + 1 // Sumar 1
                    likesNode.child(currentUserId).value = true // Añadir el ID del usuario a "Likes"
                }

                // 4. Actualizar el contador en la raíz del post
                mutableData.child("contadorLikes").value = post.contadorLikes

                // 5. Devolver los datos modificados para que se guarden
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("FeedViewModel", "Error en transacción de like: ${error.message}")
                } else {
                    Log.d("FeedViewModel", "Like/Unlike exitoso")
                }
            }
        })
    }


    fun agregarPostFirebase(postText: String, imageUriString: String?, location: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val currentUserUid = currentUser.uid

        // --- >>> OBTENER AVATAR URL ANTES DE GUARDAR <<< ---
        usuariosRef.child(currentUserUid).child("urlImagenPerfil")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val avatarUrl = snapshot.getValue(String::class.java) ?: "" // Get avatar URL or empty

                    // Ahora crear y guardar el post con la URL obtenida
                    val nuevoPost = ModeloPost(
                        uidAutor = currentUserUid,
                        nombreAutor = currentUser.displayName ?: "Usuario Binku",
                        textoPost = postText,
                        imagenUrlPost = imageUriString ?: "", // TODO: Replace with Storage URL
                        timestamp = System.currentTimeMillis(),
                        ubicacion = location,
                        contadorLikes = 0,
                        urlAvatarAutor = avatarUrl

                    )

                    // Generate unique ID using push()
                    val postId = postsRef.push().key
                    if (postId != null) {
                        // Save the complete post data
                        postsRef.child(postId).setValue(nuevoPost)
                            .addOnSuccessListener {
                                Log.d("FeedViewModel", "Post added successfully with Avatar URL, ID: $postId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FeedViewModel", "Error adding post: ${e.message}")
                            }
                    } else {
                        Log.e("FeedViewModel", "Could not generate unique post ID")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedViewModel", "Error fetching avatar URL for new post: ${error.message}")

                }
            })
    }

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

    /**
     * Starts listening for real-time updates on the "Posts" node in Firebase.
     */
    private fun cargarPostsDesdeFirebase() {
        Log.d("FeedViewModel", "Starting Firebase post listener (orderByChild timestamp)")
        postsRef.orderByChild("timestamp").addValueEventListener(postListener)
    }

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d("FeedViewModel", "Stopping Firebase post listener")
        postsRef.removeEventListener(postListener)
    }
}