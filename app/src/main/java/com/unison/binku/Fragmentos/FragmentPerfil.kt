package com.unison.binku.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
// --- >>> AÑADIR ESTOS IMPORTS <<< ---
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
// --- >>> FIN DE IMPORTS <<< ---
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unison.binku.Constantes
import com.unison.binku.EditarPerfil
import com.unison.binku.OpcionesLogin // Corregido el import a OpcionesLogin
import com.unison.binku.databinding.FragmentPerfilBinding
import com.unison.binku.R


class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mContext: Context
    private lateinit var mGoogleSignInClient: GoogleSignInClient


    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPerfilBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)


        leerInfo()

        binding.BtnEditarPerfil.setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }

        binding.BtnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            mGoogleSignInClient.signOut().addOnCompleteListener {

                if (isAdded && mContext != null) {
                    val intent = Intent(mContext, OpcionesLogin::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    mContext.startActivity(intent)
                    activity?.finishAffinity()
                }
            }
        }
    }

    private fun leerInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombres = "${snapshot.child("nombres").value}"
                val email = "${snapshot.child("email").value}"
                val imagen = "${snapshot.child("urlImagenPerfil").value}"
                val f_nac = "${snapshot.child("fecha_nac").value}"
                var tiempo = "${snapshot.child("tiempo").value}"
                val telefono = "${snapshot.child("telefono").value}"
                val codTelefono= "${snapshot.child("codigoTelefono").value}"
                val proveedor = "${snapshot.child("proveedor").value}"

                val cod_tel = codTelefono + telefono

                if(tiempo == "null" || tiempo.isEmpty()){
                    tiempo = "0"
                }
                val for_tiempo = Constantes.obtenerFecha(tiempo.toLong())

                binding.TvEmail.text =email
                binding.TvNombres.text = nombres
                binding.TvNacimiento.text = f_nac
                binding.TvTelefono.text = cod_tel
                binding.TvMiembroDesde.text = for_tiempo

                try{
                    if (imagen.isNotEmpty() && imagen != "null") {
                        Glide.with(mContext)
                            .load(imagen)
                            .placeholder(R.drawable.ic_perfil_black)
                            .error(R.drawable.ic_perfil_black)
                            .into(binding.TvPerfil)
                    } else {
                        binding.TvPerfil.setImageResource(R.drawable.ic_perfil_black)
                    }
                } catch (e: Exception){
                    Toast.makeText(mContext, "${e.message}", Toast.LENGTH_SHORT).show()
                }

                if (proveedor == "Email") {
                    firebaseAuth.currentUser?.let { user ->
                        if (user.isEmailVerified) {
                            binding.TvEstadoCuenta.text = "Verificado"
                        } else {
                            binding.TvEstadoCuenta.text = "No Verificado"
                        }
                    } ?: run {
                        binding.TvEstadoCuenta.text = "No Verificado"
                    }
                } else {
                    binding.TvEstadoCuenta.text = "Verificado"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(mContext, "Error al cargar los datos.", Toast.LENGTH_SHORT).show()
            }

        })
    }
}