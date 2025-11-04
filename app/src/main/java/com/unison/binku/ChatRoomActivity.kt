package com.unison.binku

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener el ID del usuario con quien chateamos
        otroUsuarioId = intent.getStringExtra("OTRO_USER_ID") ?: ""
        if (otroUsuarioId.isEmpty()) {
            Toast.makeText(this, "Error: No se especificó el usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Configurar ViewModel
        val factory = ChatRoomVMFactory(otroUsuarioId)
        viewModel = ViewModelProvider(this, factory)[ChatRoomViewModel::class.java]

        // 3. Configurar Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }
        viewModel.nombreOtroUsuario.observe(this) { nombre ->
            binding.toolbar.title = nombre
        }

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

        // 6. Observar nuevos mensajes
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AdaptadorMensajes(this, listaMensajes)
        layoutManager = LinearLayoutManager(this)
        // Esto hace que el RecyclerView se "ancle" al fondo, perfecto para chats
        layoutManager.stackFromEnd = true
        binding.rvMensajes.layoutManager = layoutManager
        binding.rvMensajes.adapter = adapter
    }

    private fun observeViewModel() {
        // El ViewModel emite UN mensaje a la vez (gracias a addChildEventListener)
        viewModel.mensajes.observe(this) { mensaje ->
            adapter.addMensaje(mensaje)
            // Hacer scroll al nuevo mensaje
            binding.rvMensajes.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun enviarMensaje() {
        val texto = binding.etMensaje.text.toString().trim()
        if (texto.isNotEmpty()) {
            viewModel.enviarMensaje(texto)
            binding.etMensaje.setText("") // Limpiar el campo
        }
    }
}