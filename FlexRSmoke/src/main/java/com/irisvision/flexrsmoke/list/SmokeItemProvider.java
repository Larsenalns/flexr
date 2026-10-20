package com.irisvision.flexrsmoke.list;

import com.irisvision.flexrcoremodule.observable.FlexProvider;

/**
 * Provider d'état d'<b>un</b> item de la liste de fumée.
 *
 * <p>Un provider par item, c'est ce qui rend la mise à jour <b>granulaire</b> (étage D) : notifier
 * l'item 3 ne touche que la ligne 3, sans notification de liste.
 */
public class SmokeItemProvider extends FlexProvider {

    private final String key;

    public SmokeItemProvider(String key) {
        this.key = key;
    }

    @Override
    protected String providerFlag() {
        return "smoke-item:" + this.key;
    }
}
