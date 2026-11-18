package com.example.myapplication.model;
public class SesionEstudio {

    // Atributos de cada sesión
    private String id;
    private String curso;
    private String fecha;
    private double horas;
    private String actividad;

    // Constructor vacío (necesario para Firestore)
    public SesionEstudio() {
    }

    // Constructor completo
    public SesionEstudio(String id, String curso, String fecha,
                         double horas, String actividad) {
        this.id = id;
        this.curso = curso;
        this.fecha = fecha;
        this.horas = horas;
        this.actividad = actividad;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getHoras() {
        return horas;
    }

    public void setHoras(double horas) {
        this.horas = horas;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }
}
