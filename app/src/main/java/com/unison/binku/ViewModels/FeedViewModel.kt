package com.unison.binku.ViewModels // Or your correct package name

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Models.ModeloPost // Ensure correct import

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
                        // --- OBTENER URL AVATAR GUARDADA ---
                        post.urlAvatarAutor = ds.child("urlAvatarAutor").getValue(String::class.java) ?: ""
                        // --- FIN ---
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

    /**
     * Toggles the like status for a given post using a Firebase Transaction.
     */
    fun toggleLikePost(postId: String) {
        // ... (Transaction logic remains the same) ...
    }


    /**
     * Adds a new post to Firebase Database under the "Posts" node,
     * including the author's current avatar URL.
     */
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
                        urlAvatarAutor = avatarUrl // <-- Guardar la URL del avatar
                        // isLiked is calculated locally
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
                    // Opcional: Podrías guardar el post sin avatar o mostrar un error
                    // Por ahora, no se guardará el post si falla la lectura del avatar
                    // O podrías llamar a una función para guardar SIN avatar aquí
                    // guardarPostSinAvatar(postText, imageUriString, location, currentUser)
                }
            })
        // --- >>> FIN OBTENER AVATAR <<< ---
    }


    /**
     * Deletes a post from Firebase Database.
     */
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