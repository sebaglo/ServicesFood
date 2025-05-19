package com.example.servicios;

public class Alumno {
    private String rut;
    private String nombres;
    private String apPaterno;
    private String apMaterno;

    public Alumno() {
        // Constructor vacío necesario para JSON y otras operaciones
    }

    public Alumno(String rut, String nombres, String apPaterno, String apMaterno) {
        this.rut = rut;
        this.nombres = nombres;
        this.apPaterno = apPaterno;
        this.apMaterno = apMaterno;
    }

    // Getters y setters
    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApPaterno() {
        return apPaterno;
    }

    public void setApPaterno(String apPaterno) {
        this.apPaterno = apPaterno;
    }

    public String getApMaterno() {
        return apMaterno;
    }

    public void setApMaterno(String apMaterno) {
        this.apMaterno = apMaterno;
    }
}
