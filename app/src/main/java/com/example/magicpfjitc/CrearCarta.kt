package com.example.magicpfjitc

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityCrearCartaBinding

class CrearCarta : AppCompatActivity() {


    private lateinit var binding: ActivityCrearCartaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)




    }
}