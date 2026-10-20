package com.irisvision.flexrsmoke.containers;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.component.FlexProgress;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.ui.container.FlexContainer;
import com.irisvision.flexrcoremodule.ui.container.FlexRadioGroup;
import com.irisvision.flexrcoremodule.ui.layout.FlexButton;
import com.irisvision.flexrcoremodule.ui.layout.FlexCheck;
import com.irisvision.flexrcoremodule.ui.layout.FlexImg;
import com.irisvision.flexrcoremodule.ui.layout.FlexInput;
import com.irisvision.flexrcoremodule.ui.layout.FlexRadioButton;
import com.irisvision.flexrcoremodule.ui.layout.FlexSlider;
import com.irisvision.flexrcoremodule.ui.layout.FlexSliderRange;
import com.irisvision.flexrcoremodule.ui.layout.FlexSwitch;
import com.irisvision.flexrcoremodule.ui.layout.FlexTag;
import com.irisvision.flexrcoremodule.ui.layout.FlexTagGroup;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexFlipper;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexGrid;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexScrollHorizontal;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexScrollNested;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexScrollVertical;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrsmoke.SmokeChecklist;

/**
 * Écran de fumée — familles « conteneurs, dispositions et composants » (lot 11).
 *
 * <p>Il instancie et configure les classes qui n'étaient <b>instanciées nulle part</b> dans le dépôt,
 * en isolant chaque étape : un composant en panne n'empêche pas les suivants, et le rapport s'affiche
 * à l'écran comme dans {@code adb logcat} (tag {@code FlexRSmoke}).
 *
 * <p>Y figurent en particulier les situations qu'aucun test ne pouvait couvrir : conteneurs recevant
 * enfin leurs enfants (U6/L5), animation de fond, coins relatifs (U8), décoration de champ par index
 * (U16/U40), et une ressource désignée <b>par son nom</b> ({@code smoke_probe}), qui sert aussi de
 * témoin pour l'expérience {@code shrinkResources}.
 */
public class SmokeContainersBody extends FlexBody {

    private final SmokeChecklist check = new SmokeChecklist("conteneurs");
    private FlexText report;

    @Override
    protected FlexView dynamicBuild() {
        FlexBox root = new FlexBox(this);
        root.background(0).autoApply(true).boxParams().Sizer(Val.Match);

        this.report = new FlexText(this);
        this.report.text("écran de fumée — conteneurs : en cours…");

        FlexBox column = new FlexBox(this);
        column.background(0).autoApply(true).boxParams().Sizer(Val.Match, Val.Wrap);

        /* 1. Conteneurs et familles — U6/L5 : ils refusaient silencieusement TOUS leurs enfants. */

        check.run("FlexCard -> enfant", () -> {
            FlexCard card = new FlexCard(this);
            card.cardColor(1).autoApply(true).boxParams().Sizer("100%", "56");

            FlexText label = new FlexText(this);
            label.text("FlexCard");
            card.add(label);

            column.add(card);
        });

        check.run("FlexContainer -> enfant", () -> {
            FlexContainer container = new FlexContainer(this);
            container.background(1).autoApply(true).boxParams().Sizer("100%", "56");

            FlexText label = new FlexText(this);
            label.text("FlexContainer");
            container.add(label);

            column.add(container);
        });

        check.run("FlexRadioGroup -> 2 FlexRadioButton", () -> {
            FlexRadioGroup group = new FlexRadioGroup(this);
            group.boxParams().Sizer("100%", "56");

            FlexRadioButton first = new FlexRadioButton(this);
            FlexRadioButton second = new FlexRadioButton(this);
            group.add(first);
            group.add(second);

            column.add(group);
        });

        /* 2. Dispositions : grille, bascule, défilement. */

        check.run("FlexGrid (2 colonnes) -> enfants", () -> {
            FlexGrid grid = new FlexGrid(this);
            grid.columnCount(2).rowCount(2).horizontal();
            grid.boxParams().Sizer("100%", "80");

            FlexText one = new FlexText(this);
            one.text("grille 1");
            FlexText two = new FlexText(this);
            two.text("grille 2");
            grid.add(one);
            grid.add(two);

            column.add(grid);
        });

        check.run("FlexFlipper -> enfants + start/stop", () -> {
            FlexFlipper flipper = new FlexFlipper(this);
            flipper.interval(1500);
            flipper.boxParams().Sizer("100%", "56");

            FlexText first = new FlexText(this);
            first.text("flipper A");
            FlexText second = new FlexText(this);
            second.text("flipper B");
            flipper.add(first);
            flipper.add(second);

            flipper.goToNext();
            flipper.goToPrevious();
            flipper.start();
            flipper.stop();

            column.add(flipper);
        });

        check.run("FlexScrollHorizontal", () -> {
            FlexScrollHorizontal scroll = new FlexScrollHorizontal(this);
            scroll.boxParams().Sizer("100%", "56");

            FlexText wide = new FlexText(this);
            wide.text("contenu large");
            scroll.add(wide);

            scroll.scrollTo(10);
            scroll.isMoreElements();

            column.add(scroll);
        });

        check.run("FlexScrollVertical", () -> {
            FlexScrollVertical scroll = new FlexScrollVertical(this);
            scroll.boxParams().Sizer("100%", "56");

            FlexText content = new FlexText(this);
            content.text("contenu vertical");
            scroll.add(content);

            scroll.scrollTo(10);
            column.add(scroll);
        });

        check.run("FlexScrollNested", () -> {
            FlexScrollNested scroll = new FlexScrollNested(this);
            scroll.boxParams().Sizer("100%", "56");

            FlexText content = new FlexText(this);
            content.text("contenu imbriqué");
            scroll.add(content);

            column.add(scroll);
        });

        /* 3. Composants de saisie — U16 (rendu), U18 (vocabulaire), U40 (index de palette). */

        check.run("FlexInput (boxHint + boxDecoration par index)", () -> {
            FlexInput input = new FlexInput(this, false);
            input.boxHint("champ de fumée", 4);
            input.boxDecoration(1, 1, 2, 2);
            input.boxParams().Sizer("100%", "72");

            column.add(input);
        });

        check.run("FlexSlider", () -> {
            FlexSlider slider = new FlexSlider(this);
            slider.boxParams().Sizer("100%", "48");
            column.add(slider);
        });

        check.run("FlexSliderRange", () -> {
            FlexSliderRange slider = new FlexSliderRange(this);
            slider.boxParams().Sizer("100%", "48");
            column.add(slider);
        });

        check.run("FlexCheck / FlexSwitch", () -> {
            FlexCheck checkBox = new FlexCheck(this);
            FlexSwitch switchView = new FlexSwitch(this);
            column.add(checkBox);
            column.add(switchView);
        });

        /* 4. Autres composants. */

        check.run("FlexTag dans FlexTagGroup", () -> {
            FlexTagGroup group = new FlexTagGroup(this);
            group.boxParams().Sizer("100%", "56");

            FlexTag tag = new FlexTag(this);
            group.add(tag);

            column.add(group);
        });

        check.run("FlexButton", () -> {
            FlexButton button = new FlexButton(this);
            button.boxParams().Sizer("100%", "56");
            column.add(button);
        });

        check.run("FlexText", () -> {
            FlexText text = new FlexText(this);
            text.text("FlexText");
            column.add(text);
        });

        check.run("FlexImg.load(\"smoke_probe\")  [ressource par son nom]", () -> {
            FlexImg image = new FlexImg(this);
            image.load("smoke_probe");
            image.boxParams().Sizer("64", "64");
            column.add(image);
        });

        check.run("FlexProgress", () -> {
            FlexProgress progress = new FlexProgress(this);
            column.add(progress);
        });

        check.run("FlexShape (coins relatifs, U8)", () -> {
            FlexShape shape = new FlexShape(this);
            shape.newCompose().likeRelative().toAllCorners(0.5f, false).shapeColor(2);

            FlexBox decorated = new FlexBox(this);
            decorated.background(shape).autoApply(true).boxParams().Sizer("100%", "64");
            column.add(decorated);
        });

        check.run("FlexBox (reference)", () -> {
            FlexBox box = new FlexBox(this);
            box.background(1).autoApply(true).boxParams().Sizer("100%", "40");
            column.add(box);
        });

        /* 5. Navigation vers les deux autres écrans de fumée. */

        check.run("ouverture de l'ecran « listes »", () -> column.add(
                navigation("-> ecran listes", com.irisvision.flexrsmoke.list.SmokeListActivity.class)));

        check.run("ouverture de l'ecran « fx + theme »", () -> column.add(
                navigation("-> ecran fx + theme", com.irisvision.flexrsmoke.fxtheme.SmokeFxThemeActivity.class)));

        this.report.text(this.check.report());
        root.addAll(new FlexView[]{this.report, column});

        return root;
    }

    /** Bouton qui ouvre un autre écran de fumée (seule la première activité est exposée au lanceur). */
    private FlexButton navigation(String label, Class<?> activityClass) {
        FlexButton button = new FlexButton(this);
        button.text(label);
        button.background(2).autoApply(true).boxParams().Sizer("100%", "48");
        button.onClick(view -> {
            android.content.Intent intent =
                    new android.content.Intent(getFlexContext(), activityClass);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            getFlexContext().startActivity(intent);
        });
        return button;
    }

    @Override
    protected void attachEvents() {
        // Les interactions sont testées sur l'écran « listes » (clics, sélection).
    }

    @Override
    protected void initialBusinessLogic() {
        // Rien à charger : cet écran vérifie la construction, pas la donnée.
    }
}
