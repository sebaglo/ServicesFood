package com.example.servicios;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuardarDatos extends AppCompatActivity {

    private TextView txtNombreAlumno;
    private Spinner spinnerColacion;
    private EditText editTextRut;
    private Button btnIngresar, btnRegistrarSalida, btnEscanear, btnGuardar;
    private ListView listViewAlumnos;

    private String fechaSeleccionada = "";
    private String rutEscaneado = "";
    private ArrayList<String> listaRegistros;
    private ArrayAdapter<String> adapter;

    private Toast toastGlobal;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guardar_datos);

        txtNombreAlumno = findViewById(R.id.txtNombreAlumno);
        spinnerColacion = findViewById(R.id.spinnerColacion);
        editTextRut = findViewById(R.id.txtResultado);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnGuardar = findViewById(R.id.btnRegresar);
        btnRegistrarSalida = findViewById(R.id.btnRegistrarSalida);
        btnEscanear = findViewById(R.id.btnEscanear);
        listViewAlumnos = findViewById(R.id.listViewAlumnos);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.colacion,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColacion.setAdapter(spinnerAdapter);

        listaRegistros = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaRegistros);
        listViewAlumnos.setAdapter(adapter);

        Date fechaActual = new Date();
        fechaSeleccionada = formatearFecha(fechaActual);

        listViewAlumnos.setOnItemLongClickListener((parent, view, position, id) -> {
            String registroSeleccionado = listaRegistros.get(position);
            new AlertDialog.Builder(GuardarDatos.this)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Estás seguro de que quieres eliminar este registro?\n" + registroSeleccionado)
                    .setPositiveButton("Sí", (dialog, which) -> {
                        listaRegistros.remove(position);
                        adapter.notifyDataSetChanged();
                        mostrarToast("Registro eliminado");
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        btnIngresar.setOnClickListener(v -> {
            String rut = rutEscaneado.isEmpty() ? editTextRut.getText().toString().trim() : rutEscaneado;
            if (!rut.isEmpty()) {
                buscarAlumnoPorRut(rut);
            } else {
                mostrarToast("Por favor escanee o escriba un RUT");
            }
        });

        btnGuardar.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnRegistrarSalida.setOnClickListener(v -> {
            String rut = rutEscaneado.isEmpty() ? editTextRut.getText().toString().trim() : rutEscaneado;

            if (!rut.isEmpty()) {
                boolean encontrado = false;
                String horaSalida = obtenerHoraActual();

                for (int i = 0; i < listaRegistros.size(); i++) {
                    String registro = listaRegistros.get(i);
                    if (registro.contains(rut) && !registro.contains("Hora salida")) {
                        listaRegistros.set(i, registro + "\nHora salida: " + horaSalida);
                        encontrado = true;

                        registrarSalida(rut, fechaSeleccionada, horaSalida);

                        break;
                    }
                }

                if (encontrado) {
                    adapter.notifyDataSetChanged();
                    mostrarToast("Hora de salida registrada para el RUT: " + rut);
                } else {
                    mostrarToast("Alumno no encontrado o ya tiene hora de salida registrada");
                }

                editTextRut.setText("");
                rutEscaneado = "";
            } else {
                mostrarToast("Por favor escanee o escriba un RUT");
            }
        });

        btnEscanear.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(GuardarDatos.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Lector-CDP");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                mostrarToast("Lector Cancelado");
            } else {
                mostrarToast(result.getContents());
                rutEscaneado = result.getContents();
                editTextRut.setText(rutEscaneado);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String formatearFecha(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }

    private String obtenerHoraActual() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void buscarAlumnoPorRut(String rut) {
        String url = "http:/172.100.8.85/buscar_alumno.php?RUT_ALUMNO=" + rut;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("RESPUESTA_SERVIDOR", response); // Ver si llega vacío o malformado
                    Toast.makeText(GuardarDatos.this, "Respuesta: " + response, Toast.LENGTH_LONG).show(); // <-- línea añadida

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            JSONObject alumno = jsonObject.getJSONObject("alumno");
                            String nombre = alumno.getString("nombre");
                            String apPaterno = alumno.getString("ap_paterno");
                            String apMaterno = alumno.getString("ap_materno");

                            txtNombreAlumno.setText(nombre + " " + apPaterno + " " + apMaterno);

                            String horaIngreso = obtenerHoraActual();
                            String colacion = spinnerColacion.getSelectedItem().toString();

                            String registro = "RUT: " + rut + "\nFecha: " + fechaSeleccionada + "\nColación: " + colacion + "\nHora ingreso: " + horaIngreso;
                            listaRegistros.add(registro);
                            adapter.notifyDataSetChanged();

                            registrarIngreso(rut, nombre, apPaterno, apMaterno, fechaSeleccionada, horaIngreso, colacion);

                            editTextRut.setText("");
                            rutEscaneado = "";

                        } else {
                            mostrarToast("Alumno no encontrado en la base de datos");
                            txtNombreAlumno.setText("");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mostrarToast("Error al procesar la respuesta del servidor");
                    }
                },
                error -> {
                    mostrarToast("Error de conexión: " + error.getMessage());
                });

        queue.add(stringRequest);
    }


    private void registrarIngreso(String rut, String nombre, String apPaterno, String apMaterno,
                                  String fecha, String horaIngreso, String colacion) {

        String nombreCompleto = nombre + " " + apPaterno + " " + apMaterno;

        String url = "http:/172.100.8.85/registrar_ingreso.php";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    mostrarToast("Ingreso registrado correctamente");
                    Log.d("RegistrarIngreso", "Respuesta: " + response);
                },
                error -> {
                    mostrarToast("Error al registrar ingreso: " + error.getMessage());
                    Log.e("RegistrarIngreso", "Error: " + error.toString());
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("RUT_ALUMNO", rut);
                params.put("NOMBRE_COMPLETO_ALUMNO", nombreCompleto);
                params.put("FECHA", fecha);
                params.put("HORA_INGRESO", horaIngreso);
                params.put("COLACION", colacion);
                return params;
            }
        };

        queue.add(postRequest);
    }

    private void registrarSalida(String rut, String fecha, String horaSalida) {
        String url = "http:/172.100.8.85/registrar_salida.php";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    mostrarToast("Salida registrada correctamente");
                    Log.d("RegistrarSalida", "Respuesta: " + response);
                },
                error -> {
                    mostrarToast("Error al registrar salida: " + error.getMessage());
                    Log.e("RegistrarSalida", "Error: " + error.toString());
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("RUT_ALUMNO", rut);
                params.put("FECHA", fecha);
                params.put("HORA_SALIDA", horaSalida);
                return params;
            }
        };

        queue.add(postRequest);
    }

    private void mostrarToast(String mensaje) {
        if (toastGlobal != null) toastGlobal.cancel();
        toastGlobal = Toast.makeText(this, mensaje, Toast.LENGTH_SHORT);
        toastGlobal.show();
    }
}
