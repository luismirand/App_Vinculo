package com.unison.binku

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.unison.binku.databinding.ActivityRegistroEmailBinding
import java.util.Calendar // <-- AÑADIR IMPORT para el calendario

class Registro_email : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroEmailBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.BtnRegistrar.setOnClickListener {
            validarInfo()
        }

        binding.EtFNac.setOnClickListener {
            mostrarSelectorFecha()
        }
    }

    private fun mostrarSelectorFecha() {
        val c = Calendar.getInstance()
        val anio = c.get(Calendar.YEAR)
        val mes = c.get(Calendar.MONTH)
        val dia = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            binding.EtFNac.setText(fechaSeleccionada)
        }, anio, mes, dia)

        // Opcional: Poner una fecha máxima (ej. que no sea menor de 13 años)
        c.add(Calendar.YEAR, -13)
        datePickerDialog.datePicker.maxDate = c.timeInMillis

        datePickerDialog.show()
    }

    private var email = ""
    private var password = ""
    private var r_password = ""
    private var nombres = ""
    private var apellidos = ""
    private var telefono = ""
    private var codTelefono = ""
    private var f_nac = ""

    private fun validarInfo() {
        nombres = binding.EtNombres.text.toString().trim()
        apellidos = binding.EtApellidos.text.toString().trim()
        telefono = binding.EtTelefono.text.toString().trim()
        codTelefono = binding.selectorCod.selectedCountryCodeWithPlus
        f_nac = binding.EtFNac.text.toString().trim()

        email = binding.EtEmail.text.toString().trim()
        password = binding.EtPassword.text.toString().trim()
        r_password = binding.EtRPassword.text.toString().trim()

        if (nombres.isEmpty()) {
            binding.EtNombres.error = "Ingrese su nombre"
            binding.EtNombres.requestFocus()
        }
        else if (apellidos.isEmpty()) {
            binding.EtApellidos.error = "Ingrese sus apellidos"
            binding.EtApellidos.requestFocus()
        }
        else if (telefono.isEmpty()) {
            binding.EtTelefono.error = "Ingrese su teléfono"
            binding.EtTelefono.requestFocus()
        }
        else if (f_nac.isEmpty()) {
            binding.EtFNac.error = "Seleccione su fecha de nacimiento"
            binding.EtFNac.requestFocus() // Opcional, ya que es un selector
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.EtEmail.error = "Email inválido"
            binding.EtEmail.requestFocus()
        }
        else if(password.isEmpty()){
            binding.EtPassword.error = "Ingrese la contraseña"
            binding.EtPassword.requestFocus()
        }
        else if(r_password.isEmpty()){
            binding.EtRPassword.error = "Repita la contraseña"
            binding.EtRPassword.requestFocus()
        }
        else if(password != r_password){
            binding.EtRPassword.error = "Las contraseñas no coinciden"
            binding.EtRPassword.requestFocus()
        }
        else{
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                llenarInfoBD()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this,
                    "ERROR DE REGISTRO: ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun llenarInfoBD() {
        progressDialog.setMessage("Guardando información")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid

        val hashMap = HashMap<String, Any?>()
        hashMap["nombres"] = "$nombres $apellidos" // Juntamos nombre y apellido
        hashMap["codigoTelefono"] = codTelefono
        hashMap["telefono"] = telefono
        hashMap["fecha_nac"] = f_nac

        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Email"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["email"] = "${emailUsuario}"
        hashMap["uid"] = "${uidUsuario}"

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {

                progressDialog.dismiss()
                startActivity(Intent(this,MainActivity::class.java))
                finishAffinity()

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this,"No se registró debido a ${exception.message}",
                    Toast.LENGTH_SHORT)
                    .show()
            }
    }

}