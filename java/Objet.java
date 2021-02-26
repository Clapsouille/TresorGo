package fr.cnam.nfa022.bonelli;

import android.location.Location;
import android.util.Log;
import java.util.Random;

class Objet {

    /** Classe stockant l'ensemble des informations relatives aux objets créés dans ChasseActivity **/

    /*
            ########################
            ##### DECLARATIONS #####
            ########################
    */

    private String tag = "OBJET";

    private static float ALPHA = 0.6f;

    private static int compteur = 0;
    private int num;

    private double lat;
    private double lon;
    private double alt;
    private float distance;
    private double difAlt;
    private double angleH;
    private double angleV;

    /*
            ########################
            ##### CONSTRUCTEUR #####
            ########################
    */

    // Le booléen nouveau indique si :
    // true -> les objets doivent être instanciés à partir de la localisation du téléphone
    // false -> les objets doivent être recréés à partir de leur propre localisation
    Objet(Location l, boolean nouveau) {
        num = compteur;
        compteur++;

        // Données de localisation
        if(nouveau) {
            lat = l.getLatitude() + randDif(0);
            lon = l.getLongitude() + randDif(0);
            alt = l.getAltitude() + randDif(1);
        }

        else {
            lat = l.getLatitude();
            lon = l.getLongitude();
            alt = l.getAltitude();
        }
    }

    /*
            ######################################
            ##### CALCULS LIES A LA POSITION #####
            ######################################
    */

    // Générer un nombre aléatoire pour varier la distance de l'objet à l'appareil
    private double randDif(int i) {
        Random r = new Random();
        double min;
        double max;
        // i=0 pour latitude et longitude
        if(i == 0) {
            min = 0.00003;
            max = 0.00007;
        }
        // i=1 pour altitude
        else {
            min = 0.5;
            max = 5;
        }
        double dif = min + (max - min) * r.nextDouble();
        if(r.nextBoolean()) {
            return dif;
        }
        else {
            return -dif;
        }
    }

    // Calcul de la distance (en mètres) entre l'objet et l'appareil
    void calculerDistance(Location mLoc) {
        float[] dist = new float[3];
        Location.distanceBetween(mLoc.getLatitude(), mLoc.getLongitude(), lat, lon, dist);
        if(distance == 0) {
            distance = dist[0];
        }
        else {
            distance = (ALPHA * distance) + ((1 - ALPHA) * dist[0]);
        }

        // Calcul de la différence d'altitude
        difAlt = mLoc.getAltitude() - alt;
    }

    // Calcul des angles (en degrés) entre l'appareil et l'objet
    void calculerAngles(Location mLoc) {
        // Angle horizontal (par rapport au Nord)
        // (Code adapté de https://www.developpez.net/forums/d1039202/java/developpement-mobile-java/android/api-standards-tierces/calcul-l-angle-entre-2-points-gps/)
        double latM = Math.toRadians(mLoc.getLatitude());
        double latOR = Math.toRadians(lat);
        double delta = Math.toRadians(lon - mLoc.getLongitude());
        double x = Math.sin(delta) * Math.cos(latOR);
        double y = Math.cos(latM) * Math.sin(latOR) - Math.sin(latM) * Math.cos(latOR) * Math.cos(delta);
        double ang = Math.toDegrees(Math.atan2(x, y));
        ang = (ang + 360) % 360;
        if(ang>180) {
            ang = 180 - ang;
        }
        if(angleH==0) {
            angleH = ang;
        }
        else {
            angleH = (ALPHA/2 * angleH) + ((1 - ALPHA/2) * ang);
        }

        // Angle vertical (différence d'altitude)
        if(angleV==0) {
            angleV = Math.atan(difAlt/distance);
        }
        else {
            angleV = ALPHA/2 * angleV + (1 - ALPHA/2) * Math.atan(difAlt / distance);
        }
    }

    /*
            #############################
            ##### SETTERS & GETTERS #####
            #############################
    */

    int getNum() { return num; }

    double getLat() { return lat; }

    double getLon() { return lon; }

    double getAlt() { return alt; }

    double getAngleH() { return angleH; }

    double getAngleV() { return angleV; }

    float getDist() { return distance; }

    static void resetCompteur() { compteur = 0; }
}