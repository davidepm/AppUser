package com.example.clases;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.appuser.MapsActivity;
import com.example.appuser.NotificationActivity;
import com.example.appuser.Principal;
import com.example.appuser.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

public class Servicio extends Service{

    private FusedLocationProviderClient fusedLocationClient;

    DatabaseReference dbr;

    String nombre;

    LocationRequest locationRequest;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 5000;


    //aqui el codigo que se ejcuta cada cierto tiempo para enviar las actualizaciones a firebase
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if(locationResult == null){
                return;
            }
            for(Location location:locationResult.getLocations()){
                    //obtener fecha
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    Date date = new Date();
                    String fecha = dateFormat.format(date);

                    //obtener hora
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                    Date curDate = new Date(System.currentTimeMillis());
                    String hora = formatter.format(curDate);

                    Map<String,Object> latLang = new HashMap<>();
                    latLang.put("latitud",location.getLatitude());
                    latLang.put("longitud",location.getLongitude());
                    latLang.put("fecha",fecha);
                    latLang.put("hora",hora);
                    latLang.put("usuario",nombre);

                    dbr.child("usuarios").child(nombre).updateChildren(latLang);
            }
        }
    };

    public Servicio() {

    }

    //metodo devuelve true si gps esta encendido y false si esta apagado
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            //si esta encendido
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            //si esta apagado
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("mensaje","creado");
    }


    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //handler que se ejecuta cada 5 seg para saber si el gps se ha desactivado
        handler.postDelayed(runnable = new Runnable() {
            ///asfkjabsk
            public void run() {
                handler.postDelayed(runnable, delay);
                //si esta desactivado se muestra un msj, se detiene el servicio y se cierra el activity si esta en primer plano
                //commit
                if(!isLocationEnabled(Servicio.this)){
                    Toast.makeText(Servicio.this, "GPS Apagado, Debe encenderlo", Toast.LENGTH_LONG).show();
                    stopService(new Intent(Servicio.this, Servicio.class));
                    Principal.actividad.finish();
                }
            }
        }, delay);

        dbr = FirebaseDatabase.getInstance().getReference();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            nombre = extras.getString("nombre");
        }

        //se crea un CHANNEL_ID para API26  y superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel("notification","mi notificacion", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager m = getSystemService(NotificationManager.class);
            m.createNotificationChannel(nc);
        }

        //Notificacion
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"notification");
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setContentTitle("GPS");
        builder.setContentText("Obteniendo Ubicacion en tiempo real");
        builder.setAutoCancel(true);
        //builder.setContentIntent(pi);
        // NotificationManagerCompat mm = NotificationManagerCompat.from(getApplicationContext());
        //mm.notify(1,builder.build());


        // codigo para inciicializar FusedLocationProviderClient y luego pedir las actualziaciones de ubicacion
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(4000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,Looper.getMainLooper());


        //se inicia en primer plano la notificacion para que el codigo se siga ejecutando aun si esta en segundo plano o se destruye la app
        startForeground(100,builder.build());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //dejar de enviar actualizaciones, detener notificacion, remover el handler
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        handler.removeCallbacks(runnable);
    }









}