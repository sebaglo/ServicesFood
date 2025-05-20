package com.example.servicios;

public class ModeloAsistencia {
    public String rut;
    public String idServicio;
    public String fecha;
    public String horaEntrada;
    public String horaSalida;
    public String observacion;

    public ModeloAsistencia(String rut, String idServicio, String fecha, String horaEntrada, String horaSalida, String observacion) {
        this.rut = rut;
        this.idServicio = idServicio;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.observacion = observacion;
    }

    @Override
    public String toString() {
        return rut + " - " + fecha + " - " + horaEntrada;
    }
}
