package com.irisvision.flexrsmoke;

import android.util.Log;

import java.util.concurrent.Callable;

/**
 * Checklist d'exécution d'un écran de fumée.
 *
 * <p>Chaque étape est isolée : l'échec d'un composant est <b>enregistré</b> (et journalisé) sans
 * empêcher les suivants d'être exercés. Un seul passage sur l'écran donne donc le verdict complet —
 * à l'écran (le rapport est affiché) et dans {@code adb logcat} (tag {@code FlexRSmoke}).
 */
public final class SmokeChecklist {

    private static final String TAG = "FlexRSmoke";

    private final String family;
    private final StringBuilder report = new StringBuilder();
    private int ok = 0;
    private int ko = 0;

    public SmokeChecklist(String family) {
        this.family = family;
        Log.i(TAG, "=== " + family + " : début ===");
    }

    /** Exécute une étape et renvoie sa valeur, ou {@code null} si elle a échoué. */
    public <T> T step(String label, Callable<T> step) {
        try {
            T value = step.call();
            this.ok++;
            this.report.append("OK  ").append(label).append('\n');
            Log.i(TAG, "OK  " + label);
            return value;
        }
        catch (Throwable failure) {
            this.ko++;
            this.report.append("KO  ").append(label).append("  →  ").append(failure).append('\n');
            Log.e(TAG, "KO  " + label, failure);
            return null;
        }
    }

    /** Exécute une étape sans valeur de retour. */
    public void run(String label, Runnable step) {
        step(label, () -> {
            step.run();
            return null;
        });
    }

    /** Rapport lisible, à afficher sur l'écran lui-même. */
    public String report() {
        Log.i(TAG, "=== " + family + " : " + this.ok + " OK, " + this.ko + " KO ===");
        return this.family + "  —  " + this.ok + " OK, " + this.ko + " KO\n\n" + this.report;
    }
}
