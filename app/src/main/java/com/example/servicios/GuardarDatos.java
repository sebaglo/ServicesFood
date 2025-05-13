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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuardarDatos extends AppCompatActivity {


    private Spinner spinnerColacion;
    private EditText editTextRut;
    private Button btnIngresar, btnRegistrarSalida, btnEscanear, btnGuardar, btnLista;
    private ListView listViewAlumnos;

    private String fechaSeleccionada = "";
    private String rutEscaneado = ""; // Variable global para guardar el RUT escaneado
    private ArrayList<String> listaRegistros;
    private ArrayAdapter<String> adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guardar_datos);

        spinnerColacion = findViewById(R.id.spinnerColacion);
        editTextRut = findViewById(R.id.txtResultado);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnLista = findViewById(R.id.btnLista);
        btnGuardar = findViewById(R.id.btnRegresar);
        btnRegistrarSalida = findViewById(R.id.btnRegistrarSalida);
        btnEscanear = findViewById(R.id.btnEscanear);
        listViewAlumnos = findViewById(R.id.listViewAlumnos);

        // Inicializar spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.colacion,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColacion.setAdapter(spinnerAdapter);

        //Boton listado
        btnLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuardarDatos.this, ListarAlumnosActivity.class);
                startActivity(intent);
            }
        });

        // Inicializar lista
        listaRegistros = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaRegistros);
        listViewAlumnos.setAdapter(adapter);

        // Obtener la fecha actual del teléfono
        Date fechaActual = new Date();
        fechaSeleccionada = formatearFecha(fechaActual);

        // Configuración del Long Click para eliminar un ítem
        listViewAlumnos.setOnItemLongClickListener((parent, view, position, id) -> {
            // Obtener el registro seleccionado
            String registroSeleccionado = listaRegistros.get(position);

            // Mostrar cuadro de diálogo de confirmación
            new AlertDialog.Builder(GuardarDatos.this)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Estás seguro de que quieres eliminar este registro?\n" + registroSeleccionado)
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Eliminar el ítem de la lista
                        listaRegistros.remove(position);

                        // Notificar al adapter que los datos han cambiado
                        adapter.notifyDataSetChanged();

                        // Mostrar un mensaje de confirmación
                        Toast.makeText(GuardarDatos.this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();

            // Retornar true para indicar que hemos manejado el click largo
            return true;
        });

        // Botón ingresar
        btnIngresar.setOnClickListener(v -> {
            String rut = rutEscaneado.isEmpty() ? editTextRut.getText().toString().trim() : rutEscaneado; // Usar el RUT escaneado si está disponible
            String colacion = spinnerColacion.getSelectedItem().toString();

            if (!rut.isEmpty()) {
                String horaIngreso = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String registro = "RUT: " + rut + "\nFecha: " + fechaSeleccionada + "\nColación: " + colacion + "\nHora ingreso: " + horaIngreso;
                listaRegistros.add(registro);
                adapter.notifyDataSetChanged();

                // Limpiar campo
                editTextRut.setText("");
            } else {
                Toast.makeText(this, "Por favor escanee o escriba un RUT", Toast.LENGTH_SHORT).show();
            }
        });

        btnGuardar.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Botón registrar salida
        btnRegistrarSalida.setOnClickListener(v -> {
            String rut = rutEscaneado.isEmpty() ? editTextRut.getText().toString().trim() : rutEscaneado; // Usar el RUT escaneado si está disponible

            if (!rut.isEmpty()) {
                boolean encontrado = false;
                String horaSalida = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                // Buscar el alumno en la lista
                for (int i = 0; i < listaRegistros.size(); i++) {
                    String registro = listaRegistros.get(i);
                    if (registro.contains(rut) && !registro.contains("Hora salida")) {
                        // Si se encuentra al alumno y no tiene hora de salida, lo actualizamos
                        listaRegistros.set(i, registro + "\nHora salida: " + horaSalida);
                        encontrado = true;
                        break;
                    }
                }

                if (encontrado) {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Hora de salida registrada para el RUT: " + rut, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Alumno no encontrado o ya tiene hora de salida registrada", Toast.LENGTH_SHORT).show();
                }

                // Limpiar campo
                editTextRut.setText("");
            } else {
                Toast.makeText(this, "Por favor escanee o escriba un RUT", Toast.LENGTH_SHORT).show();
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

    // Método que se llama cuando se obtiene el resultado del escaneo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Lector Cancelado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, result.getContents(), Toast.LENGTH_SHORT).show();
                rutEscaneado = result.getContents();  // Guardar el RUT escaneado
                editTextRut.setText(rutEscaneado);// Mostrarlo en el campo de texto
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private String formatearFecha(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }
}