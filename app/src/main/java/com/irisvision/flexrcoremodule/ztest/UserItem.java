package com.irisvision.flexrcoremodule.ztest;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItem;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItemData;

/**
 * Exemple de ligne FlexR.
 *
 * <p>Illustre le patron complet : fabrique statique fournissant la {@code FrameLayout} attendue par
 * {@link FlexItem}, construction de l'arbre par {@link #dynamicBuild()} (via {@code nativeView(...)}),
 * puis liaison de la donnée par {@link #bindData(FlexItemData)}.
 *
 * <p>Le champ {@code label} conserve la vue créée afin que {@code bindData} puisse la mettre à jour —
 * c'est l'usage type d'une ligne.
 */
public class UserItem extends FlexItem {

    private UserData data;
    private FlexText label;

    public UserItem(@NonNull View itemView) {
        super(itemView);
    }

    //This method grant to dev more flexibility about instantiation and permit to escape the constraint of the default constructor
    //of
    public static FlexItem instance(FlexContextInterface f) {
        return new UserItem(new FrameLayout(f.getFlexContext()));
    }

    @Override
    protected View dynamicBuild() {
        this.label = new FlexText(f);
        this.label.text("");
        return nativeView(this.label);
    }

    @Override
    protected void attachEvents() {

    }

    @Override
    protected void initialBusinessLogic() {

    }

    @Override
    public void bindData(FlexItemData data) {
        this.data = (UserData) data;
        this.label.text(this.data.getName());
    }

    public void test(){

    }
}
