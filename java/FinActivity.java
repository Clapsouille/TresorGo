package fr.cnam.nfa022.bonelli;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

public class FinActivity extends AppCompatActivity {

    /** Activité s'affichant une fois tous les objets trouvés **/

    String tag = "FIN_ACTIVITY";
    Location lastLoc;
    boolean gagne;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = this.getIntent();
        lastLoc = (Location) Objects.requireNonNull(in.getExtras()).get("LAST_LOC");
        gagne = in.getBooleanExtra("TEMPS_EPUISE", true);
        setContentView(R.layout.activity_fin);
        TextView tv = findViewById(R.id.fin_txt);
        if(gagne) {
            tv.setText(R.string.chasse_fin_gagne);
        }
        else {
            tv.setText(R.string.chasse_fin_perdu);
        }
    }

    // Transmission de la localisation enregistrée et relance de l'activité de chasse
    public void rejouer(View v) {
        Intent out = new Intent(this, ChoixActivity.class);
        out.putExtra("LAST_LOC", lastLoc);
        startActivity(out);
    }

    public void quitter(View v) {
        finishAffinity();
    }
}