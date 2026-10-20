package com.irisvision.flexrsmoke.list;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterObservable;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItem;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItemData;

/**
 * Adaptateur de la liste de fumée : chaque ligne s'abonne au provider de <b>son</b> item.
 *
 * <p>Rien d'autre à écrire : la fabrique de ligne et le provider par item, et la librairie fait le
 * reste (libération avant chaque liaison, abonnement, remise sur le thread principal).
 */
public class SmokeListAdapter extends FlexAdapterObservable {

    public SmokeListAdapter(FlexContextInterface f) {
        super(f);
    }

    @Override
    protected FlexItem item(FlexContextInterface f) {
        return SmokeRow.instance(f);
    }

    @Override
    protected FlexProvider providerOf(FlexItemData item) {
        return (item instanceof SmokeRowData) ? ((SmokeRowData) item).provider() : null;
    }
}
