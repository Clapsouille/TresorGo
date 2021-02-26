package fr.cnam.nfa022.bonelli;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Objects;

public class ChoixActivity extends AppCompatActivity {

    static final String tag = "CHOIX_ACTIVITY";
    static final int NB_OBJETS_MAX = 5;

    NumberPicker npNb;
    NumberPicker npTps;

    FusedLocationProviderClient provider;
    Location lastLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choix);

        // Mise en place des NumberPickers
        npNb = findViewById(R.id.choix_nb_np);
        npNb.setMinValue(1);
        npNb.setMaxValue(NB_OBJETS_MAX);
        npNb.setValue(3);
        npNb.setWrapSelectorWheel(false);
        npTps = findViewById(R.id.choix_tps_np);
        npTps.setMinValue(1);
        npTps.setMaxValue(6);
        npTps.setValue(3);
        npTps.setWrapSelectorWheel(false);
        npTps.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.valueOf(i*10);
            }
        });

        // Récupération de la localisation
        Intent in = this.getIntent();
        lastLoc = (Location) Objects.requireNonNull(in.getExtras()).get("LAST_LOC");
        if(lastLoc==null) {
            ecouterLoc();
        }
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

    // Lancement de l'exploration et transmission de la localisation à ChasseActivity
    public void lancerJeu(View v) {
            Intent lancer = new Intent(this, ChasseActivity.class);
            lancer.putExtra("NB_OBJETS", npNb.getValue());
            lancer.putExtra("TPS", npTps.getValue()*10);
            lancer.putExtra("LAST_LOC", lastLoc);
            startActivity(lancer);
    }
}