package com.example.myapplication;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.SesionEstudio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GraficosActivity extends AppCompatActivity {

    private Spinner spFiltroCurso;
    private BarChart graficoBarras;
    private PieChart graficoCircular;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private List<SesionEstudio> sesiones;
    private ListenerRegistration listenerSesiones;

    private String cursoSeleccionado = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficos);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spFiltroCurso = findViewById(R.id.spFiltroCurso);
        graficoBarras = findViewById(R.id.graficoBarras);
        graficoCircular = findViewById(R.id.graficoCircular);

        sesiones = new ArrayList<>();

        configurarGraficos();
        configurarFiltro();
    }

    private void configurarGraficos() {
        Description descripcionBarras = new Description();
        descripcionBarras.setText("Horas de estudio");
        graficoBarras.setDescription(descripcionBarras);
        graficoBarras.getAxisRight().setEnabled(false);
        graficoBarras.getAxisLeft().setAxisMinimum(0f);
        graficoBarras.getXAxis().setGranularity(1f);
        graficoBarras.getXAxis().setPosition(
                com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        );

        Description descripcionCircular = new Description();
        descripcionCircular.setText("Distribución de horas");
        graficoCircular.setDescription(descripcionCircular);
        graficoCircular.setUsePercentValues(true);
        graficoCircular.setEntryLabelTextSize(12f);
    }

    private void configurarFiltro() {
        spFiltroCurso.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               android.view.View view,
                                               int position, long id) {
                        cursoSeleccionado =
                                parent.getItemAtPosition(position).toString();
                        actualizarGraficos();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
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
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "Error al cargar datos: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (value != null) {
                        sesiones.clear();

                        for (DocumentSnapshot documento : value.getDocuments()) {
                            SesionEstudio sesion =
                                    documento.toObject(SesionEstudio.class);

                            if (sesion != null) {
                                sesiones.add(sesion);
                            }
                        }

                        cargarCursosEnFiltro();
                        actualizarGraficos();
                    }
                });
    }

    private void cargarCursosEnFiltro() {
        Set<String> cursos = new HashSet<>();

        for (SesionEstudio sesion : sesiones) {
            if (sesion.getCurso() != null &&
                    !sesion.getCurso().trim().isEmpty()) {
                cursos.add(sesion.getCurso());
            }
        }

        List<String> opciones = new ArrayList<>();
        opciones.add("Todos");

        List<String> cursosOrdenados = new ArrayList<>(cursos);
        Collections.sort(cursosOrdenados);

        opciones.addAll(cursosOrdenados);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                opciones
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spFiltroCurso.setAdapter(adapter);

        int posicion = opciones.indexOf(cursoSeleccionado);

        if (posicion >= 0) {
            spFiltroCurso.setSelection(posicion);
        } else {
            cursoSeleccionado = "Todos";
            spFiltroCurso.setSelection(0);
        }
    }

    private List<SesionEstudio> obtenerSesionesFiltradas() {
        List<SesionEstudio> filtradas = new ArrayList<>();

        for (SesionEstudio sesion : sesiones) {
            if (cursoSeleccionado.equals("Todos") ||
                    cursoSeleccionado.equals(sesion.getCurso())) {
                filtradas.add(sesion);
            }
        }

        return filtradas;
    }

    private void actualizarGraficos() {
        List<SesionEstudio> filtradas = obtenerSesionesFiltradas();

        actualizarGraficoBarras(filtradas);
        actualizarGraficoCircular(filtradas);
    }

    private void actualizarGraficoBarras(List<SesionEstudio> datos) {

        Map<String, Float> horasPorMes = new TreeMap<>();

        for (SesionEstudio sesion : datos) {
            String fecha = sesion.getFecha();

            if (fecha != null && fecha.length() >= 7) {
                String mes = fecha.substring(0, 7);

                float horas = (float) sesion.getHoras();

                float acumulado = horasPorMes.containsKey(mes)
                        ? horasPorMes.get(mes) : 0f;

                horasPorMes.put(mes, acumulado + horas);
            }
        }

        List<BarEntry> entradas = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();

        int posicion = 0;

        for (Map.Entry<String, Float> entrada : horasPorMes.entrySet()) {
            etiquetas.add(entrada.getKey());
            entradas.add(new BarEntry(posicion, entrada.getValue()));
            posicion++;
        }

        if (entradas.isEmpty()) {
            graficoBarras.clear();
            graficoBarras.invalidate();
            return;
        }

        BarDataSet conjunto = new BarDataSet(
                entradas, "Horas por mes"
        );

        BarData datosBarras = new BarData(conjunto);
        datosBarras.setBarWidth(0.6f);

        graficoBarras.setData(datosBarras);
        graficoBarras.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(etiquetas)
        );
        graficoBarras.getXAxis().setLabelCount(etiquetas.size());
        graficoBarras.invalidate();
    }

    private void actualizarGraficoCircular(List<SesionEstudio> datos) {

        Map<String, Float> horasPorActividad = new HashMap<>();

        for (SesionEstudio sesion : datos) {
            String actividad = sesion.getActividad();

            if (actividad != null) {
                float horas = (float) sesion.getHoras();

                float acumulado = horasPorActividad.containsKey(actividad)
                        ? horasPorActividad.get(actividad) : 0f;

                horasPorActividad.put(actividad, acumulado + horas);
            }
        }

        List<PieEntry> entradas = new ArrayList<>();

        for (Map.Entry<String, Float> entrada : horasPorActividad.entrySet()) {
            entradas.add(new PieEntry(
                    entrada.getValue(),
                    entrada.getKey()
            ));
        }

        if (entradas.isEmpty()) {
            graficoCircular.clear();
            graficoCircular.invalidate();
            return;
        }

        PieDataSet conjunto = new PieDataSet(
                entradas, "Horas por actividad"
        );

        PieData datosCircular = new PieData(conjunto);
        datosCircular.setValueTextSize(12f);

        graficoCircular.setData(datosCircular);
        graficoCircular.invalidate();
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
