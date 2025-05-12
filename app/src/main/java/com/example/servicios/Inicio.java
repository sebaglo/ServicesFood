package com.example.servicios;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Inicio extends AppCompatActivity {

    private Button btnInicio;

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inicio_activity);

        btnInicio = findViewById(R.id.btnIniciar);


        btnInicio.setOnClickListener(v ->{
            Intent intent = new Intent(Inicio.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
