package com.example.servicios;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.android.volley.toolbox.JsonObjectRequest;
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
    private Button btnIngresar, btnEscanear, btnGuardar, btnRegistrarAlumnos, btnRegistrarAsistencia;
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

        //Validamos instancia para boton registrar en caso de que no este
        btnRegistrarAsistencia = findViewById(R.id.btnRegistrarAsistencia);
        btnRegistrarAsistencia.setVisibility(View.GONE);

        //Instanciamos de mas variables
        btnRegistrarAlumnos = findViewById(R.id.btnRegistrarAlumno);
        txtNombreAlumno = findViewById(R.id.txtNombreAlumno);
        spinnerColacion = findViewById(R.id.spinnerColacion);
        editTextRut = findViewById(R.id.txtResultado);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnGuardar = findViewById(R.id.btnRegresar);
        btnEscanear = findViewById(R.id.btnEscanear);
        listViewAlumnos = findViewById(R.id.listViewAlumnos);

        btnRegistrarAlumnos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alumnoEncontrado != null) {
                    String rut = alumnoEncontrado.getRut();
                    String nombre = alumnoEncontrado.getNombres();
                    String apPaterno = alumnoEncontrado.getApPaterno();
                    String apMaterno = alumnoEncontrado.getApMaterno();

                    String fecha = fechaSeleccionada;
                    String horaIngreso = obtenerHoraActual();
                    String colacion = spinnerColacion.getSelectedItem().toString();

                    String nombreCompleto = nombre + " " + apPaterno + " " + apMaterno;

                    // 🟢 REGISTRAMOS EN LA BASE DE DATOS
                    registrarAlumnoEnAsistencia(rut, nombreCompleto, fecha, horaIngreso, colacion);

                    // 🟢 MOSTRAMOS EN EL LISTVIEW
                    String registro = rut + " - " + nombreCompleto +
                            "\nFecha: " + fecha + " | Hora ingreso: " + horaIngreso +
                            " | Colación: " + colacion;

                    listaRegistros.add(registro);
                    adapter.notifyDataSetChanged();

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
        String url = "https://172.100.8.99/registrar_salida.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equals("success")) {
                        mostrarToast("Salida registrada correctamente");
                        // Aquí actualiza UI si es necesario
                    } else {
                        mostrarToast("Error al registrar salida: " + response);
                    }
                },
                error -> mostrarToast("Error en la petición: " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rut", rut);
                params.put("fecha", fecha);
                params.put("hora_salida", horaSalida);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
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
                        listaAlumnos.clear();

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

        // Verificar si ya tiene registro sin salida
        for (int i = 0; i < listaRegistros.size(); i++) {
            String registro = listaRegistros.get(i);

            if (registro.startsWith(rut)) {
                if (!registro.contains("Hora salida:")) {
                    String horaSalida = obtenerHoraActual();
                    registro += " | Hora salida: " + horaSalida;
                    listaRegistros.set(i, registro);
                    adapter.notifyDataSetChanged();
                    mostrarToast("Salida registrada automáticamente a las " + horaSalida);

                    editTextRut.setText("");
                    rutEscaneado = "";
                    return;
                } else {
                    break; // Ya tiene salida
                }
            }
        }

        // Buscar en la lista local de alumnos
        for (Alumno alumno : listaAlumnos) {
            if (alumno.getRut().equalsIgnoreCase(rut)) {
                alumnoEncontrado = alumno;

                String nombreCompleto = alumno.getNombres() + " " + alumno.getApPaterno() + " " + alumno.getApMaterno();
                txtNombreAlumno.setText(nombreCompleto);

                String colacion = spinnerColacion.getSelectedItem().toString();
                String horaIngreso = obtenerHoraActual();

                String registro = alumno.getRut() + " - " + nombreCompleto +
                        "\nFecha: " + fechaSeleccionada + " | Hora ingreso: " + horaIngreso + " | " + colacion;
                listaRegistros.add(registro);
                adapter.notifyDataSetChanged();

                editTextRut.setText("");
                rutEscaneado = "";

                verificarAsistencia(rut); // Llamada online

                return;
            }
        }

        // 👉 Mostrar diálogo si no está en lista local
        mostrarDialogoRegistrarAlumnoNuevo(rut); // <-- Esto es lo nuevo
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

    private void verificarAsistencia(String rut) {
        String url = "https://172.100.8.99/verificar_asistencia.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    switch (response.trim()) {
                        case "no_registro":
                            // Registrar entrada
                            registrarAsistenciaEnBD(alumnoEncontrado);
                            break;
                        case "ingreso_sin_salida":
                            // Registrar salida
                            registrarSalidaEnBD(rut, fechaSeleccionada, obtenerHoraActual());
                            break;
                        case "completo":
                            mostrarToast("Ya registró ingreso y salida hoy.");
                            break;
                        default:
                            mostrarToast("Respuesta desconocida del servidor: " + response);
                            break;
                    }
                },
                error -> {
                    mostrarToast("Error al verificar asistencia: " + error.getMessage());
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rut", rut);
                params.put("fecha", fechaSeleccionada);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    private void registrarAsistenciaEnBD(Alumno alumno) {
        String url = "https://172.100.8.99/registrar_asistencia.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("ok")) {
                        mostrarToast("🎉 Asistencia registrada correctamente");
                        btnRegistrarAsistencia.setVisibility(View.GONE);
                    } else {
                        mostrarToast("⚠️ Error al registrar asistencia: " + response);
                    }
                },
                error -> mostrarToast("Error de red: " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rut", alumno.getRut());
                params.put("nombre", alumno.getNombres() + " " + alumno.getApPaterno() + " " + alumno.getApMaterno());
                params.put("fecha", fechaSeleccionada);
                params.put("hora_ingreso", obtenerHoraActual());
                params.put("colacion", spinnerColacion.getSelectedItem().toString());
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void registrarSalidaEnBD(String rut, String fecha, String horaSalida) {
        String url = "https://tu_dominio_o_ip/tu_carpeta/registrar_salida.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");

                        if (success) {
                            mostrarToast("Salida registrada correctamente");
                        } else {
                            mostrarToast("Error al registrar salida: " + message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mostrarToast("Error al interpretar la respuesta del servidor");
                    }
                },
                error -> {
                    mostrarToast("Error en la conexión: " + error.getMessage());
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("RUT_ALUMNO", rut);
                params.put("FECHA", fecha);
                params.put("HORA_SALIDA", horaSalida);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void mostrarDialogoRegistrarAlumnoNuevo(String rut) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrar nuevo alumno");

        // Inflar layout personalizado con campos para nombre y apellidos
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_registrar_alumno, (ViewGroup) findViewById(android.R.id.content), false);

        final EditText inputNombres = viewInflated.findViewById(R.id.inputNombres);
        final EditText inputApPaterno = viewInflated.findViewById(R.id.inputApPaterno);
        final EditText inputApMaterno = viewInflated.findViewById(R.id.inputApMaterno);

        // Mostrar el rut en un TextView o EditText (solo lectura)
        final TextView tvRut = viewInflated.findViewById(R.id.tvRut);
        tvRut.setText(rut);

        builder.setView(viewInflated);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombres = inputNombres.getText().toString().trim();
            String apPaterno = inputApPaterno.getText().toString().trim();
            String apMaterno = inputApMaterno.getText().toString().trim();

            if (nombres.isEmpty() || apPaterno.isEmpty() || apMaterno.isEmpty()) {
                mostrarToast("Por favor, complete todos los campos");
            } else {
                // Llamar función para guardar el alumno nuevo
                registrarAlumnoNuevo(rut, nombres, apPaterno, apMaterno);
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void registrarAlumnoNuevo(String rut, String nombres, String apPaterno, String apMaterno) {
        String url = "https://tu_ip_o_dominio/registrar_alumno.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.trim().equalsIgnoreCase("success")) {
                        mostrarToast("Alumno registrado correctamente");

                        // Agregar alumno nuevo a la lista local
                        Alumno nuevoAlumno = new Alumno(rut, nombres, apPaterno, apMaterno);
                        listaAlumnos.add(nuevoAlumno);

                        // Actualizar UI con nuevo alumno
                        txtNombreAlumno.setText(nombres + " " + apPaterno + " " + apMaterno);
                        alumnoEncontrado = nuevoAlumno;

                        // Luego verificar asistencia para el alumno ya creado
                        verificarAsistencia(rut);

                    } else {
                        mostrarToast("Error al registrar alumno: " + response);
                    }
                },
                error -> mostrarToast("Error al registrar alumno: " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rut", rut);
                params.put("nombres", nombres);
                params.put("ap_paterno", apPaterno);
                params.put("ap_materno", apMaterno);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


}
