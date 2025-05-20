package com.example.servicios;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Asistencia extends AppCompatActivity {

    private Spinner spinnerColacion;
    private Button btnGuardar;
    private ListView lista;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> datosAsistencia;

    private String rut;
    private String nombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asistencia_activity);

        spinnerColacion = findViewById(R.id.spinnerColacion);
        ArrayAdapter<String> colacionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Desayuno", "Almuerzo"});
        colacionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColacion.setAdapter(colacionAdapter);

        btnGuardar = findViewById(R.id.btnGuardarAsistencia);
        lista = findViewById(R.id.listaAsistencia);

        rut = getIntent().getStringExtra("rut");
        nombre = getIntent().getStringExtra("Nombre");

        datosAsistencia = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datosAsistencia);
        lista.setAdapter(adapter);

        cargarAsistencias();

        btnGuardar.setOnClickListener(v -> {
            // Aquí obtenemos el tipo de colación elegido
            String colacionSeleccionada = spinnerColacion.getSelectedItem().toString();
            obtenerIdServicioYRegistrarAsistencia(rut, colacionSeleccionada);
        });
    }


    private void cargarAsistencias() {
        String url = "http://172.100.8.99/insertar_asistencia.php";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONArray data = response.getJSONArray("data");

                            datosAsistencia.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.getJSONObject(i);
                                String rut = obj.getString("RUT_ALUMNO");
                                String idServicio = obj.getString("ID_SERVICIO");
                                String fecha = obj.getString("FECHA_ASISTENCIA");
                                String horaEntrada = obj.getString("HORA_ENTRADA");
                                String horaSalida = obj.optString("HORA_SALIDA", "");
                                String observacion = obj.optString("OBSERVACION", "");

                                String fila = rut + " | " + fecha + " | " + horaEntrada + " - " + horaSalida;
                                datosAsistencia.add(fila);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            String message = response.getString("message");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error procesando datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                });

        queue.add(jsonObjectRequest);
    }

    private void obtenerIdServicioYRegistrarAsistencia(String rut, String colacion) {
        String url = "http://172.100.8.99/obtener_id_servicio.php";

        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RESPUESTA_ID_SERVICIO", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        String idServicio = json.getString("id_servicio");
                        guardarAsistencia(rut, idServicio); // Llamada correcta
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar id_servicio", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error al obtener id_servicio", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("colacion", colacion);
                params.put("fecha", fechaHoy); // ✅ IMPORTANTE: que este parámetro exista
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }

    private boolean modoSimulado = true; // Cambia a false para usar servidor real

    private void guardarAsistencia(String rut, String idServicio) {
        if (modoSimulado) {
            // Modo simulación sin conexión a base de datos
            String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String horaEntrada = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            String asistenciaSimulada = rut + " | " + fecha + " | " + horaEntrada + " - ";

            datosAsistencia.add(asistenciaSimulada);
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Asistencia registrada (simulada)", Toast.LENGTH_SHORT).show();
        } else {
            // Modo real: enviar datos al servidor
            String url = "http://172.100.8.99/insertar_asistencia.php";

            String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String horaEntrada = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            JSONObject params = new JSONObject();
            try {
                params.put("RUT_ALUMNO", rut);
                params.put("ID_SERVICIO", idServicio);
                params.put("FECHA_ASISTENCIA", fecha);
                params.put("HORA_ENTRADA", horaEntrada);
                params.put("HORA_SALIDA", "");
                params.put("OBSERVACION", "");
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error preparando datos", Toast.LENGTH_SHORT).show();
                return;
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                            if (success) {
                                cargarAsistencias();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error en respuesta del servidor", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Error al registrar asistencia", Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(this).add(request);
        }
    }
}