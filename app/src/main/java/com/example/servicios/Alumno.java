package com.example.servicios;

public class Alumno {
    private String rut;
    private String nombreCompleto;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private int idCurso;
    private boolean tieneBeca;
    private boolean alumnoActivo;

    public Alumno(String rut, String nombreCompleto, String nombres, String apPaterno, String apMaterno, int idCurso, boolean tieneBeca, boolean alumnoActivo) {
        this.rut = rut;
        this.nombreCompleto = nombreCompleto;
        this.nombres = nombres;
        this.apellidoPaterno = apPaterno;
        this.apellidoMaterno = apMaterno;
        this.idCurso = idCurso;
        this.tieneBeca = tieneBeca;
        this.alumnoActivo = alumnoActivo;
    }

    public String getRut() { return rut; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getNombres() { return nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public int getIdCurso() { return idCurso; }
    public boolean getTieneBeca() { return tieneBeca; }
    public boolean getAlumnoActivo() { return alumnoActivo; }
}
