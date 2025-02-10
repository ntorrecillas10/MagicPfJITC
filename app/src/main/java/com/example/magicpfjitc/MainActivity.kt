package com.example.magicpfjitc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.magicpfjitc.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var cartasList: MutableList<Carta>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cartasList = mutableListOf()

        auth = FirebaseAuth.getInstance()

        Log.d("MainActivity", auth.currentUser?.email.toString())
        val admin = intent.getBooleanExtra("admin", false)

        binding.recyclerCartas.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerCartas.adapter = CartaAdapter(cartasList)

        if (!admin) {
            binding.btnFlotante.visibility = View.GONE
        }
        binding.btnFlotante.setOnClickListener {
            val intent = Intent(this, CrearCarta::class.java)
            startActivity(intent)
        }


        binding.btn4.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }



    }
}