package com.irisvision.flexrsmoke.list;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItem;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItemData;

/**
 * Une ligne de la liste de fumée.
 *
 * <p>Elle illustre le cœur de l'étage D : la ligne <b>s'abonne</b> au provider de son item (par
 * l'adaptateur) et se met à jour <b>elle-même</b> — sans {@code notifyDataSetChanged}, sans
 * {@code DiffUtil}, et sans toucher les autres lignes.
 */
public class SmokeRow extends FlexItem {

    private FlexText label;
    private SmokeRowData data;

    public SmokeRow(@NonNull View itemView) {
        super(itemView);
    }

    /**
     * Fabrique statique : elle fournit la {@code FrameLayout} qu'impose le constructeur de
     * {@code ViewHolder}, et c'est le seul endroit où cette contrainte apparaît.
     */
    public static FlexItem instance(FlexContextInterface f) {
        return new SmokeRow(new FrameLayout(f.getFlexContext()));
    }

    @Override
    protected View dynamicBuild() {
        this.label = new FlexText(f);
        this.label.text("");
        return nativeView(this.label);
    }

    @Override
    protected void attachEvents() {
        // Les clics sont posés par l'adaptateur (onItemClick), pas par la ligne.
    }

    @Override
    protected void initialBusinessLogic() {
        // Rien à charger : cet écran vérifie le cycle de vie des lignes, pas la donnée distante.
    }

    @Override
    public void bindData(FlexItemData itemData) {
        this.data = (SmokeRowData) itemData;
        this.label.text(this.data.label());
    }

    /**
     * Appelée <b>sur le thread principal</b> par la librairie quand un provider observé notifie : la
     * ligne réécrit son propre texte. C'est la mise à jour granulaire, côté ligne.
     */
    @Override
    protected void onProviderChanged(FlexProvider flexProvider) {
        if (this.data != null) {
            this.label.text(this.data.label() + "   [mise à jour par la ligne]");
        }
    }
}
