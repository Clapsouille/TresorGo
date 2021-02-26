package fr.cnam.nfa022.bonelli;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class ChasseGestionCapteurs implements LocationListener, SensorEventListener {

    /** Classe gérant les différents capteurs et donc la position et l'orientation de l'appareil **/

    /*
            ########################
            ##### DECLARATIONS #####
            ########################
    */

    String tag = "CHASSE_GESTION_CAPTEURS";

    // Référence vers l'activité principale
    private ChasseActivity master;

    // Capteurs
    private LocationManager lm;
    private Location loc;
    private SensorManager sm;
    private Sensor accel;
    private Sensor compas;
    private String provider;

    // Données des capteurs
    private float[] valAccel;
    private float[] valCompas;
    private float[] temp = new float[9];
    private float[] rotation = new float[9];
    private float[] inclinaison = new float[9];
    private float[] orientation = new float[3];

    // Données du low-pass filter
    private double[] derSin = new double[3]; // 0 = azimuth, 1 = pitch et 2 = roll
    private double[] derCos = new double[3];
    private static float ALPHA = 0.99f;

    // Angle de vue
    private double fovV;
    private double fovH;

    /*
            ########################
            ##### CONSTRUCTEUR #####
            ########################
    */

    ChasseGestionCapteurs(ChasseActivity m, Context context) {
        master = m;

        // Récupération du provider de localisation
        lm = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        crit.setPowerRequirement(Criteria.NO_REQUIREMENT);
        provider = lm.getBestProvider(crit, true);

        // Récupération des capteurs
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compas = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    void lancerListeners() {
        // Localisation
        try {
            lm.requestLocationUpdates(provider, 0, 0, this);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }

        // Orientation
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, compas, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
            ########################
            ##### CYCLE DE VIE #####
            ########################
    */

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Récupération des valeurs des capteurs
        switch(event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                valAccel = filtreCapteurs(event.values, valAccel);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                valCompas = filtreCapteurs(event.values, valCompas);
                break;
        }
        // Calcul des données de positionnement
        if(valAccel != null && valCompas != null) {
            calculerOrientation();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onLocationChanged(Location location) {
        if(loc==null) {
            loc = location;
            master.creerObjets(loc);
        }

        else {
            // Algorithme de smoothing pour la localisation
            double lat = ALPHA * loc.getLatitude() + (1 - ALPHA) * location.getLatitude();
            double lon = ALPHA * loc.getLongitude() + (1 - ALPHA) * location.getLongitude();
            double alt = ALPHA * loc.getAltitude() + (1 - ALPHA) * location.getAltitude();
            loc.setLatitude(lat);
            loc.setLongitude(lon);
            loc.setAltitude(alt);
        }
        master.calculerDistances(loc);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    void pause() {
        for(int i=0; i<3; i++) {
            derSin[i] = 0;
            derCos[i] = 0;
        }
        valAccel = null;
        valCompas = null;
        lm.removeUpdates(this);
        sm.unregisterListener(this, accel);
        sm.unregisterListener(this, compas);
    }

    /*
            ############################
            ##### LOW-PASS FILTERS #####
            ############################
    */

    // data = nouvelles données, val = données précédemment stockées
    private float[] filtreCapteurs(float[] data, float[] val) {
        // (Code adapté de https://developer.android.com/guide/topics/sensors/sensors_motion)
        if (val == null) {
            return data;
        } else {
            for (int i = 0; i < data.length; i++) {
                val[i] = ALPHA * val[i] + (1 - ALPHA) * data[i];
            }
            return val;
        }
    }

    private double filtreAngle(float nouvAng, int i) {
        if(derSin[i]==0 && derCos[i]==0) {
            return Math.toDegrees(nouvAng);
        }
        else {
            derSin[i] = ALPHA * derSin[i] + (1 - ALPHA) * Math.sin(nouvAng);
            derCos[i] = ALPHA * derCos[i] + (1 - ALPHA) * Math.cos(nouvAng);
            return Math.toDegrees(Math.atan2(derSin[i], derCos[i]));
        }
    }

    /*
            ##############################################
            ##### CALCULS ET RECUPERATION DE DONNEES #####
            ##############################################
    */

    // Calcul de l'orientation du téléphone
    private void calculerOrientation() {
        SensorManager.getRotationMatrix(temp, inclinaison, valAccel, valCompas);
        SensorManager.getInclination(inclinaison);
        SensorManager.remapCoordinateSystem(temp, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotation);
        SensorManager.getOrientation(rotation, orientation);

        // Récupération des données d'orientation et application des filtres
        // (0 = azimuth, 1 = pitch et 2 = roll)
        double azimuth = filtreAngle(orientation[0], 0);
        double pitch = filtreAngle(orientation[1], 1);
        double roll = filtreAngle(orientation[2], 2);

        // Adaptation des angles en fonction du roll
        double variationRoll = (fovH-fovV)*Math.abs(roll)/90;
        double angleV = fovV + variationRoll;
        double angleH = fovH - variationRoll;

        if(loc!=null) {
            master.calculerEcarts(azimuth, pitch, roll, angleV, angleH);
        }
    }

    Location recupererLastLoc(Bundle data) {
        loc = new Location("");
        try {
            loc.setLatitude(Objects.requireNonNull(data.getDoubleArray("LAST_LOC"))[0]);
            loc.setLongitude(Objects.requireNonNull(data.getDoubleArray("LAST_LOC"))[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loc;
    }

    /*
            #############################
            ##### SETTERS & GETTERS #####
            #############################
    */

    void setFOV(float v, float h) {
        fovV = v;
        fovH = h;
    }

    void setLoc(Location l) { loc = l; }

    Location getLoc() { return loc; }
}
