package com.irisvision.flexrsmoke.fxtheme;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.ui.layout.FlexButton;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrfxmodule.FlexAnimate;
import com.irisvision.flexrfxmodule.FlexChoreo;
import com.irisvision.flexrthememodule.FlexTheme;
import com.irisvision.flexrsmoke.SmokeChecklist;

/**
 * Écran de fumée — famille « animations et thème » (lot 13).
 *
 * <p>Il exerce les corrections du lot 13 <b>par l'exécution</b> : animation de <b>taille</b> (X1, dont
 * la chaîne était morte trois fois), paddings et marges par côté avec refus d'un côté inconnu (X11),
 * {@code scrollX} en entier (X5), et les deux modes de {@link FlexChoreo} (X4/X6/X9/X10). Il bascule
 * ensuite le thème pour vérifier que des vues <b>déjà construites</b> se repeignent — y compris la
 * décoration d'un champ, qui suit désormais le thème (U40).
 */
public class SmokeFxThemeBody extends FlexBody {

    private final SmokeChecklist check = new SmokeChecklist("fx + theme");

    /** Compteur de trames des deux chorégraphies : sert aussi de condition d'arrêt. */
    private int frames = 0;

    private FlexText checklistReport;
    private FlexText status;
    private FlexBox animated;
    private FlexCard themedCard;
    private FlexBox themedShapeBox;

    @Override
    protected FlexView dynamicBuild() {
        FlexBox root = new FlexBox(this);
        root.background(0).autoApply(true).boxParams().Sizer(Val.Match);

        this.checklistReport = new FlexText(this);
        this.status = new FlexText(this);
        this.status.text("prêt");

        // La cible des animations : toute FlexView implémente FlexPropertiesAnimate.
        this.animated = new FlexBox(this);
        this.animated.background(2).autoApply(true).boxParams().Sizer("120", "60");

        // Deux vues thématiques : elles existent AVANT la bascule, pour vérifier qu'elles se repeignent.
        this.themedCard = new FlexCard(this);
        this.themedCard.cardColor(1).autoApply(true).boxParams().Sizer("100%", "56");

        FlexShape themedShape = new FlexShape(this);
        themedShape.newCompose().likeRelative().toAllCorners(0.25f, false).shapeColor(2);

        this.themedShapeBox = new FlexBox(this);
        this.themedShapeBox.background(themedShape).autoApply(true).boxParams().Sizer("100%", "56");

        FlexBox controls = new FlexBox(this);
        controls.background(0).autoApply(true).boxParams().Sizer(Val.Match, Val.Wrap);

        /* --- Animations : une entrée par correction du lot 13 --- */

        check.run("FlexAnimate : translation / echelle / alpha", () -> {
            FlexAnimate animate = new FlexAnimate(getFlexFragment());
            animate.translationX(0f, 60f, 600, 0, null, this.animated);
            animate.translationY(0f, 40f, 600, 0, null, this.animated);
            animate.scaleX(1f, 1.3f, 600, 0, null, this.animated);
            animate.scaleY(1f, 1.3f, 600, 0, null, this.animated);
            animate.alpha(1f, 0.4f, 600, 0, null, this.animated);
        });

        check.run("FlexAnimate.width() / height()   [X1 : chaine morte]", () -> {
            FlexAnimate animate = new FlexAnimate(getFlexFragment());
            animate.width(120, 300, 900, 0, null, this.animated);
            animate.height(60, 180, 900, 0, null, this.animated);
        });

        check.run("FlexAnimate paddings / margins / scrollX   [X5, X11]", () -> {
            FlexAnimate animate = new FlexAnimate(getFlexFragment());
            animate.paddings(0, 24, 600, 0, null, this.animated);
            animate.padding("l", 0, 24, 600, 0, null, this.animated);
            animate.margins(0, 16, 600, 0, null, this.animated);
            animate.scrollX(0, 40, 600, 0, null, this.animated);
        });

        check.run("un cote inconnu est REFUSE   [X11]", () -> {
            FlexAnimate animate = new FlexAnimate(getFlexFragment());
            try {
                animate.padding("X", 0, 10, 100, 0, null, this.animated);
                throw new AssertionError("padding(\"X\") aurait dû être refusé, pas ignoré");
            }
            catch (IllegalArgumentException expected) {
                // Comportement attendu : refus explicite au lieu du silence.
            }
        });

        /* --- FlexChoreo : les deux modes d'exécution --- */

        check.run("FlexChoreo mode Timer   [X4]", () -> {
            FlexChoreo choreo = new FlexChoreo(FlexChoreo.ExecMode.Timer, true, 0, getFlexFragment()) {
                @Override
                public void callback(long frameTimeNanos) {
                    frames++;
                }
            };
            choreo.timer(2000);
            choreo.launch();
        });

        check.run("FlexChoreo mode Condition   [X6, X9, X10]", () -> {
            FlexChoreo choreo = new FlexChoreo(FlexChoreo.ExecMode.Condition, true, 0, getFlexFragment()) {
                @Override
                public void callback(long frameTimeNanos) {
                    frames++;
                    // La condition est repoussée DEPUIS le callback : c'est le mode d'emploi.
                    refreshCondition(frames < 120);
                }
            };
            choreo.launch();
            choreo.pause();
            choreo.resume();
        });

        /* --- Thème et ressource par nom --- */

        controls.add(button("basculer le theme   [U13/U35/U40]", () -> {
            boolean toAlt = !SmokeFxThemeActivity.THEME_ALT.equals(FlexTheme.currentTheme);
            FlexTheme.appChangeTheme(toAlt ? SmokeFxThemeActivity.THEME_ALT : "smoke");
            this.status.text("theme courant : " + FlexTheme.currentTheme
                    + "   (les vues deja construites doivent changer de couleur)");
        }));

        controls.add(button("fond par NOM   [flexDrawable]", () -> {
            this.animated.background("smoke_probe");
            this.status.text("fond pose par son nom : smoke_probe");
        }));

        controls.add(button("rejouer translation + taille", () -> {
            FlexAnimate animate = new FlexAnimate(getFlexFragment());
            animate.translationX(0f, 60f, 600, 0, null, this.animated);
            animate.width(120, 300, 900, 0, null, this.animated);
            this.status.text("animations relancees : translation + taille");
        }));

        FlexBox themed = new FlexBox(this);
        themed.background(0).autoApply(true).boxParams().Sizer(Val.Match, Val.Wrap);
        themed.addAll(new FlexView[]{this.animated, this.themedCard, this.themedShapeBox});

        this.checklistReport.text(this.check.report());
        root.addAll(new FlexView[]{this.checklistReport, this.status, controls, themed});

        return root;
    }

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
        // Rien à charger : cet écran vérifie les animations et le thème.
    }
}
