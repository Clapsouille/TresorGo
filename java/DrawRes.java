package fr.cnam.nfa022.bonelli;

import android.graphics.Color;

class DrawRes {

    /** Classe permettant de stocker les informations de base relatives aux objets (image et couleur associée) **/

    private static int[] tabDrw = new int[5];
    private static int[] tabCouleurs = new int[5];

    DrawRes() {
        // Images disponibles sur http://www.pngall.com/
        tabDrw[0] = R.drawable.diamond;
        tabDrw[1] = R.drawable.chest;
        tabDrw[2] = R.drawable.gold;
        tabDrw[3] = R.drawable.crown;
        tabDrw[4] = R.drawable.coin;

        // Couleurs pour chaque objet
        tabCouleurs[0] = Color.parseColor("#b4d2eb");
        tabCouleurs[1] = Color.parseColor("#e8dda9");
        tabCouleurs[2] = Color.parseColor("#9be5ac");
        tabCouleurs[3] = Color.parseColor("#e8caf1");
        tabCouleurs[4] = Color.parseColor("#e2a483");
    }

    int getDrw(int i) { return tabDrw[i]; }

    int getCouleur(int i) { return tabCouleurs[i]; }
}
