package com.irisvision.flexrsmoke.list;

import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItemData;

/**
 * Donnée d'une ligne de la liste de fumée : clé stable, libellé affichable, et <b>son</b> provider.
 */
public class SmokeRowData extends FlexItemData {

    private final int id;
    private final SmokeItemProvider provider;
    private String label;

    public SmokeRowData(int id, String label) {
        this.id = id;
        this.label = label;
        this.provider = new SmokeItemProvider("" + id);
    }

    /**
     * Clé métier <b>stable</b> : elle ne change pas quand le libellé change. C'est elle qui permet à
     * {@code submitData(...)} de reconnaître un item déplacé, et aux identifiants stables de RecyclerView
     * d'animer les déplacements.
     */
    @Override
    public long stableId() {
        return this.id;
    }

    /** Le modèle doit être affichable : les champs à suggestion ({@code FlexInput}) montrent son texte. */
    @Override
    public String toString() {
        return this.label;
    }

    public int id() {
        return this.id;
    }

    public String label() {
        return this.label;
    }

    public void label(String value) {
        this.label = value;
    }

    public SmokeItemProvider provider() {
        return this.provider;
    }
}
