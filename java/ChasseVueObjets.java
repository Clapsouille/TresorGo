package fr.cnam.nfa022.bonelli;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import static fr.cnam.nfa022.bonelli.ChasseActivity.NB_OBJETS;

public class ChasseVueObjets extends View {

    /** Classe gérant l'affichage des objets au-dessus du flux de caméra **/

    /*
            ########################
            ##### DECLARATIONS #####
            ########################
    */

    String tag = "CHASSE_VUE_OBJETS";
    static final int DIST_MAX = 8;
    static final float ALPHA = 0.9f;

    // Référence vers l'activité principale
    ChasseActivity master;

    // Cible (X)
    Paint p;
    float[][] pos; // Pour chaque objet : 0=x, 1=y et 2=distance (en mètres)
    boolean[] show; // L'objet est dans l'angle de vue

    // Informations de dessin
    DrawRes drw = new DrawRes();

    // Images
    Bitmap[] images;
    ImageView[] imgV;
    int wCible = 150;
    int hCible = 150;
    boolean attendre;

    // UI
    TextView[] etatObj;
    boolean[] trouve;
    boolean[] visible;
    TextView tempsTxt;

    // Suivi de la recherche
    int aTrouver = NB_OBJETS;

    /*
            ########################
            ##### CONSTRUCTEUR #####
            ########################
    */

    public ChasseVueObjets(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Instanciations
        images = new Bitmap[NB_OBJETS];
        imgV = new ImageView[NB_OBJETS];
        pos = new float[NB_OBJETS][];
        show = new boolean[NB_OBJETS];
        visible = new boolean[NB_OBJETS];
        trouve = new boolean[NB_OBJETS];
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        attendre = false;

        // Récupération des images
        for(int i=0; i<NB_OBJETS; i++) {
            pos[i] = new float[3];
            pos[i][0] = -1;
            trouve[i] = false;
            visible[i] = false;
            images[i] = BitmapFactory.decodeResource(context.getResources(), drw.getDrw(i));
            imgV[i] = new ImageView(context);
            imgV[i].setImageBitmap(Bitmap.createScaledBitmap(images[i], wCible, hCible, false));
        }
    }

    /*
            #####################################
            ##### CYCLE DE VIE ET AFFICHAGE #####
            #####################################
    */

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        for(int i=0; i<NB_OBJETS; i++) {
            if(show[i] && !attendre) {
                // Afficher une croix dont la taille est proportionnelle à la proximité de l'objet
                float taille = 1000/pos[i][2];
                if(taille>1000) {
                    taille = 1000;
                }
                p.setTextSize(taille);
                // Si l'objet est assez près pour être découvert
                if(pos[i][2] < DIST_MAX) {
                    p.setColor(Color.RED);
                }
                // Sinon
                else {
                    p.setColor(drw.getCouleur(i));
                }
                c.drawText("X", pos[i][0], pos[i][1], p);
            }
            else if(visible[i]) {
                if(pos[i][0] > 0) {
                    ViewGroup.LayoutParams params = imgV[i].getLayoutParams();
                    imgV[i].setLayoutParams(params);
                    imgV[i].setX(pos[i][0] - imgV[i].getWidth() / 2f);
                    imgV[i].setY(pos[i][1] - imgV[i].getHeight() / 2f);
                    imgV[i].setVisibility(VISIBLE);
                }
                else {
                    imgV[i].setVisibility(GONE);
                }
            }
        }
    }

    // Appelé quand l'objet apparaît dans l'angle de vue : il est à afficher aux coordonnées transmises
    public void afficher(Objet obj, float x, float y) {
        int num = obj.getNum();
        if(!trouve[num]) {
            show[num] = true;
        }
        // Filtre de réduction d'à-coups pour la position de la cible
        if(pos[num][0] < 0) {
            pos[num][0] = x;
            pos[num][1] = y;
        }
        else {
            pos[num][0] = ALPHA * pos[num][0] + (1 - ALPHA) * x;
            pos[num][1] = ALPHA * pos[num][1] + (1 - ALPHA) * y;
        }
        pos[num][2] = obj.getDist();
        this.invalidate();
    }

    // Appelée quand l'objet est en vue et que le joueur a fait l'action de le révéler (type = 0 pour tap simple, 1 pour double tap)
    public void reveler(int type) {
        for(int i=0; i<NB_OBJETS; i++) {
            // Si un objet n'est pas déjà affiché
            if(!attendre) {
                // Si l'objet est en vue et non encore trouvé et que (tap simple et distance assez petite) ou (tap double)
                if (show[i] && !trouve[i] && ((type == 0 && pos[i][2] < DIST_MAX) || (type == 1))) {
                    // Masquer la croix et afficher l'image
                    show[i] = false;
                    attendre = true;
                    this.invalidate();
                    imgV[i].setVisibility(VISIBLE);
                    if (imgV[i].getParent() == null) {
                        master.getLayout().addView(imgV[i]);
                    }
                    trouve[i] = true;
                    visible[i] = true;
                    // L'objet est trouvé : on décrémente le nombre de ceux restants
                    aTrouver -= 1;
                    master.incrementerTimer();

                    // Laisser l'image affichée 3 secondes avant de la faire disparaître
                    Handler handler = new Handler();
                    final int finalI = i;
                    final ChasseVueObjets s = this;
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            imgV[finalI].setVisibility(GONE);
                            visible[finalI] = false;
                            etatObj[finalI].setText("OK");
                            master.supprimer(finalI);
                            s.invalidate();
                            attendre = false;
                            // Si c'était le dernier objet à trouver, on lance l'activité de fin
                            if (aTrouver == 0) {
                                master.fin(true);
                            }
                        }
                    }, 3000);
                }
            }
        }
    }

    // L'objet sort de l'angle de vue, donc à ne plus afficher
    public void masquer(Objet obj) {
        int num = obj.getNum();
        if(!trouve[num]) {
            show[num] = false;
            pos[num][0] = -1;
            this.invalidate();
        }
    }

    public void pause() {
        for(int i=0; i<NB_OBJETS; i++) {
            if(!trouve[i]) {
                show[i] = false;
                pos[i][0] = -1;
            }
        }
        this.invalidate();
    }

    /*
            #############################
            ##### SETTERS & GETTERS #####
            #############################
    */

    // Connection à l'activité principale et in stanciation de l'UI
    void setMaster(ChasseActivity m) {
        master = m;
        TextView tv = new TextView(master.getApplicationContext());
        tv.setTextColor(Color.WHITE);
        tv.setText(R.string.a_trouver);
        tv.setX(50);
        tv.setY(25);
        tv.setTextSize(15);
        tv.setTextColor(Color.WHITE);
        master.getUI().addView(tv);
        if(etatObj == null) {
            etatObj = new TextView[NB_OBJETS];
            for (int i = 0; i < NB_OBJETS; i++) {
                // Création de l'UI
                etatObj[i] = new TextView(master.getApplicationContext());
                etatObj[i].setX(100*(i+1) + 100);
                etatObj[i].setY(25);
                etatObj[i].setTextSize(20);
                etatObj[i].setTextColor(drw.getCouleur(i));
                etatObj[i].setText("?");
                master.getUI().addView(etatObj[i]);
            }
            tempsTxt = new TextView(master.getApplicationContext());
            tempsTxt.setX(100*(NB_OBJETS+1) + 200);
            tempsTxt.setY(25);
            tempsTxt.setTextSize(15);
            tempsTxt.setTextColor(Color.WHITE);
            tempsTxt.setText(master.getTempsRestant());
            master.getUI().addView(tempsTxt);
        }
    }

    @SuppressLint("SetTextI18n")
    void majTimer(long tps) {
        if(tps>5) {
            tempsTxt.setTextColor(Color.WHITE);
        }
        else {
            tempsTxt.setTextColor(Color.RED);
        }
        tempsTxt.setText(master.getTempsRestant());
    }
}