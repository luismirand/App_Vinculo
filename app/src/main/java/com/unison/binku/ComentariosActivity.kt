package com.unison.binku

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.unison.binku.Adaptadores.AdaptadorComentarios
import com.unison.binku.ViewModels.CommentsVMFactory
import com.unison.binku.ViewModels.CommentsViewModel
import com.unison.binku.databinding.ActivityComentariosBinding
import java.text.SimpleDateFormat
import java.util.*

class ComentariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComentariosBinding
    private lateinit var viewModel: CommentsViewModel
    private lateinit var adapter: AdaptadorComentarios

    private var postId: String = ""
    private var selectedImageUri: Uri? = null // Usado por ambos launchers

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.btnRemovePreview.visibility = View.VISIBLE
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.ivPreview.setImageURI(selectedImageUri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.btnRemovePreview.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            selectedImageUri = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComentariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra("POST_ID")
            ?: intent.getStringExtra("postId")
                    ?: ""

        if (postId.isEmpty()) {
            Toast.makeText(this, "Falta POST_ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ====== ViewModel ======
        val factory = CommentsVMFactory(application, postId)
        viewModel = ViewModelProvider(this, factory)[CommentsViewModel::class.java]

        // ====== Recycler + Adapter ======
        adapter = AdaptadorComentarios(
            context = this,
            comentarios = arrayListOf(),
            onLikeClick = { commentId -> viewModel.toggleLike(commentId) }
        )
        binding.rvComentarios.layoutManager = LinearLayoutManager(this)
        binding.rvComentarios.adapter = adapter

        viewModel.comments.observe(this) { list ->
            adapter.update(list ?: arrayListOf())
            if (!list.isNullOrEmpty()) binding.rvComentarios.smoothScrollToPosition(list.size - 1)
        }

        // --- NUEVO: Observar Likes del Post ---
        viewModel.postLikes.observe(this) { likes ->
            binding.tvHeaderLikeCountText.text = likes.toString()
        }
        viewModel.isPostLiked.observe(this) { isLiked ->
            val likeIconColor = if (isLiked) R.color.guinda else R.color.gris_oscuro
            binding.btnHeaderLike.setIconTintResource(likeIconColor)
        }
        // --- FIN DE NUEVO ---

        // ====== Header del post ======
        pintarHeaderConExtrasSiHay()
        cargarHeaderPostDesdeDB(postId) // Esto sobreescribirá los likes con datos en vivo

        // ====== Acciones (MODIFICADO) ======
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnAddImage.setOnClickListener { mostrarOpcionesAdjuntar() }
        binding.btnRemovePreview.setOnClickListener {
            selectedImageUri = null
            binding.ivPreview.setImageDrawable(null)
            binding.ivPreview.visibility = View.GONE
            binding.btnRemovePreview.visibility = View.GONE
        }
        binding.btnSend.setOnClickListener { enviarComentario() }
        binding.btnHeaderLike.setOnClickListener { viewModel.toggleLikePost() } // <-- NUEVO

        binding.etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarComentario(); true
            } else false
        }
    }

    // --- NUEVO: Mostrar Popup para Cámara/Galería ---
    private fun mostrarOpcionesAdjuntar() {
        val popupMenu = PopupMenu(this, binding.btnAddImage)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Cámara")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galería")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { // Cámara
                    abrirCamara()
                    true
                }
                2 -> { // Galería
                    galeriaLauncher.launch("image/*")
                    true
                }
                else -> false
            }
        }
    }

    // --- NUEVO: Lógica para abrir la cámara ---
    private fun abrirCamara() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Binku_Comentario_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Foto para Comentario Binku")
        }
        selectedImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (selectedImageUri == null) {
            Toast.makeText(this, "No se pudo crear archivo para la foto", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri)
        }
        cameraActivityResultLauncher.launch(intent)
    }

    private fun enviarComentario() {
        val text = binding.etComment.text?.toString()?.trim() ?: ""
        if (text.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Escribe algo o agrega una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.agregarComentario(text, selectedImageUri?.toString())

        // limpiar
        binding.etComment.setText("")
        selectedImageUri = null
        binding.ivPreview.setImageDrawable(null)
        binding.ivPreview.visibility = View.GONE
        binding.btnRemovePreview.visibility = View.GONE
    }

    // --- MODIFICADO: Para leer y mostrar el estado inicial de likes ---
    private fun pintarHeaderConExtrasSiHay() {
        val nombre = intent.getStringExtra("POST_NOMBRE")
        val avatar = intent.getStringExtra("POST_AVATAR")
        val texto = intent.getStringExtra("POST_TEXTO")
        val imagen = intent.getStringExtra("POST_IMAGEN")
        val ubicacion = intent.getStringExtra("POST_UBICACION") ?: ""
        val ts = intent.getLongExtra("POST_TIMESTAMP", 0L)
        val likes = intent.getIntExtra("POST_LIKE_COUNT", 0) // <-- MODIFICADO
        val isLiked = intent.getBooleanExtra("POST_IS_LIKED", false) // <-- NUEVO

        if (nombre != null) binding.tvNombreAutor.text = nombre
        if (!avatar.isNullOrBlank()) {
            Glide.with(this)
                .load(avatar)
                .placeholder(R.drawable.ic_perfil_black)
                .error(R.drawable.ic_perfil_black)
                .circleCrop()
                .into(binding.ivAvatarAutor)
        } else {
            binding.ivAvatarAutor.setImageResource(R.drawable.ic_perfil_black)
        }

        if (texto != null) binding.tvTextoPost.text = texto
        if (!imagen.isNullOrBlank()) {
            binding.ivImagenPost.visibility = View.VISIBLE
            Glide.with(this)
                .load(imagen)
                .placeholder(R.drawable.ic_perfil_black)
                .error(R.drawable.ic_perfil_black)
                .into(binding.ivImagenPost)
        }

        if (ts > 0L || ubicacion.isNotBlank()) {
            val fecha = formatearFecha(ts)
            binding.tvFechaYUbicacion.text =
                if (ubicacion.isBlank()) fecha else "$fecha · $ubicacion"
        }

        // --- NUEVO: Asignar estado inicial de likes ---
        binding.tvHeaderLikeCountText.text = likes.toString()
        val likeIconColor = if (isLiked) R.color.guinda else R.color.gris_oscuro
        binding.btnHeaderLike.setIconTintResource(likeIconColor)
        // --- FIN DE NUEVO ---
    }

    private fun cargarHeaderPostDesdeDB(postId: String) {
        // Esta función ahora solo cargará la info estática
        // Los likes se actualizan en vivo por el listener del ViewModel
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val texto = snap.child("textoPost").getValue(String::class.java)
                    ?: snap.child("texto").getValue(String::class.java) ?: ""
                val imagenUrl = snap.child("imagenUrlPost").getValue(String::class.java)
                    ?: snap.child("imagenUrl").getValue(String::class.java) ?: ""
                val ubicacion = snap.child("ubicacion").getValue(String::class.java) ?: ""
                val ts = snap.child("timestamp").getValue(Long::class.java) ?: 0L
                val uidAutor = snap.child("uidAutor").getValue(String::class.java) ?: ""

                binding.tvTextoPost.text = texto
                if (imagenUrl.isNotBlank()) {
                    binding.ivImagenPost.visibility = View.VISIBLE
                    Glide.with(this@ComentariosActivity)
                        .load(imagenUrl)
                        .placeholder(R.drawable.ic_perfil_black)
                        .error(R.drawable.ic_perfil_black)
                        .into(binding.ivImagenPost)
                } else {
                    binding.ivImagenPost.visibility = View.GONE
                }

                val fecha = formatearFecha(ts)
                binding.tvFechaYUbicacion.text =
                    if (ubicacion.isBlank()) fecha else "$fecha · $ubicacion"

                if (uidAutor.isNotBlank()) {
                    FirebaseDatabase.getInstance()
                        .getReference("Usuarios")
                        .child(uidAutor)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(uSnap: DataSnapshot) {
                                val nombre = uSnap.child("nombres").getValue(String::class.java) ?: "Usuario"
                                val avatar = uSnap.child("urlImagenPerfil").getValue(String::class.java) ?: ""
                                binding.tvNombreAutor.text = nombre
                                if (avatar.isNotBlank()) {
                                    Glide.with(this@ComentariosActivity)
                                        .load(avatar)
                                        .placeholder(R.drawable.ic_perfil_black)
                                        .error(R.drawable.ic_perfil_black)
                                        .circleCrop()
                                        .into(binding.ivAvatarAutor)
                                } else {
                                    binding.ivAvatarAutor.setImageResource(R.drawable.ic_perfil_black)
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ComentariosActivity, "No se pudo cargar el post", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatearFecha(ts: Long): String {
        if (ts <= 0L) return ""
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}