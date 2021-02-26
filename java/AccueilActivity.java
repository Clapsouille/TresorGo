package fr.cnam.nfa022.bonelli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class AccueilActivity extends AppCompatActivity {

    /** Activité d'accueil, avant lancement de l'exploration **/

    String tag = "ACCUEIL_ACTIVITY";

    static final int PERMISSION_CAMERA = 1;
    static final int PERMISSION_LOCALISATION = 2;

    Location lastLoc;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accueil);

        // Vérification de l'accès à la localisation
        if(verifPerm(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_LOCALISATION)) {
            ecouterLoc();
        }
    }

    // Vérification des permissions
    boolean verifPerm(String perm, int code) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), perm)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, code);
            return false;
        }
        else {
            return true;
        }
    }

    // Demande de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String message = "";
        String[] perm = new String[0];
        int code = 0;
        switch (requestCode) {
            case PERMISSION_CAMERA:
                perm = new String[]{Manifest.permission.CAMERA};
                message = "Accès à la caméra nécessaire pour commencer à explorer";
                code = PERMISSION_CAMERA;
                break;
            case PERMISSION_LOCALISATION:
                perm = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                code = PERMISSION_LOCALISATION;
                message = "Accès à la localisation nécessaire pour commencer à explorer";
                break;
        }

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            final String[] fPerm = perm;
            final int fCode = code;
            final AccueilActivity ac = this;
            new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton("Allons-y !", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ac, fPerm, fCode);
                        }
                    })
                    .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishAffinity();
                        }
                    })
                    .create()
                    .show();
        }
        else {
            if (requestCode == PERMISSION_LOCALISATION) {
                ecouterLoc();
            }
            else if(requestCode == PERMISSION_CAMERA) {
                lancerChoix(null);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Récupération de la localisation
    void ecouterLoc() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        OnSuccessListener<Location> listener = new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null) {
                    lastLoc = location;
                }
            }
        };
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(listener);
        }
        catch (SecurityException e) { e.printStackTrace(); }
    }

    public void montrerExplications(View v) {
        TextView tv = findViewById(R.id.accueil_explications);
        tv.setAllCaps(false);
        tv.setTextSize(15);
        tv.setText(R.string.explications);
    }

    // Lancement de l'exploration et transmission de la localisation à ChasseActivity
    public void lancerChoix(View v) {
        if(verifPerm(Manifest.permission.CAMERA, PERMISSION_CAMERA)) {
            Intent jouer = new Intent(this, ChoixActivity.class);
            jouer.putExtra("LAST_LOC", lastLoc);
            startActivity(jouer);
        }
    }
}
