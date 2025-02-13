package com.example.magicpfjitc

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.magicpfjitc.databinding.ItemEventoBinding

class EventoAdapter(originalList: List<Evento>, private val recyclerPadre: RecyclerView) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>()  {

    private var displayedList: List<Evento> = originalList // Lista que se muestra actualmente

    inner class EventoViewHolder(val binding: ItemEventoBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventoAdapter.EventoViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: EventoAdapter.EventoViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
    fun updateList(newList: List<Evento>) {
        displayedList = newList
        notifyDataSetChanged()
    }

}