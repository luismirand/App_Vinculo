package com.unison.binku.Fragmentos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.unison.binku.Adaptadores.AdaptadorUsuario
import com.unison.binku.Adaptadores.TipoListaUsuario
import com.unison.binku.ViewModels.AmigosViewModel
import com.unison.binku.databinding.FragmentAmigosBinding

class FragmentAmigos : Fragment() {

    private lateinit var binding: FragmentAmigosBinding
    private lateinit var mContext: Context

    // Obtener el ViewModel
    private val viewModel: AmigosViewModel by viewModels()

    private lateinit var adaptadorAmigos: AdaptadorUsuario
    private lateinit var adaptadorSolicitudes: AdaptadorUsuario
    private lateinit var adaptadorBusqueda: AdaptadorUsuario

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAmigosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupObservers()
        setupSearch()
    }

    private fun setupAdapters() {
        // Adaptador Amigos
        adaptadorAmigos = AdaptadorUsuario(
            context = mContext,
            userList = emptyList(),
            listType = TipoListaUsuario.AMIGOS,
            onActionClick = { friendId ->
                viewModel.eliminarAmigo(friendId)
                Toast.makeText(mContext, "Amigo eliminado", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvAmigos.layoutManager = LinearLayoutManager(mContext)
        binding.rvAmigos.adapter = adaptadorAmigos

        // Adaptador Solicitudes
        adaptadorSolicitudes = AdaptadorUsuario(
            context = mContext,
            userList = emptyList(),
            listType = TipoListaUsuario.SOLICITUDES,
            onActionClick = {}, // No se usa
            onAcceptClick = { senderId ->
                viewModel.aceptarSolicitud(senderId)
                Toast.makeText(mContext, "Solicitud aceptada", Toast.LENGTH_SHORT).show()
            },
            onDeclineClick = { senderId ->
                viewModel.rechazarSolicitud(senderId)
                Toast.makeText(mContext, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvSolicitudes.layoutManager = LinearLayoutManager(mContext)
        binding.rvSolicitudes.adapter = adaptadorSolicitudes

        // Adaptador Búsqueda
        adaptadorBusqueda = AdaptadorUsuario(
            context = mContext,
            userList = emptyList(),
            listType = TipoListaUsuario.BUSQUEDA,
            onActionClick = { recipientId ->
                viewModel.enviarSolicitud(recipientId)
                Toast.makeText(mContext, "Solicitud enviada", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvBusqueda.layoutManager = LinearLayoutManager(mContext)
        binding.rvBusqueda.adapter = adaptadorBusqueda
    }

    private fun setupObservers() {
        viewModel.amigos.observe(viewLifecycleOwner) { amigos ->
            adaptadorAmigos.updateUsers(amigos)
            binding.tvTituloAmigos.text = "Mis Amigos (${amigos.size})"
        }

        viewModel.solicitudes.observe(viewLifecycleOwner) { solicitudes ->
            adaptadorSolicitudes.updateUsers(solicitudes)
            if (solicitudes.isEmpty()) {
                binding.tvTituloSolicitudes.visibility = View.GONE
                binding.rvSolicitudes.visibility = View.GONE
            } else {
                binding.tvTituloSolicitudes.visibility = View.VISIBLE
                binding.rvSolicitudes.visibility = View.VISIBLE
                binding.tvTituloSolicitudes.text = "Solicitudes Pendientes (${solicitudes.size})"
            }
        }

        viewModel.busqueda.observe(viewLifecycleOwner) { resultados ->
            adaptadorBusqueda.updateUsers(resultados)
        }
    }

    private fun setupSearch() {
        binding.searchViewUsuarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.buscarUsuarios(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // Si la búsqueda está vacía, ocultar resultados y mostrar listas
                    binding.rvBusqueda.visibility = View.GONE
                    binding.layoutListas.visibility = View.VISIBLE
                    viewModel.buscarUsuarios("") // Limpia la lista de búsqueda
                } else {
                    // Si hay texto, mostrar resultados y ocultar listas
                    binding.rvBusqueda.visibility = View.VISIBLE
                    binding.layoutListas.visibility = View.GONE
                    // Opcional: buscar en tiempo real (puede ser costoso)
                    // viewModel.buscarUsuarios(newText)
                }
                return true
            }
        })
    }
}