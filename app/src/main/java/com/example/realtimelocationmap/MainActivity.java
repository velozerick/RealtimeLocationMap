package com.example.realtimelocationmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView locationText;
    private Button btnOpenInMaps, btnShare, btnToggleUpdates, btnShowHistory;

    private double currentLat = 0;
    private double currentLon = 0;
    private final int LOCATION_PERMISSION_CODE = 1001;

    private boolean updatesRunning = false;
    private final ArrayList<String> locationHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationText = findViewById(R.id.locationText);
        btnOpenInMaps = findViewById(R.id.btnOpenInMaps);
        btnShare = findViewById(R.id.btnShare);
        btnToggleUpdates = findViewById(R.id.btnToggleUpdates);
        btnShowHistory = findViewById(R.id.btnShowHistory);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnToggleUpdates.setOnClickListener(v -> {
            if (updatesRunning) {
                stopLocationUpdates();
            } else {
                if (checkPermissions()) {
                    startLocationUpdates();
                } else {
                    requestPermissions();
                }
            }
        });

        btnOpenInMaps.setOnClickListener(v -> {
            if (currentLat != 0 && currentLon != 0) {
                String uri = "geo:" + currentLat + "," + currentLon + "?q=" + currentLat + "," + currentLon + "(Estoy aquí)";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Ubicación aún no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            if (currentLat != 0 && currentLon != 0) {
                String message = "Mi ubicación actual es:\nLatitud: " + currentLat + "\nLongitud: " + currentLon;
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(intent, "Compartir ubicación"));
            }
        });

        btnShowHistory.setOnClickListener(v -> {
            if (locationHistory.isEmpty()) {
                Toast.makeText(this, "Sin historial aún", Toast.LENGTH_SHORT).show();
            } else {
                StringBuilder fullHistory = new StringBuilder("Historial:\n");
                for (String loc : locationHistory) {
                    fullHistory.append(loc).append("\n");
                }
                locationText.setText(fullHistory.toString());
            }
        });

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        if (!checkPermissions()) return;

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLon = location.getLongitude();

                    String text = "Latitud: " + currentLat + "\nLongitud: " + currentLon;
                    locationText.setText(text);

                    locationHistory.add(text);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            updatesRunning = true;
            btnToggleUpdates.setText("Detener ubicación");
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permiso denegado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            updatesRunning = false;
            btnToggleUpdates.setText("Comenzar ubicación");
            Toast.makeText(this, "Ubicación detenida", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }
}
