package com.example.myapplication;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormularioSesionActivity extends AppCompatActivity {

    private EditText etCurso, etFecha, etHoras;
    private Spinner spActividad;
    private Button btnGuardarSesion, btnCancelar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String fechaFirestore = "";
    private String sesionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_sesion);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etCurso = findViewById(R.id.etCurso);
        etFecha = findViewById(R.id.etFecha);
        etHoras = findViewById(R.id.etHoras);
        spActividad = findViewById(R.id.spActividad);

        btnGuardarSesion = findViewById(R.id.btnGuardarSesion);
        btnCancelar = findViewById(R.id.btnCancelar);

        String[] actividades = {"Teoría", "Práctica", "Laboratorio"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                actividades
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spActividad.setAdapter(adapter);

        // Revisar si estamos editando una sesión existente
        sesionId = getIntent().getStringExtra("sesionId");

        if (sesionId != null) {
            cargarDatosParaEditar();
            btnGuardarSesion.setText("Actualizar sesión");
        }

        etFecha.setOnClickListener(v -> mostrarCalendario());

        btnGuardarSesion.setOnClickListener(v -> guardarSesion());

        btnCancelar.setOnClickListener(v -> finish());
    }

    private void cargarDatosParaEditar() {
        etCurso.setText(getIntent().getStringExtra("curso"));

        fechaFirestore = getIntent().getStringExtra("fecha");

        if (fechaFirestore != null) {
            try {
                SimpleDateFormat formatoBD =
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                SimpleDateFormat formatoPantalla =
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                etFecha.setText(formatoPantalla.format(
                        formatoBD.parse(fechaFirestore)
                ));
            } catch (Exception e) {
                etFecha.setText(fechaFirestore);
            }
        }

        double horas = getIntent().getDoubleExtra("horas", 0);
        etHoras.setText(String.valueOf(horas));

        String actividad = getIntent().getStringExtra("actividad");
        if (actividad != null) {
            int posicion = ((ArrayAdapter<String>) spActividad.getAdapter())
                    .getPosition(actividad);
            spActividad.setSelection(posicion);
        }
    }

    private void mostrarCalendario() {
        Calendar calendario = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar seleccion = Calendar.getInstance();
                    seleccion.set(year, month, dayOfMonth);

                    SimpleDateFormat formatoPantalla =
                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    SimpleDateFormat formatoBD =
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    etFecha.setText(formatoPantalla.format(seleccion.getTime()));
                    fechaFirestore = formatoBD.format(seleccion.getTime());
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void guardarSesion() {
        String curso = etCurso.getText().toString().trim();
        String horasTexto = etHoras.getText().toString().trim();
        String actividad = spActividad.getSelectedItem().toString();

        if (curso.isEmpty()) {
            etCurso.setError("Ingresa el nombre del curso");
            etCurso.requestFocus();
            return;
        }

        if (fechaFirestore.isEmpty()) {
            etFecha.setError("Selecciona una fecha");
            return;
        }

        if (horasTexto.isEmpty()) {
            etHoras.setError("Ingresa las horas dedicadas");
            etHoras.requestFocus();
            return;
        }

        double horas;

        try {
            horas = Double.parseDouble(horasTexto);
        } catch (NumberFormatException e) {
            etHoras.setError("Ingresa un número válido");
            return;
        }

        if (horas <= 0) {
            etHoras.setError("Las horas deben ser mayores que cero");
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión primero",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> sesion = new HashMap<>();
        sesion.put("curso", curso);
        sesion.put("fecha", fechaFirestore);
        sesion.put("horas", horas);
        sesion.put("actividad", actividad);

        btnGuardarSesion.setEnabled(false);

        if (sesionId == null) {
            // Crear una nueva sesión
            db.collection("usuarios")
                    .document(uid)
                    .collection("sesiones")
                    .add(sesion)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Sesión guardada correctamente",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> mostrarError(e.getMessage()));

        } else {
            // Actualizar la sesión existente
            db.collection("usuarios")
                    .document(uid)
                    .collection("sesiones")
                    .document(sesionId)
                    .update(sesion)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Sesión actualizada correctamente",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> mostrarError(e.getMessage()));
        }
    }

    private void mostrarError(String mensaje) {
        btnGuardarSesion.setEnabled(true);

        Toast.makeText(this, "Error: " + mensaje,
                Toast.LENGTH_LONG).show();
    }
}