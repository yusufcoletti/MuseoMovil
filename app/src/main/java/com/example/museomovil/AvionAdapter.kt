package com.example.museomovil

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class AvionAdapter(private val listaAviones: List<Avion>, private val context: Context) :
    RecyclerView.Adapter<AvionAdapter.AvionViewHolder>() {

    class AvionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvion: ImageView = view.findViewById(R.id.imgAvion)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreAvion)
        val txtEstado: TextView = view.findViewById(R.id.txtEstado)
        val overlay: View = view.findViewById(R.id.overlayBloqueo)
        val iconCandado: ImageView = view.findViewById(R.id.iconCandado)
        val layoutBotones: LinearLayout = view.findViewById(R.id.layoutBotones)
        val btn3D: Button = view.findViewById(R.id.btnVer3D)
        val btnInfo: Button = view.findViewById(R.id.btnInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avion, parent, false)
        return AvionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvionViewHolder, position: Int) {
        val avion = listaAviones[position]

        // 1. Cargar datos básicos
        holder.txtNombre.text = avion.nombre
        holder.imgAvion.setImageResource(avion.imagenResId)

        // 2. LÓGICA DE GAMIFICACIÓN (Bloqueado vs Desbloqueado)
        if (avion.isDesbloqueado) {
            // ESTADO: DESBLOQUEADO
            holder.overlay.visibility = View.GONE       // Quitamos la capa gris
            holder.iconCandado.visibility = View.GONE   // Quitamos el candado
            holder.layoutBotones.visibility = View.VISIBLE // Enseñamos los botones

            holder.txtEstado.text = "DESBLOQUEADO"
            holder.txtEstado.setTextColor(context.getColor(android.R.color.holo_green_dark))

            // Acción del botón 3D -> Ir a tu pantalla de Vista3DActivity
            holder.btn3D.setOnClickListener {
                val intent = Intent(context, Vista3DActivity::class.java)
                // Aquí podrías pasar datos extra si quisieras cargar modelos distintos
                context.startActivity(intent)
            }

            // Acción del botón INFO -> Abrir Bottom Sheet
            holder.btnInfo.setOnClickListener {
                // 1. Crear el diálogo
                val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
                val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
                dialog.setContentView(view)

                // 2. Conectar los elementos del diseño
                val txtTitulo = view.findViewById<TextView>(R.id.sheetTitulo)
                val txtDesc = view.findViewById<TextView>(R.id.sheetDescripcion)
                val btnVerMas = view.findViewById<Button>(R.id.btnVerMasDetalles)

                // 3. Poner la información BREVE inicialmente
                txtTitulo.text = avion.nombre
                txtDesc.text = avion.descripcion

                // 4. Lógica del botón "VER MÁS DETALLES"
                btnVerMas.setOnClickListener {
                    // Al pulsar, cambiamos el texto por la versión LARGA
                    txtDesc.text = avion.descripcionDetallada
                    // Y ocultamos el botón porque ya no hace falta
                    btnVerMas.visibility = View.GONE
                }

                // 5. Mostrar la pestaña
                dialog.show()
            }

        } else {
            // ESTADO: BLOQUEADO
            holder.overlay.visibility = View.VISIBLE
            holder.iconCandado.visibility = View.VISIBLE
            holder.layoutBotones.visibility = View.GONE // Ocultamos botones

            holder.txtEstado.text = "BLOQUEADO - Escanea el NFC en la sala"
            holder.txtEstado.setTextColor(context.getColor(android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount() = listaAviones.size
}