package com.unison.binku.Fragmentos

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Adaptadores.AdaptadorPost
import com.unison.binku.ViewModels.FeedViewModel
import com.unison.binku.databinding.FragmentFeedBinding

class FragmentFeed : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var mContext: Context
    private lateinit var adaptadorPost: AdaptadorPost
    private lateinit var firebaseAuth: FirebaseAuth

    private val feedViewModel: FeedViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()

        adaptadorPost = AdaptadorPost(
            context = mContext,
            postList = ArrayList(),
            onDeleteClick = { postIdToDelete ->
                feedViewModel.eliminarPostFirebase(postIdToDelete)
            },
            onLikeClick = { postIdToLike ->
                feedViewModel.toggleLikePost(postIdToLike)
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.rvPosts.layoutManager = LinearLayoutManager(mContext)
        binding.rvPosts.adapter = adaptadorPost // Assign initialized adapter

        // Observe LiveData from ViewModel
        feedViewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            Log.d("FragmentFeed", "LiveData observed changes, updating adapter with ${posts.size} posts.")
            adaptadorPost.updatePosts(posts) // Update adapter's list
            actualizarVistaFeed(posts.isEmpty()) // Update placeholder visibility
        })
    }

    // Function to show/hide placeholder
    private fun actualizarVistaFeed(isEmpty: Boolean) {
        Log.d("FragmentFeed", "Updating feed view, isEmpty: $isEmpty")
        if (isEmpty) {
            binding.placeholderContainer.visibility = View.VISIBLE
            binding.rvPosts.visibility = View.GONE
        } else {
            binding.placeholderContainer.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        }
    }
}