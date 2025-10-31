package com.unison.binku.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.unison.binku.CrearPostActivity
import com.unison.binku.Adaptadores.AdaptadorPost
import com.unison.binku.Models.ModeloPost
import com.unison.binku.ViewModels.FeedViewModel
import com.unison.binku.databinding.FragmentFeedBinding

class FragmentFeed : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var adaptador: AdaptadorPost
    private var items = arrayListOf<ModeloPost>()

    private val crearPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val texto = data.getStringExtra("POST_TEXT") ?: ""
            val imagenUriString = data.getStringExtra("POST_IMAGE_URI")
            val ubicacion = data.getStringExtra("POST_LOCATION") ?: ""
            feedViewModel.agregarPostFirebase(texto, imagenUriString, ubicacion)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)

        // RecyclerView
        adaptador = AdaptadorPost(
            requireContext(),
            items,
            onDeleteClick = { postIdToDelete ->
                feedViewModel.eliminarPostFirebase(postIdToDelete)
            },
            onLikeClick = { postIdToLike ->
                feedViewModel.toggleLikePost(postIdToLike)
            }
        )

        binding.recyclerFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFeed.adapter = adaptador

        // Observa los posts y alterna placeholder
        feedViewModel.posts.observe(viewLifecycleOwner) { list ->
            adaptador.updatePosts(list)
            if (list.isNullOrEmpty()) {
                binding.placeholderContainer.visibility = View.VISIBLE
                binding.recyclerFeed.visibility = View.GONE
            } else {
                binding.placeholderContainer.visibility = View.GONE
                binding.recyclerFeed.visibility = View.VISIBLE
            }
        }

        return binding.root
    }

    /** Llama este método desde tu menú para abrir la pantalla de crear post */
    fun startCrearPost() {
        val intent = Intent(requireContext(), CrearPostActivity::class.java)
        crearPostLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
