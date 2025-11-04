package com.unison.binku

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.btnRemovePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComentariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ====== OBTENER POST_ID (acepta ambas llaves por compatibilidad) ======
        postId = intent.getStringExtra("POST_ID")
            ?: intent.getStringExtra("postId")
                    ?: ""

        if (postId.isEmpty()) {
            Toast.makeText(this, "Falta POST_ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ====== ViewModel ======
        viewModel = ViewModelProvider(
            this,
            CommentsVMFactory(postId)
        )[CommentsViewModel::class.java]

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

        // ====== Header del post ======
        // 1) Si vienen extras, pintamos rápido (mejor UX)
        pintarHeaderConExtrasSiHay()
        // 2) Además consultamos DB por si hay cambios o no vinieron extras
        cargarHeaderPostDesdeDB(postId)

        // ====== Acciones ======
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnAddImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnRemovePreview.setOnClickListener {
            selectedImageUri = null
            binding.ivPreview.setImageDrawable(null)
            binding.ivPreview.visibility = View.GONE
            binding.btnRemovePreview.visibility = View.GONE
        }
        binding.btnSend.setOnClickListener { enviarComentario() }

        binding.etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarComentario(); true
            } else false
        }
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

    private fun pintarHeaderConExtrasSiHay() {
        val nombre = intent.getStringExtra("POST_NOMBRE")
        val avatar = intent.getStringExtra("POST_AVATAR")
        val texto = intent.getStringExtra("POST_TEXTO")
        val imagen = intent.getStringExtra("POST_IMAGEN")
        val ubicacion = intent.getStringExtra("POST_UBICACION") ?: ""
        val ts = intent.getLongExtra("POST_TIMESTAMP", 0L)

        if (nombre != null) binding.tvNombreAutor.text = nombre
        if (!avatar.isNullOrBlank()) {
            Glide.with(this).load(avatar).circleCrop().into(binding.ivAvatarAutor)
        }

        if (texto != null) binding.tvTextoPost.text = texto
        if (!imagen.isNullOrBlank()) {
            binding.ivImagenPost.visibility = View.VISIBLE
            Glide.with(this).load(imagen).into(binding.ivImagenPost)
        }

        if (ts > 0L || ubicacion.isNotBlank()) {
            val fecha = formatearFecha(ts)
            binding.tvFechaYUbicacion.text =
                if (ubicacion.isBlank()) fecha else "$fecha · $ubicacion"
        }
    }

    private fun cargarHeaderPostDesdeDB(postId: String) {
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                // OJO: usa los nombres reales de tu DB
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
                    Glide.with(this@ComentariosActivity).load(imagenUrl).into(binding.ivImagenPost)
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
                                        .load(avatar).circleCrop().into(binding.ivAvatarAutor)
                                } else {
                                    binding.ivAvatarAutor.setImageResource(R.drawable.ic_perfil_white)
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
