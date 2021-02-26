package fr.cnam.nfa022.bonelli;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.Objects;

public class ChasseActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    /** Activité principale prenant en charge la liste des objets et l'écoute des interactions **/

    /*
            ########################
            ##### DECLARATIONS #####
            ########################
    */

    static final String tag = "CHASSE_ACTIVITY";

    // Gestion des objets
    static int NB_OBJETS;
    Objet[] objets = new Objet[NB_OBJETS];

    // Gestion des capteurs
    ChasseGestionCapteurs monde;

    // Layout et affichage
    FrameLayout layout;
    LinearLayout UI;
    ChasseVueCamera fluxCam;
    ChasseVueObjets surface;
    float fovH;
    float fovV;

    // Timer
    long tempsRestant;
    CountDownTimer timer;

    // Détection des interactions
    GestureDetectorCompat detecteur;

    /*
            ########################
            ##### CONSTRUCTEUR #####
            ########################
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Récupération de la dernière localisation
        monde = new ChasseGestionCapteurs(this, getApplicationContext());
        Location lastLoc;
        if(savedInstanceState!=null) {
                // Enregistrée dans l'activité
            lastLoc = monde.recupererLastLoc(savedInstanceState);
            NB_OBJETS = savedInstanceState.getInt("NB_OBJETS", 3);
            tempsRestant = (long) savedInstanceState.getInt("TPS", 30)*1000;
        }
        else {
            // Transmise par l'activité d'accueil
            Intent i = this.getIntent();
            lastLoc = (Location) Objects.requireNonNull(i.getExtras()).get("LAST_LOC");
            monde.setLoc(lastLoc);
            NB_OBJETS = i.getIntExtra("NB_OBJETS", 3);
            tempsRestant = (long) i.getIntExtra("TPS", 30)*1000;
        }

        // Mise en place du layout et de l'affichage
        setContentView(R.layout.activity_chasse);
        layout = findViewById(R.id.chasse_layout);
        UI = findViewById(R.id.chasse_ui);
        fluxCam = findViewById(R.id.chasse_preview);
        fluxCam.setMaster(this);
        surface = findViewById(R.id.chasse_vue_objets);
        surface.setMaster(this);

        // Instanciation du détecteur d'interactions
        detecteur = new GestureDetectorCompat(this, this);
        detecteur.setOnDoubleTapListener(this);

        // Création des objets
        creerObjets(lastLoc);
        calculerDistances(lastLoc);
    }

    /*
            ########################
            ##### CYCLE DE VIE #####
            ########################
     */

    @Override
    protected void onResume() {
        monde.lancerListeners();
        lancerTimer(tempsRestant);
        super.onResume();
    }

    @Override
    public void onPause() {
        monde.pause();
        surface.pause();
        timer.cancel();
        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            double[] tab = savedInstanceState.getDoubleArray(Integer.toString(0));
            assert tab != null;
            NB_OBJETS = savedInstanceState.getInt("NB_OBJETS", 3);
            tempsRestant = (long) savedInstanceState.getInt("TPS", 30)*1000;
            monde.recupererLastLoc(savedInstanceState);
            creerObjets(savedInstanceState);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("NB_OBJETS", NB_OBJETS);
        outState.putInt("TPS", (int)tempsRestant/1000);

        if (objets != null) {
            // Sauvegarde de la localisation des objets
            double[] tabLoc = new double[3];
            for (int i = 0; i < NB_OBJETS; i++) {
                if(objets[i]!=null) {
                    tabLoc[0] = objets[i].getLat();
                    tabLoc[1] = objets[i].getLon();
                    tabLoc[2] = objets[i].getAlt();
                    outState.putDoubleArray(Integer.toString(i), tabLoc);
                }
            }
            if (monde.getLoc() != null) {
                // Sauvegarde de la localisation du téléphone
                tabLoc[0] = monde.getLoc().getLatitude();
                tabLoc[1] = monde.getLoc().getLongitude();
                outState.putDoubleArray("LAST_LOC", tabLoc);
            }
        }
        super.onSaveInstanceState(outState);
    }

    // Lancement du timer
    void lancerTimer(long tps) {
        timer = new CountDownTimer(tps, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tempsRestant = millisUntilFinished;
                surface.majTimer(millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                fin(false);
            }
        };
        timer.start();
    }

    /*
            ##############################################
            ##### CREATION ET MISE A JOUR DES OBJETS #####
            ##############################################
     */


    // Première création d'objets à partir de la localisation du téléphone
    public void creerObjets(Location loc) {
        objets = new Objet[NB_OBJETS];
        for (int i = 0; i < NB_OBJETS; i++) {
            objets[i] = new Objet(loc, true);
        }
    }

    // Récupération d'objets précédemment créés et dont la localisation est stockée dans le Bundle
    private void creerObjets(Bundle data) {
        Objet.resetCompteur();
        objets = new Objet[NB_OBJETS];
        Location loc = new Location("");
        double[] tab;
        for (int i = 0; i < NB_OBJETS; i++) {
            tab = data.getDoubleArray(Integer.toString(i));
            assert tab != null;
            loc.setLatitude(tab[0]);
            loc.setLongitude(tab[1]);
            loc.setAltitude(tab[2]);
            objets[i] = new Objet(loc, false);
        }
    }

    // Calcul de la distance de l'objet par rapport à la localisation du téléphone
    public void calculerDistances(Location loc) {
        if (objets != null) {
            for (int i = 0; i < NB_OBJETS; i++) {
                if (objets[i] != null) {
                    objets[i].calculerDistance(loc);
                }
            }
        }
    }

    // Pour chaque objet, calcul de l'écart vertical et horizontal par rapport à la position et l'orientation du téléphone
    public void calculerEcarts(double azimuth, double pitch, double roll, double angleV, double angleH) {
        float posY;
        float posX;
        double ecartV;
        double ecartH;
        for (int i = 0; i < NB_OBJETS; i++) {
            if (objets[i] != null) {
                objets[i].calculerAngles(monde.getLoc());

                // Calcul de la position verticale et horizontale de la cible sur l'écran
                ecartV = objets[i].getAngleV() - pitch;
                ecartH = objets[i].getAngleH() - azimuth;
                if (Math.abs(ecartV) < angleV / 2 && Math.abs(ecartH) < angleH / 2) {
                    // Adaptation en fonction de l'inclinaison de l'écran
                    double varRoll = Math.abs(roll) / 90;
                    double relY = varRoll * ecartV - (1 - varRoll) * ecartH;
                    double relX = varRoll * ecartH + (1 - varRoll) * ecartV;
                    posY = (float) (surface.getHeight() / 2 + (relY * surface.getHeight() / fovV));
                    posX = (float) (surface.getWidth() / 2 + (relX * surface.getWidth() / fovH));
                    surface.afficher(objets[i], posX, posY);
                }
                else {
                    surface.masquer(objets[i]);
                }
            }
        }
    }

    public void supprimer(int i) {
        objets[i] = null;
    }

    /*
            #############################
            ##### SETTERS & GETTERS #####
            #############################
    */

    // Récupération des données du champ de vision
    public void setParamsCam(Camera.Parameters p) {
        fovV = p.getVerticalViewAngle();
        fovH = p.getHorizontalViewAngle();
        monde.setFOV(fovV, fovH);
    }

    public FrameLayout getLayout() { return layout; }

    public LinearLayout getUI() { return UI; }

    public String getTempsRestant() { return "Temps restant : "+tempsRestant/1000+" s"; }

    public void incrementerTimer() {
        timer.cancel();
        tempsRestant+=5000;
        lancerTimer(tempsRestant);
    }

    /*
            #####################
            ##### LISTENERS #####
            #####################
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detecteur.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        surface.reveler(0);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        surface.reveler(1);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) { }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) { }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

        /*
            ######################
            ##### FIN DU JEU #####
            ######################
    */

    public void fin(boolean gagne) {
        Objet.resetCompteur();
        Location loc = monde.getLoc();
        Intent fin = new Intent(this, FinActivity.class);
        fin.putExtra("LAST_LOC", loc);
        fin.putExtra("TEMPS_EPUISE", gagne);
        startActivity(fin);
    }
}