package com.example.myapplication;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.SesionEstudio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListaSesionesActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView rvSesiones;
    private SesionAdapter adapter;
    private List<SesionEstudio> listaSesiones;
    private ListenerRegistration listenerSesiones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_sesiones);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvSesiones = findViewById(R.id.rvSesiones);
        listaSesiones = new ArrayList<>();

        adapter = new SesionAdapter(listaSesiones,
                new SesionAdapter.OnSesionClickListener() {

                    @Override
                    public void onEditar(SesionEstudio sesion) {
                        Intent intent = new Intent(
                                ListaSesionesActivity.this,
                                FormularioSesionActivity.class
                        );

                        intent.putExtra("sesionId", sesion.getId());
                        intent.putExtra("curso", sesion.getCurso());
                        intent.putExtra("fecha", sesion.getFecha());
                        intent.putExtra("horas", sesion.getHoras());
                        intent.putExtra("actividad", sesion.getActividad());

                        startActivity(intent);
                    }

                    @Override
                    public void onEliminar(SesionEstudio sesion) {
                        new AlertDialog.Builder(ListaSesionesActivity.this)
                                .setTitle("Eliminar sesión")
                                .setMessage("¿Estás seguro de que deseas eliminar esta sesión?")
                                .setPositiveButton("Eliminar", (dialog, which) ->
                                        eliminarSesion(sesion))
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                });

        rvSesiones.setLayoutManager(new LinearLayoutManager(this));
        rvSesiones.setAdapter(adapter);

        Button btnAgregarSesion = findViewById(R.id.btnAgregarSesion);
        btnAgregarSesion.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ListaSesionesActivity.this,
                    FormularioSesionActivity.class
            );
            startActivity(intent);
        });

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> {
            auth.signOut();

            Intent intent = new Intent(
                    ListaSesionesActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });

        Button btnVerGraficos = findViewById(R.id.btnVerGraficos);

        btnVerGraficos.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ListaSesionesActivity.this,
                    GraficosActivity.class
            );

            startActivity(intent);
        });
    }

    private void eliminarSesion(SesionEstudio sesion) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios")
                .document(uid)
                .collection("sesiones")
                .document(sesion.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Sesión eliminada",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al eliminar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        listenerSesiones = db.collection("usuarios")
                .document(uid)
                .collection("sesiones")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "Error al cargar sesiones: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (value != null) {
                        listaSesiones.clear();

                        for (DocumentSnapshot documento : value.getDocuments()) {
                            SesionEstudio sesion =
                                    documento.toObject(SesionEstudio.class);

                            if (sesion != null) {
                                sesion.setId(documento.getId());
                                listaSesiones.add(sesion);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (listenerSesiones != null) {
            listenerSesiones.remove();
            listenerSesiones = null;
        }
    }
}
