package com.unison.binku.Fragmentos

import android.content.Context
import android.content.Intent // --- >>> AÑADIR ESTE IMPORT <<< ---
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // (Este ya no lo usamos, pero lo puedes dejar)
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.unison.binku.Adaptadores.AdaptadorChatList
import com.unison.binku.ChatRoomActivity // --- >>> AÑADIR ESTE IMPORT <<< ---
import com.unison.binku.ViewModels.ChatListViewModel
import com.unison.binku.databinding.FragmentChatListBinding

class FragmentChatList : Fragment() {

    private lateinit var binding: FragmentChatListBinding
    private lateinit var mContext: Context
    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: AdaptadorChatList

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    // --- >>> ¡AQUÍ ESTÁ LA CORRECCIÓN! <<< ---
    private fun setupRecyclerView() {
        adapter = AdaptadorChatList(
            context = mContext,
            listaConversaciones = arrayListOf(),
            onItemClick = { conversacion ->

                // 1. Crear el Intent para abrir la sala de chat
                val intent = Intent(mContext, ChatRoomActivity::class.java)

                // 2. Poner el ID del OTRO usuario.
                //    (Esta es la única info que ChatRoomActivity necesita)
                intent.putExtra("OTRO_USER_ID", conversacion.otroUsuarioId)

                // 3. Iniciar la actividad
                startActivity(intent)
            }
        )
        binding.rvConversaciones.layoutManager = LinearLayoutManager(mContext)
        binding.rvConversaciones.adapter = adapter
    }
    // --- >>> FIN DE LA CORRECCIÓN <<< ---

    private fun observeViewModel() {
        viewModel.conversaciones.observe(viewLifecycleOwner) { lista ->
            if (lista.isNullOrEmpty()) {
                binding.rvConversaciones.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvConversaciones.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                adapter.updateList(lista)
            }
        }
    }
}