package com.example.appuser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Bundle;
import android.widget.Toast;


import com.example.clases.Servicio;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Principal extends AppCompatActivity{

    DatabaseReference dbr;
    public LocationManager locationManager;
    LocationRequest lr;
    AlertDialog alert = null;
    public String nombre = "";


    public static Activity actividad;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nombre = extras.getString("clave");
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        dbr = FirebaseDatabase.getInstance().getReference();

        actividad = this;
        verificarPermiso();

    }


    //verifica si se accedieron los permisos de ubicacion y luegosi el gps esta encendido
    public void verificarPermiso(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "No se han aceptado los permisos!", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("GPS Apagado, Debe encenderlo")
                        .setCancelable(false)
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                finish();
                            }
                        });
                alert = builder.create();
                alert.show();
                //AlertNoGPS();
            }else{
                Intent service = new Intent(getApplicationContext(),Servicio.class);
                service.putExtra("nombre",nombre);
                startForegroundService(service);
            }

        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopService(new Intent(this, Servicio.class));
    }

    //mensaje cunado el GPS esta desactivado
    public void AlertNoGPS(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Toast.makeText(Principal.this, "El GPS esta desactivado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
        alert = builder.create();
        alert.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PackageManager.PERMISSION_GRANTED){
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    boolean showRationale = shouldShowRequestPermissionRationale( permission );
                    if (! showRationale) {
                        Toast.makeText(this, "denied!", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "accpet!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }


    }

}