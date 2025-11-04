package com.unison.binku

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import java.lang.Exception

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

    private enum class FriendshipStatus {
        OWN_PROFILE,
        FRIENDS,
        PENDING_SENT_BY_ME,
        PENDING_SENT_TO_ME,
        STRANGERS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewingUserId = intent.getStringExtra("USER_ID") ?: ""

        if (viewingUserId.isEmpty()) {
            Toast.makeText(this, "No se pudo cargar el perfil.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupLists()
        loadUserInfo()
        loadUserPosts()
        loadUserFriends()
        setupButtons()
        loadFriendshipStatus()
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

        // AMIGOS
        amigosAdapter = AdaptadorUsuario(
            context = this,
            userList = emptyList(),
            listType = TipoListaUsuario.AMIGOS,
            onActionClick = {},
            onAcceptClick = {},
            onDeclineClick = {},
            onItemClick = { user ->
                if (user.uid.isNotEmpty() && user.uid != viewingUserId) {
                    val intent = Intent(this, UserProfileActivity::class.java).putExtra("USER_ID", user.uid)
                    startActivity(intent)
                }
            }
        )
        binding.rvUserFriends.layoutManager = LinearLayoutManager(this)
        binding.rvUserFriends.adapter = amigosAdapter
    }

    private fun setupButtons() {
        // ... (Tu código de setupButtons va aquí, sin cambios)
        val isOwn = viewingUserId.isNotEmpty() && viewingUserId == currentUserId

        binding.btnEditarPerfil.visibility = if (isOwn) View.VISIBLE else View.GONE
        binding.btnCerrarSesion.visibility = if (isOwn) View.VISIBLE else View.GONE
        binding.btnAgregarAmigo.visibility = View.GONE
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
    }

    // --- >>> FUNCIÓN MODIFICADA <<< ---
    private fun loadUserInfo() {
        if (viewingUserId.isEmpty()) return
        usuariosRef.child(viewingUserId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val nombres = "${s.child("nombres").value}"
                val imagen = "${s.child("urlImagenPerfil").value}"
                val fNac = "${s.child("fecha_nac").value}"
                val ts = "${s.child("tiempo").value}".ifEmpty { "0" }

                binding.tvNombre.text = nombres
                binding.tvEmail.visibility = View.GONE
                binding.tvTelefono.visibility = View.GONE

                if (fNac.isNotEmpty() && fNac != "null") {
                    val cumpleañosFormateado = formatearFechaNacimiento(fNac)
                    binding.tvNacimiento.text = "Cumpleaños: $cumpleañosFormateado"
                    binding.tvNacimiento.visibility = View.VISIBLE
                } else {
                    binding.tvNacimiento.visibility = View.GONE
                }

                binding.tvMiembroDesde.text = "Miembro desde: ${com.unison.binku.Constantes.obtenerFecha(ts.toLongOrNull() ?: 0L)}"

                if (imagen.isNotEmpty() && imagen != "null") {
                    Glide.with(this@UserProfileActivity)
                        .load(imagen)
                        .placeholder(R.drawable.ic_perfil_black)
                        .error(R.drawable.ic_perfil_black)
                        .into(binding.ivAvatar)
                } else {
                    binding.ivAvatar.setImageResource(R.drawable.ic_perfil_black)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfileActivity, "Error al cargar info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatearFechaNacimiento(fechaDDMMAAAA: String): String {
        return try {
            val parts = fechaDDMMAAAA.split("/")
            if (parts.size == 3) {
                val dia = parts[0].toInt()
                val mes = parts[1].toInt()

                val nombreMes = when (mes) {
                    1 -> "enero"
                    2 -> "febrero"
                    3 -> "marzo"
                    4 -> "abril"
                    5 -> "mayo"
                    6 -> "junio"
                    7 -> "julio"
                    8 -> "agosto"
                    9 -> "septiembre"
                    10 -> "octubre"
                    11 -> "noviembre"
                    12 -> "diciembre"
                    else -> ""
                }

                if (nombreMes.isNotEmpty()) {
                    "$dia de $nombreMes" // Ej: "12 de octubre"
                } else {
                    fechaDDMMAAAA // Fallback a la fecha original si el mes es inválido
                }
            } else {
                fechaDDMMAAAA // Fallback si el formato no es DD/MM/AAAA
            }
        } catch (e: Exception) {
            Log.w("UserProfileActivity", "Error al formatear fecha: $fechaDDMMAAAA", e)
            fechaDDMMAAAA // Fallback a la fecha original en caso de error
        }
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

    private fun loadFriendshipStatus() {
        val isOwn = viewingUserId.isNotEmpty() && viewingUserId == currentUserId
        if (isOwn || currentUserId.isEmpty()) {
            updateButtonsForStatus(FriendshipStatus.OWN_PROFILE)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val areFriends = amigosRef.child(currentUserId).child(viewingUserId).get().await().exists()
                if (areFriends) {
                    withContext(Dispatchers.Main) { updateButtonsForStatus(FriendshipStatus.FRIENDS) }
                    return@launch
                }

                val iSentRequest = solicitudesRef.child(viewingUserId).child(currentUserId).get().await().exists()
                if (iSentRequest) {
                    withContext(Dispatchers.Main) { updateButtonsForStatus(FriendshipStatus.PENDING_SENT_BY_ME) }
                    return@launch
                }

                val theySentRequest = solicitudesRef.child(currentUserId).child(viewingUserId).get().await().exists()
                if (theySentRequest) {
                    withContext(Dispatchers.Main) { updateButtonsForStatus(FriendshipStatus.PENDING_SENT_TO_ME) }
                    return@launch
                }

                withContext(Dispatchers.Main) { updateButtonsForStatus(FriendshipStatus.STRANGERS) }

            } catch (e: Exception) {
                Log.e("UserProfileActivity", "Error al verificar amistad", e)
            }
        }
    }

    private fun updateButtonsForStatus(status: FriendshipStatus) {
        binding.btnAgregarAmigo.visibility = View.VISIBLE

        when (status) {
            FriendshipStatus.OWN_PROFILE -> {
                binding.btnAgregarAmigo.visibility = View.GONE
            }
            FriendshipStatus.FRIENDS -> {
                binding.btnAgregarAmigo.text = "Amigos ✅"
                binding.btnAgregarAmigo.isEnabled = false
            }
            FriendshipStatus.PENDING_SENT_BY_ME -> {
                binding.btnAgregarAmigo.text = "Solicitud Enviada"
                binding.btnAgregarAmigo.isEnabled = false
            }
            FriendshipStatus.PENDING_SENT_TO_ME -> {
                binding.btnAgregarAmigo.text = "Aceptar Solicitud"
                binding.btnAgregarAmigo.isEnabled = true
                binding.btnAgregarAmigo.setOnClickListener { acceptFriendRequest() }
            }
            FriendshipStatus.STRANGERS -> {
                binding.btnAgregarAmigo.text = "Agregar Amigo"
                binding.btnAgregarAmigo.isEnabled = true
                binding.btnAgregarAmigo.setOnClickListener { sendFriendRequest() }
            }
        }
    }

    private fun sendFriendRequest() {
        solicitudesRef.child(viewingUserId).child(currentUserId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                loadFriendshipStatus()
            }
    }

    private fun acceptFriendRequest() {
        amigosRef.child(currentUserId).child(viewingUserId).setValue(true)
        amigosRef.child(viewingUserId).child(currentUserId).setValue(true)
            .addOnSuccessListener {
                solicitudesRef.child(currentUserId).child(viewingUserId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Amigo agregado", Toast.LENGTH_SHORT).show()
                        loadFriendshipStatus()
                        loadUserFriends()
                    }
            }
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
        postsRef.child(postId).removeValue()
    }
}