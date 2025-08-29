package com.unison.appcomprayventa

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.unison.appcomprayventa.Fragmentos.FragmentFeed
import com.unison.appcomprayventa.Fragmentos.FragmentNotificaciones
import com.unison.appcomprayventa.Fragmentos.FragmentPerfil
import com.unison.appcomprayventa.Fragmentos.FragmentVideos
import com.unison.appcomprayventa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        verFragmentoFeed()

        binding.BottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Item_Feed->{
                    verFragmentoFeed()
                true }
                R.id.Item_Videos->{
                    verFragmentoVideos()
                true }
                R.id.Item_Notificaciones->{
                    verFragmentoNotificaciones()
                true }
                R.id.Item_Perfil->{
                    verFragmentoPerfil()
                true }
                else -> {
                    false
                }
            }
        }
    }

    private fun verFragmentoFeed(){
        binding.TituloRL.text="Feed Principal"
        val fragment = FragmentFeed()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentInicio")
        fragmentTransition.commit()
    }

    private fun verFragmentoVideos(){
        binding.TituloRL.text="Videos"
        val fragment = FragmentVideos()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentChats")
        fragmentTransition.commit()
    }

    private fun verFragmentoNotificaciones(){
        binding.TituloRL.text="Notificaciones"
        val fragment = FragmentNotificaciones()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentMisAnuncios")
        fragmentTransition.commit()
    }

    private fun verFragmentoPerfil(){
        binding.TituloRL.text="Mi Perfil"
        val fragment = FragmentPerfil()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentCuenta")
        fragmentTransition.commit()
    }
}