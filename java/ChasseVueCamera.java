package fr.cnam.nfa022.bonelli;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;

public class ChasseVueCamera extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    /** Classe gérant l'accès à la caméra et l'affichage du flux obtenu **/

    /*
            ########################
            ##### DECLARATIONS #####
            ########################
    */

    String tag = "CAM_PREVIEW";
    static final int CAMERA_CHASSE = 0;

    // Référence vers l'activité principale
    ChasseActivity master;

    Camera cam;
    Camera.Parameters paramsCam;

    SurfaceHolder holder;

    /*
        #########################
        ##### CONSTRUCTEURS #####
        #########################
    */

    public ChasseVueCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void init() {
        // Mise en place de la vue
        holder = this.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Récupération de la caméra
        ouvrirCam();
    }

    /*
            #######################################
            ##### CYCLE DE VIE ET PERMISSIONS #####
            #######################################
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(cam==null) {
            ouvrirCam();
        }
        else {
            try {
                cam.setPreviewDisplay(holder);
                cam.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setMeasuredDimension(width, height);
        requestLayout();
        cam.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        libererCam();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) { }

    protected void ouvrirCam() {
        libererCam();
        cam = Camera.open(CAMERA_CHASSE);
        setCam(cam);
    }

    /*
        #############################
        ##### SETTERS & GETTERS #####
        #############################
    */

    // Connection à l'activité principale et transmission des paramètres de la caméra
    protected void setMaster(ChasseActivity m) {
        master = m;
        if(cam != null) {
            paramsCam = cam.getParameters();
            master.setParamsCam(paramsCam);
        }
    }

    // Définition de la caméra
    public void setCam(Camera c) {
        if (cam != c) {
            libererCam();
            cam = c;
        }

        if (cam != null) {
            paramsCam = cam.getParameters();
            paramsCam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            cam.setParameters(paramsCam);
            requestLayout();

            try {
                cam.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cam.startPreview();
        }
    }

    protected void libererCam() {
        if (cam != null) {
            cam.stopPreview();
            cam.release();
            cam = null;
        }
    }
}
