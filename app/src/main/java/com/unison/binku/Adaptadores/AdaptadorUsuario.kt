package com.unison.binku.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unison.binku.Models.ModeloUsuario
import com.unison.binku.R
import com.unison.binku.databinding.ItemUsuarioBinding

enum class TipoListaUsuario { AMIGOS, SOLICITUDES, BUSQUEDA }

class AdaptadorUsuario(
    private val context: Context,
    private var userList: List<ModeloUsuario>,
    private val listType: TipoListaUsuario,
    private val onActionClick: (String) -> Unit,         // Agregar / Eliminar
    private val onAcceptClick: (String) -> Unit = {},    // Aceptar solicitud
    private val onDeclineClick: (String) -> Unit = {},   // Rechazar solicitud
    private val onItemClick: (ModeloUsuario) -> Unit = {}// ← NUEVO: abrir perfil
) : RecyclerView.Adapter<AdaptadorUsuario.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUsuarioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(context), parent, false)
        return UserViewHolder(binding)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.binding.tvNombreUsuario.text = user.nombres

        if (user.urlImagenPerfil.isNotEmpty()) {
            Glide.with(context)
                .load(user.urlImagenPerfil)
                .placeholder(R.drawable.ic_perfil_white)
                .error(R.drawable.ic_perfil_white)
                .circleCrop()
                .into(holder.binding.ivAvatarUsuario)
        } else {
            holder.binding.ivAvatarUsuario.setImageResource(R.drawable.ic_perfil_white)
        }

        // Click en toda la fila → abrir perfil del usuario
        holder.binding.root.setOnClickListener { onItemClick(user) }

        when (listType) {
            TipoListaUsuario.AMIGOS -> {
                holder.binding.btnAccionPrincipal.text = "Eliminar"
                holder.binding.btnAccionPrincipal.visibility = View.VISIBLE
                holder.binding.btnAccionRechazar.visibility = View.GONE
                holder.binding.btnAccionPrincipal.setOnClickListener { onActionClick(user.uid) }
            }
            TipoListaUsuario.SOLICITUDES -> {
                holder.binding.btnAccionPrincipal.text = "Aceptar"
                holder.binding.btnAccionPrincipal.visibility = View.VISIBLE
                holder.binding.btnAccionRechazar.text = "Rechazar"
                holder.binding.btnAccionRechazar.visibility = View.VISIBLE
                holder.binding.btnAccionPrincipal.setOnClickListener { onAcceptClick(user.uid) }
                holder.binding.btnAccionRechazar.setOnClickListener { onDeclineClick(user.uid) }
            }
            TipoListaUsuario.BUSQUEDA -> {
                holder.binding.btnAccionPrincipal.text = "Agregar"
                holder.binding.btnAccionPrincipal.visibility = View.VISIBLE
                holder.binding.btnAccionRechazar.visibility = View.GONE
                holder.binding.btnAccionPrincipal.setOnClickListener {
                    onActionClick(user.uid)
                    (it as Button).text = "Enviado"
                    it.isEnabled = false
                }
            }
        }
    }

    fun updateUsers(newUsers: List<ModeloUsuario>) {
        userList = newUsers
        notifyDataSetChanged()
    }
}
