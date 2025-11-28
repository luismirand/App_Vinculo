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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.unison.binku.Adaptadores.AdaptadorMensajes
import com.unison.binku.Models.ModeloMensaje
import com.unison.binku.ViewModels.ChatRoomVMFactory
import com.unison.binku.ViewModels.ChatRoomViewModel
import com.unison.binku.databinding.ActivityChatRoomBinding

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var viewModel: ChatRoomViewModel
    private lateinit var adapter: AdaptadorMensajes
    private lateinit var layoutManager: LinearLayoutManager

    private var otroUsuarioId: String = ""
    private var listaMensajes = ArrayList<ModeloMensaje>()

    private var imageUri: Uri? = null // Para guardar la URI de la cámara/galería

    // Launcher para Galería (este estaba bien)
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.ivPreview.setImageURI(imageUri)
            binding.previewContainer.visibility = View.VISIBLE
        } else {
            // El usuario canceló la galería
            imageUri = null
        }
    }

    // Usamos el contrato 'TakePicture' que es más robusto
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // La imagen se guardó exitosamente en la 'imageUri' que creamos en 'abrirCamara'
            imageUri?.let {
                binding.ivPreview.setImageURI(it)
                binding.previewContainer.visibility = View.VISIBLE
            }
        } else {
            // El usuario canceló la cámara
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            imageUri = null // Limpiamos la URI si la captura falló o se canceló
        }
    }
    // --- >>> FIN DE LA CORRECCIÓN <<< ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener el ID del usuario
        otroUsuarioId = intent.getStringExtra("OTRO_USER_ID") ?: ""
        if (otroUsuarioId.isEmpty()) {
            Toast.makeText(this, "Error: No se especificó el usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Configurar ViewModel
        val factory = ChatRoomVMFactory(application, otroUsuarioId)
        viewModel = ViewModelProvider(this, factory)[ChatRoomViewModel::class.java]

        // 3. Configurar Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }
        viewModel.nombreOtroUsuario.observe(this) { nombre ->
            binding.tvToolbarNombre.text = nombre
        }
        viewModel.avatarOtroUsuario.observe(this) { avatarUrl ->
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_perfil_black)
                .error(R.drawable.ic_perfil_black)
                .circleCrop()
                .into(binding.ivToolbarAvatar)
        }

        // Listeners para ir al perfil
        val profileClickListener = View.OnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("USER_ID", otroUsuarioId)
            startActivity(intent)
        }
        binding.toolbarProfileClickableArea.setOnClickListener(profileClickListener)


        // 4. Configurar RecyclerView
        setupRecyclerView()

        // 5. Configurar Botón de Enviar
        binding.btnEnviar.setOnClickListener {
            enviarMensaje()
        }
        binding.etMensaje.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarMensaje(); true
            } else false
        }

        // Botones de adjuntar y quitar preview
        binding.btnAdjuntar.setOnClickListener {
            mostrarOpcionesAdjuntar()
        }
        binding.btnQuitarPreview.setOnClickListener {
            imageUri = null
            binding.previewContainer.visibility = View.GONE
        }

        // 6. Observar nuevos mensajes
        observeViewModel()
    }

    // Mostrar Popup para Cámara/Galería
    private fun mostrarOpcionesAdjuntar() {
        val popupMenu = PopupMenu(this, binding.btnAdjuntar)
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

    // --- >>> ¡LÓGICA DE CÁMARA CORREGIDA! <<< ---
    private fun abrirCamara() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Binku_Chat_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Foto para Binku Chat")
        }
        // Crear el URI y guardarlo en la variable de clase *antes* de lanzar la cámara
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (imageUri == null) {
            Toast.makeText(this, "No se pudo crear archivo para la foto", Toast.LENGTH_SHORT).show()
            return
        }

        // Lanzar el nuevo launcher pasándole el URI donde debe guardar la foto
        cameraLauncher.launch(imageUri)
    }
    // --- >>> FIN DE LA CORRECCIÓN <<< ---

    private fun setupRecyclerView() {
        adapter = AdaptadorMensajes(this, listaMensajes)
        layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvMensajes.layoutManager = layoutManager
        binding.rvMensajes.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.mensajes.observe(this) { mensaje ->
            adapter.addMensaje(mensaje)
            binding.rvMensajes.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    // Envía texto E imagen
    private fun enviarMensaje() {
        val texto = binding.etMensaje.text.toString().trim()
        val imagenUriString = imageUri?.toString()

        if (texto.isNotEmpty() || imagenUriString != null) {
            viewModel.enviarMensaje(texto, imagenUriString)
            binding.etMensaje.setText("") // Limpiar el campo
            // Limpiar preview
            imageUri = null
            binding.previewContainer.visibility = View.GONE
        }
    }
}