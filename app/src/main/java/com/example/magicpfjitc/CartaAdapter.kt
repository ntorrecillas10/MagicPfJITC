package com.example.magicpfjitc

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.magicpfjitc.databinding.ItemCartaBinding
import com.google.android.gms.auth.api.signin.internal.Storage
import io.appwrite.Client
import com.google.firebase.database.DatabaseReference

class CartaAdapter(originalList: List<Carta>): RecyclerView.Adapter<CartaAdapter.CartaViewHolder>(){

    private var displayedList: List<Carta> = originalList // Lista que se muestra actualmente

    inner class CartaViewHolder(val binding: ItemCartaBinding) : RecyclerView.ViewHolder(binding.root)


    // Appwrite
    private lateinit var appWriteClient: Client
    private lateinit var storage: Storage
    private lateinit var miBucketId: String
    private lateinit var miProyectoId: String
    private lateinit var refBD: DatabaseReference


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartaAdapter.CartaViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: CartaAdapter.CartaViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

}