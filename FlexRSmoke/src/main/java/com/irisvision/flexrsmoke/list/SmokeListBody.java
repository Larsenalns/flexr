package com.irisvision.flexrsmoke.list;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.layout.FlexButton;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterRcv;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItemData;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexList;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrsmoke.SmokeChecklist;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran de fumée — famille « listes » (lot 12 + étages A→D).
 *
 * <p>Il exerce la chaîne complète : données, différences, clics, sélection, états vide et de
 * chargement, chargement à la demande, et surtout <b>la mise à jour granulaire d'une seule ligne</b>
 * par son provider (étage D) — sans aucune notification de liste.
 *
 * <p>Chaque scénario est déclenché par un bouton, pour que le verdict soit lisible d'un coup d'œil ;
 * le rapport de la checklist s'affiche en tête.
 */
public class SmokeListBody extends FlexBody {

    private final SmokeChecklist check = new SmokeChecklist("listes");
    private final List<FlexItemData> items = new ArrayList<>();

    private FlexText checklistReport;
    private FlexText status;
    private FlexList list;
    private SmokeListAdapter adapter;

    @Override
    protected FlexView dynamicBuild() {
        FlexBox root = new FlexBox(this);
        root.background(0).autoApply(true).boxParams().Sizer(Val.Match);

        this.checklistReport = new FlexText(this);
        this.status = new FlexText(this);
        this.status.text("prêt — 50 items chargés");

        /* --- Données : 50 items, chacun avec sa clé stable et son provider d'état --- */
        for (int i = 0; i < 50; i++) {
            this.items.add(new SmokeRowData(i + 1, "item " + (i + 1)));
        }

        this.adapter = new SmokeListAdapter(this);
        check.run("FlexAdapterObservable.setData(50 items)", () -> this.adapter.setData(this.items));

        /* --- La liste : disposition, adaptateur, états, clics, sélection, chargement à la demande --- */
        this.list = new FlexList(this);
        this.list.boxParams().Sizer("100%", "320");

        check.run("FlexList.line().adapter(...)", () -> this.list.line().adapter(this.adapter));

        check.run("etats vide et de chargement", () -> this.list
                .emptyState(new FlexText(this).text("— aucune donnée —"))
                .loadingState(new FlexText(this).text("— chargement… —")));

        check.run("clic de ligne", () -> this.adapter.onItemClick(
                (item, position, data) -> this.status.text("clic sur la ligne " + position + " : " + data)));

        check.run("selection MULTIPLE + select(1)", () -> {
            this.adapter.selectionMode(FlexAdapterRcv.SELECTION_MULTIPLE);
            this.adapter.select(1);
        });

        check.run("onEndReached (charge 20 items de plus)", () -> this.list.onEndReached(() -> {
            int start = this.items.size();
            for (int i = 0; i < 20; i++) {
                SmokeRowData extra = new SmokeRowData(start + i + 1, "item " + (start + i + 1));
                this.items.add(extra);
                this.adapter.addItem(extra);
            }
            this.status.text("chargement à la demande : " + this.items.size() + " items");
        }));

        /* --- Commandes : chaque scénario de l'étage D et du lot 12, sur un bouton --- */
        FlexBox controls = new FlexBox(this);
        controls.background(0).autoApply(true).boxParams().Sizer(Val.Match, Val.Wrap);

        controls.add(button("mettre a jour l'item 3 (lui seul)", () -> {
            SmokeRowData third = (SmokeRowData) this.items.get(2);
            third.label("item 3 — modifie");
            third.provider().notifyConsumers();          // une seule ligne doit changer
            this.status.text("provider de l'item 3 notifie : seule sa ligne doit changer");
        }));

        controls.add(button("submitData (deplacement + retrait)", () -> {
            List<FlexItemData> reordered = new ArrayList<>(this.items);
            FlexItemData moved = reordered.remove(0);
            reordered.add(5, moved);
            reordered.remove(reordered.size() - 1);
            this.adapter.submitData(reordered);
            this.status.text("submitData : deplacement + retrait, sans repeindre la liste");
        }));

        controls.add(button("selection simple + select(0)", () -> {
            this.adapter.selectionMode(FlexAdapterRcv.SELECTION_SINGLE);
            this.adapter.select(0);
            this.status.text("selection : " + this.adapter.selectedCount() + " ligne(s)");
        }));

        controls.add(button("liste vide (etat vide)", () -> {
            this.adapter.setData(new ArrayList<>());
            this.status.text("liste vide : l'etat vide doit s'afficher");
        }));

        controls.add(button("chargement ON / OFF", () -> {
            boolean loading = !this.status.toString().contains("ON");
            this.list.loading(loading);
            this.status.text("chargement " + (loading ? "ON" : "OFF"));
        }));

        controls.add(button("restaurer les 50 items", () -> {
            this.adapter.setData(this.items);
            this.status.text("donnees restaurees");
        }));

        this.checklistReport.text(this.check.report());
        root.addAll(new FlexView[]{this.checklistReport, this.status, controls, this.list});

        return root;
    }

    /** Bouton de commande : texte + action. */
    private FlexButton button(String label, Runnable action) {
        FlexButton button = new FlexButton(this);
        button.text(label);
        button.background(1).autoApply(true).boxParams().Sizer("100%", "48");
        button.onClick(view -> action.run());
        return button;
    }

    @Override
    protected void attachEvents() {
        // Tout est câblé ci-dessus, sur les boutons de commande.
    }

    @Override
    protected void initialBusinessLogic() {
        // La donnée est fabriquée sur place : cet écran vérifie la liste, pas un chargement distant.
    }
}
