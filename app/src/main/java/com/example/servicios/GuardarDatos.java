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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuardarDatos extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Alumno alumnoEncontrado;
    private TextView txtNombreAlumno;
    private Spinner spinnerColacion;
    private EditText editTextRut;
    private Button btnIngresar, btnRegistrarSalida, btnEscanear, btnGuardar,btnRegistrarAlumnos, btnListado;
    private ListView listViewAlumnos;

    private String fechaSeleccionada = "";
    private String rutEscaneado = "";

    // Inicializo la lista de alumnos aquí para evitar NullPointerException
    private ArrayList<Alumno> listaAlumnos = new ArrayList<>();
    private ArrayList<String> listaRegistros;
    private ArrayAdapter<String> adapter;

    private Toast toastGlobal;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guardar_datos);
        //Validamos instancias
        btnListado = findViewById(R.id.btnListado);
        btnRegistrarAlumnos = findViewById(R.id.btnRegistrarAlumno);
        txtNombreAlumno = findViewById(R.id.txtNombreAlumno);
        spinnerColacion = findViewById(R.id.spinnerColacion);
        editTextRut = findViewById(R.id.txtResultado);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnGuardar = findViewById(R.id.btnRegresar);
        btnRegistrarSalida = findViewById(R.id.btnRegistrarSalida);
        btnEscanear = findViewById(R.id.btnEscanear);
        listViewAlumnos = findViewById(R.id.listViewAlumnos);

        //Boton de Listado de Alumnos
        btnListado.setOnClickListener(v-> {
            Intent intent = new Intent(GuardarDatos.this, Asistencia.class);
            intent.putExtra("Rut", "12345678-9");
            intent.putExtra("Nombre", "Juan Perez");
            startActivity(intent);
        });



        //boton de registrar alumnos en la base de datos
        btnRegistrarAlumnos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alumnoEncontrado != null) {
                    String rut = alumnoEncontrado.getRut();
                    String nombre = alumnoEncontrado.getNombres();
                    String apPaterno = alumnoEncontrado.getApPaterno();
                    String apMaterno = alumnoEncontrado.getApMaterno();

                    Intent intent = new Intent(GuardarDatos.this, Asistencia.class);
                    intent.putExtra("rut", rutEscaneado);
                    intent.putExtra("Nombre", String.valueOf(txtNombreAlumno));
                    startActivity(intent);

                    String fecha = fechaSeleccionada;
                    String horaIngreso = obtenerHoraActual();
                    String colacion = spinnerColacion.getSelectedItem().toString(); // "Desayuno" o "Almuerzo"

                    // Registrar ingreso en PHP y local


                    // Mostrar en ListView
                    String nombreCompleto = nombre + " " + apPaterno + " " + apMaterno;
                    String registro = rut + " - " + nombreCompleto +
                            "\nFecha: " + fecha + " | Hora ingreso: " + horaIngreso +
                            " | Colación: " + colacion;

                    listaRegistros.add(registro);
                    adapter.notifyDataSetChanged();

                    // Limpiar campo y variables
                    mostrarToast("Alumno registrado correctamente");
                    editTextRut.setText("");
                    rutEscaneado = "";
                } else {
                    mostrarToast("No se encontró el alumno");
                }
            }
        });

// Limpiar campos
        editTextRut.setText("");
        rutEscaneado = "";
        // Ya inicialicé listaAlumnos arriba, solo cargo los datos
        cargarAlumnosDesdeServidor();

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

        fechaSeleccionada = formatearFecha(new Date());

        listViewAlumnos.setOnItemLongClickListener((parent, view, position, id) -> {
            String registroSeleccionado = listaRegistros.get(position);

            new AlertDialog.Builder(GuardarDatos.this)
                    .setTitle("Acción para el registro")
                    .setMessage("¿Qué deseas hacer con este registro?\n" + registroSeleccionado)
                    .setPositiveButton("Registrar salida", (dialog, which) -> {
                        // Extraer RUT del registro (asumiendo que el rut está antes del primer " - ")
                        String rut = registroSeleccionado.split(" - ")[0];

                        // Verificar si ya tiene salida registrada
                        if (registroSeleccionado.contains("Hora salida")) {
                            mostrarToast("Salida ya registrada para este alumno");
                        } else {
                            // Agregar hora salida
                            String horaSalida = obtenerHoraActual();
                            listaRegistros.set(position, registroSeleccionado + "\nHora salida: " + horaSalida);
                            adapter.notifyDataSetChanged();

                            // Registrar salida en servidor
                            registrarSalida(rut, fechaSeleccionada, horaSalida);

                            mostrarToast("Salida registrada");
                        }
                    })
                    .setNegativeButton("Eliminar", (dialog, which) -> {
                        listaRegistros.remove(position);
                        adapter.notifyDataSetChanged();
                        mostrarToast("Registro eliminado");
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();

            return true;
        });


        btnIngresar.setOnClickListener(v -> {
            String rut = rutEscaneado.isEmpty() ? editTextRut.getText().toString().trim() : rutEscaneado;
            if (!rut.isEmpty()) {
                buscarAlumnoLocalmentePorRut(rut);
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
                registrarSalidaManual(rut);
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
            if (result.getContents() != null) {
                rutEscaneado = result.getContents();
                editTextRut.setText(rutEscaneado);
                mostrarToast("RUT: " + rutEscaneado);
            } else {
                mostrarToast("Lector cancelado");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String formatearFecha(Date fecha) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha);
    }

    private String obtenerHoraActual() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }




    private void registrarSalidaManual(String rut) {
        String horaSalida = obtenerHoraActual();
        boolean encontrado = false;

        for (int i = 0; i < listaRegistros.size(); i++) {
            String registro = listaRegistros.get(i);
            // Buscar registro sin hora de salida y con el RUT
            if (registro.contains(rut) && !registro.contains("Hora salida")) {
                String nuevoRegistro = registro + "\nHora salida: " + horaSalida;
                listaRegistros.set(i, nuevoRegistro);
                encontrado = true;

                // Registrar salida en servidor
                registrarSalida(rut, fechaSeleccionada, horaSalida);
                Log.d("Salida", "Registro actualizado: " + nuevoRegistro);
                break;
            }
        }

        if (encontrado) {
            adapter.notifyDataSetChanged();
            mostrarToast("Salida registrada");
        } else {
            mostrarToast("Registro no encontrado o ya tiene salida");
        }

        // Limpiar inputs
        editTextRut.setText("");
        rutEscaneado = "";
    }

// También mejora registrarSalida para saber si hubo éxito o error

    private void registrarSalida(String rut, String fecha, String horaSalida) {
        String url = "http://172.100.8.99/registrar_salida.php";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("Salida", "Respuesta del servidor: " + response);
                    mostrarToast("Salida registrada en servidor");
                },
                error -> {
                    Log.e("Salida", "Error en el servidor: " + error.toString());
                    mostrarToast("Error al registrar salida en servidor");
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

        Volley.newRequestQueue(this).add(postRequest);
    }

    private void mostrarToast(String mensaje) {
        if (toastGlobal != null) toastGlobal.cancel();
        toastGlobal = Toast.makeText(this, mensaje, Toast.LENGTH_SHORT);
        toastGlobal.show();
    }

    private void cargarAlumnosDesdeServidor() {
        String url = "http://172.100.8.99/obtener_todos_alumnos.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        listaAlumnos.clear(); // Limpio la lista para no duplicar

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject alumnoJson = jsonArray.getJSONObject(i);
                            Alumno alumno = new Alumno(
                                    alumnoJson.getString("RUT_ALUMNO"),
                                    alumnoJson.getString("NOMBRES_ALUMNO"),
                                    alumnoJson.getString("AP_PATERNO_ALUMNO"),
                                    alumnoJson.getString("AP_MATERNO_ALUMNO")
                            );
                            listaAlumnos.add(alumno);
                        }
                        mostrarToast("Alumnos cargados: " + listaAlumnos.size());
                    } catch (JSONException e) {
                        mostrarToast("Error al leer datos");
                        e.printStackTrace();
                    }
                },
                error -> {
                    mostrarToast("Error al conectar con el servidor");
                    error.printStackTrace();
                });

        Volley.newRequestQueue(this).add(request);
    }


    private void buscarAlumnoLocalmentePorRut(String rut) {
        if (listaAlumnos == null || listaAlumnos.isEmpty()) {
            mostrarToast("Lista de alumnos no cargada o vacía");
            return;
        }

        for (Alumno alumno : listaAlumnos) {
            if (alumno.getRut().equalsIgnoreCase(rut)) {
                alumnoEncontrado = alumno; // ← Asignamos el alumno a la variable global

                String nombreCompleto = alumno.getNombres() + " " + alumno.getApPaterno() + " " + alumno.getApMaterno();
                txtNombreAlumno.setText(nombreCompleto);

                String colacion = spinnerColacion.getSelectedItem().toString();
                String horaIngreso = obtenerHoraActual();


                // Guardar registro para mostrar en ListView
                String registro = alumno.getRut() + " - " + nombreCompleto +
                        "\nFecha: " + fechaSeleccionada + " | Hora ingreso: " + horaIngreso + " | " + colacion;
                listaRegistros.add(registro);
                adapter.notifyDataSetChanged();

                // Limpiar campo de RUT y variable de escaneo
                editTextRut.setText("");
                rutEscaneado = "";

                mostrarToast("Alumno registrado");

                return;
            }
        }

        mostrarToast("Alumno no encontrado en la lista local");
    }
    private void registrarAlumnoEnAsistencia(
            String rut,
            String nombreCompleto,
            String fecha,
            String horaIngreso,
            String colacion) {

        String url = "http://172.100.8.99/insertar_asistencia.php"; // Cambia esta URL por la correcta

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");
                        String message = jsonResponse.getString("message");

                        mostrarToast(message);

                        if (status.equals("success")) {
                            // Opcional: actualizar UI, limpiar campos, etc.
                            editTextRut.setText("");
                            txtNombreAlumno.setText("");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mostrarToast("Error procesando la respuesta del servidor");
                    }
                },
                error -> {
                    error.printStackTrace();
                    mostrarToast("Error de conexión con el servidor");
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

        Volley.newRequestQueue(this).add(postRequest);
    }


}
