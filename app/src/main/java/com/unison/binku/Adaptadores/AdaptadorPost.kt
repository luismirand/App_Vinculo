package com.unison.binku.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unison.binku.Constantes
import com.unison.binku.Models.ModeloPost
import com.unison.binku.R
import com.unison.binku.databinding.ItemPostCardBinding

class AdaptadorPost(
    private val context: Context,
    // List is now mutable and updated via a function
    private var postList: ArrayList<ModeloPost>
) : RecyclerView.Adapter<AdaptadorPost.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return PostViewHolder(binding)
    }

    override fun getItemCount(): Int = postList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.binding.tvUserName.text = post.nombreAutor
        holder.binding.tvPostContent.text = post.textoPost
        holder.binding.tvPostTime.text = Constantes.obtenerFecha(post.timestamp)

        // Load author's profile picture (replace with actual logic if available)
        Glide.with(context)
            .load(R.drawable.ic_perfil) // Placeholder or actual author image URL
            .placeholder(R.drawable.ic_perfil)
            .circleCrop() // Make avatar circular
            .into(holder.binding.ivUserAvatar)

        // Load post image if it exists
        if (post.imagenUrlPost.isNotEmpty()) {
            holder.binding.ivPostImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(post.imagenUrlPost) // Load the image URL
                .placeholder(R.color.guinda_ripple) // Use ripple color as placeholder
                .into(holder.binding.ivPostImage)
        } else {
            holder.binding.ivPostImage.visibility = View.GONE
        }

        // --- Corrected Button Listeners ---
        holder.binding.btnLike.setOnClickListener {
            Toast.makeText(context, "Like post ${post.postId}", Toast.LENGTH_SHORT).show()
        }
        holder.binding.btnComment.setOnClickListener {
            Toast.makeText(context, "Comment on post ${post.postId}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to update the list when LiveData changes
    fun updatePosts(newPosts: ArrayList<ModeloPost>) {
        postList = newPosts
        notifyDataSetChanged() // Re-render the whole list (simple approach)
        // For better performance with large lists, use DiffUtil
    }
}