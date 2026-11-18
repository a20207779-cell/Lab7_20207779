package com.example.myapplication;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.SesionEstudio;

import java.util.List;

public class SesionAdapter extends RecyclerView.Adapter<SesionAdapter.SesionViewHolder> {

    public interface OnSesionClickListener {
        void onEditar(SesionEstudio sesion);
        void onEliminar(SesionEstudio sesion);
    }

    private final List<SesionEstudio> listaSesiones;
    private final OnSesionClickListener listener;

    public SesionAdapter(List<SesionEstudio> listaSesiones,
                         OnSesionClickListener listener) {
        this.listaSesiones = listaSesiones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SesionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sesion, parent, false);

        return new SesionViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull SesionViewHolder holder, int position) {
        SesionEstudio sesion = listaSesiones.get(position);

        holder.tvCurso.setText(sesion.getCurso());
        holder.tvFecha.setText("Fecha: " + sesion.getFecha());
        holder.tvHoras.setText("Horas: " + sesion.getHoras());
        holder.tvActividad.setText("Actividad: " + sesion.getActividad());

        holder.btnEditar.setOnClickListener(v -> listener.onEditar(sesion));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(sesion));
    }

    @Override
    public int getItemCount() {
        return listaSesiones.size();
    }

    public static class SesionViewHolder extends RecyclerView.ViewHolder {

        TextView tvCurso, tvFecha, tvHoras, tvActividad;
        Button btnEditar, btnEliminar;

        public SesionViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCurso = itemView.findViewById(R.id.tvCurso);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvHoras = itemView.findViewById(R.id.tvHoras);
            tvActividad = itemView.findViewById(R.id.tvActividad);

            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}