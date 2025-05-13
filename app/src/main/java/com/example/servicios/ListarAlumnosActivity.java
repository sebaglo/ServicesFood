package com.example.servicios;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ListarAlumnosActivity extends AppCompatActivity {

    ListView listViewAlumnos;
    Button btnCargarAlumnos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_alumnos);

        listViewAlumnos = findViewById(R.id.listViewAlumnos);
        btnCargarAlumnos = findViewById(R.id.btnCargarAlumnos);

        // Permitir operaciones de red en el hilo principal (solo para pruebas)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Configurar el botón para cargar los alumnos
        btnCargarAlumnos.setOnClickListener(v -> obtenerAlumnos());
    }

    private void obtenerAlumnos() {
        String urlWebService = "https://172.100.8.85/alumno.php";

        try {
            // 🔐 TrustManager para confiar en todos los certificados (solo en pruebas)
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Evitar verificación de hostname (solo para IPs o certificados no válidos)
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            // Conexión HTTPS
            URL url = new URL(urlWebService);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Leer respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resultado = new StringBuilder();
            String linea;

            while ((linea = reader.readLine()) != null) {
                resultado.append(linea);
            }

            reader.close();

            // Parsear JSON
            JSONArray jsonArray = new JSONArray(resultado.toString());
            ArrayList<String> lista = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject alumno = jsonArray.getJSONObject(i);
                String rut = alumno.getString("RUT_ALUMNO");
                String nombre = alumno.getString("NOMBRE_COMPLETO_ALUMNO");
                lista.add("RUT: " + rut + "\nNombre: " + nombre);
            }

            // Mostrar en la lista
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lista);
            listViewAlumnos.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al obtener datos", Toast.LENGTH_LONG).show();
        }
    }
}
