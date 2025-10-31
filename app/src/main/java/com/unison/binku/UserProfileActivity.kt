package com.unison.binku

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.unison.binku.Adaptadores.AdaptadorPost
import com.unison.binku.Adaptadores.AdaptadorUsuario
import com.unison.binku.Adaptadores.TipoListaUsuario
import com.unison.binku.Models.ModeloPost
import com.unison.binku.Models.ModeloUsuario
import com.unison.binku.databinding.ActivityUserProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    private val usuariosRef = db.getReference("Usuarios")
    private val amigosRef = db.getReference("Amigos")
    private val postsRef = db.getReference("Posts")
    private val solicitudesRef = db.getReference("SolicitudesPendientes")

    private lateinit var postsAdapter: AdaptadorPost
    private lateinit var amigosAdapter: AdaptadorUsuario

    private var viewingUserId: String = ""
    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewingUserId = intent.getStringExtra("USER_ID") ?: ""

        setupLists()
        loadUserInfo()
        loadUserPosts()
        loadUserFriends()

        setupButtons()
    }

    private fun setupLists() {
        // POSTS
        postsAdapter = AdaptadorPost(
            context = this,
            postList = arrayListOf(),
            onDeleteClick = { postId -> deletePost(postId) },
            onLikeClick = { postId -> toggleLikePost(postId) }
        )
        binding.rvUserPosts.layoutManager = LinearLayoutManager(this)
        binding.rvUserPosts.adapter = postsAdapter

        // AMIGOS (lista simple, sin acciones)
        amigosAdapter = AdaptadorUsuario(
            context = this,
            userList = emptyList(),
            listType = TipoListaUsuario.AMIGOS,
            onActionClick = {}, // no acciones aquí
            onItemClick = { user -> // navegar al perfil del amigo del amigo
                if (user.uid.isNotEmpty() && user.uid != viewingUserId) {
                    startActivity(Intent(this, UserProfileActivity::class.java).putExtra("USER_ID", user.uid))
                }
            }
        )
        binding.rvUserFriends.layoutManager = LinearLayoutManager(this)
        binding.rvUserFriends.adapter = amigosAdapter
    }

    private fun setupButtons() {
        val isOwn = viewingUserId.isNotEmpty() && viewingUserId == currentUserId

        // Propios
        binding.btnEditarPerfil.visibility = if (isOwn) View.VISIBLE else View.GONE
        binding.btnCerrarSesion.visibility = if (isOwn) View.VISIBLE else View.GONE

        // Otros
        binding.btnAgregarAmigo.visibility = if (!isOwn) View.VISIBLE else View.GONE
        binding.btnMensaje.visibility = if (!isOwn) View.VISIBLE else View.GONE

        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }
        binding.btnCerrarSesion.setOnClickListener {
            auth.signOut()
            val i = Intent(this, OpcionesLogin::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }
        binding.btnMensaje.setOnClickListener {
            Toast.makeText(this, "Mensajería: por implementar", Toast.LENGTH_SHORT).show()
        }
        binding.btnAgregarAmigo.setOnClickListener {
            if (viewingUserId.isNotEmpty() && currentUserId.isNotEmpty() && viewingUserId != currentUserId) {
                solicitudesRef.child(viewingUserId).child(currentUserId).setValue(true)
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserInfo() {
        if (viewingUserId.isEmpty()) return
        usuariosRef.child(viewingUserId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val nombres = "${s.child("nombres").value}"
                val email = "${s.child("email").value}"
                val imagen = "${s.child("urlImagenPerfil").value}"
                val fNac = "${s.child("fecha_nac").value}"
                val telefono = "${s.child("telefono").value}"
                val codigo = "${s.child("codigoTelefono").value}"
                val ts = "${s.child("tiempo").value}".ifEmpty { "0" }

                binding.tvNombre.text = nombres
                binding.tvEmail.text = email
                binding.tvNacimiento.text = fNac
                binding.tvTelefono.text = (codigo + telefono)
                binding.tvMiembroDesde.text = com.unison.binku.Constantes.obtenerFecha(ts.toLongOrNull() ?: 0L)

                if (imagen.isNotEmpty() && imagen != "null") {
                    Glide.with(this@UserProfileActivity)
                        .load(imagen)
                        .placeholder(R.drawable.ic_login)
                        .into(binding.ivAvatar)
                } else {
                    binding.ivAvatar.setImageResource(R.drawable.ic_login)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadUserPosts() {
        if (viewingUserId.isEmpty()) return
        postsRef.orderByChild("uidAutor").equalTo(viewingUserId)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = arrayListOf<ModeloPost>()
                    for (ds in snapshot.children) {
                        ds.getValue(ModeloPost::class.java)?.let { p ->
                            p.postId = ds.key ?: ""
                            p.contadorLikes = ds.child("contadorLikes").getValue(Int::class.java) ?: 0
                            p.urlAvatarAutor = ds.child("urlAvatarAutor").getValue(String::class.java) ?: ""
                            p.isLikedPorUsuarioActual = if (currentUserId.isNotEmpty()) {
                                ds.child("Likes").hasChild(currentUserId)
                            } else false
                            list.add(p)
                        }
                    }
                    list.sortBy { it.timestamp }
                    list.reverse()
                    postsAdapter.updatePosts(list)
                    binding.tvPostsHeader.text = "Publicaciones (${list.size})"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadUserFriends() {
        if (viewingUserId.isEmpty()) return
        amigosRef.child(viewingUserId).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendIds = snapshot.children.mapNotNull { it.key }
                if (friendIds.isEmpty()) {
                    amigosAdapter.updateUsers(emptyList())
                    binding.tvFriendsHeader.text = "Amigos (0)"
                    return
                }

                lifecycleScope.launch {
                    val users = withContext(Dispatchers.IO) {
                        friendIds.mapNotNull { uid ->
                            try {
                                val ds = usuariosRef.child(uid).get().await()
                                ds.getValue(ModeloUsuario::class.java)?.apply { this.uid = uid }
                            } catch (_: Exception) { null }
                        }
                    }
                    amigosAdapter.updateUsers(users)
                    binding.tvFriendsHeader.text = "Amigos (${users.size})"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleLikePost(postId: String) {
        val uid = currentUserId
        if (uid.isEmpty()) return
        val postRef = postsRef.child(postId)
        postRef.runTransaction(object: Transaction.Handler{
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val likes = mutableData.child("Likes")
                val wasLiked = likes.hasChild(uid)
                val count = (mutableData.child("contadorLikes").value as? Long ?: 0L).toInt()
                if (wasLiked) {
                    likes.child(uid).value = null
                    mutableData.child("contadorLikes").value = (count - 1).coerceAtLeast(0)
                } else {
                    likes.child(uid).value = true
                    mutableData.child("contadorLikes").value = count + 1
                }
                return Transaction.success(mutableData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    private fun deletePost(postId: String) {
        // Solo se mostrará el botón borrar si el post es del usuario logueado (AdaptadorPost ya lo maneja).
        postsRef.child(postId).removeValue()
    }
}
