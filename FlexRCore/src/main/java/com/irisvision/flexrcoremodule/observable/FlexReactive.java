// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.observable;

import com.irisvision.flexrcoremodule.enums.FlexScope;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The application-wide registry of {@link FlexProvider}s.
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #declare(List)} is called <b>once</b> — ideally by the first Activity that opens the
 *       application — and registers every provider at once. This is the FlexR counterpart of a
 *       declarative state tree: the state of the application is described in one place, before any
 *       screen is built.</li>
 *   <li>{@link #add(FlexProvider)} registers an extra provider later in the application's life.</li>
 *   <li>{@link #get(String)} returns a provider by its key.</li>
 *   <li>{@link #clean(FlexScope, String)} releases the consumers of a given scope — what a screen or an
 *       Activity calls when it dies.</li>
 * </ol>
 *
 * <h2>Threading</h2>
 *
 * <p>The table is a {@link ConcurrentHashMap} and the static reference is {@code volatile}: observers
 * are notified from {@link FlexNotificationExecutor}'s background thread, while subscriptions arrive on
 * the main thread.
 *
 * <h2>Failing loudly</h2>
 *
 * <p>{@link #get(String)} does not return {@code null} for an unknown key: it throws, with the list of
 * declared providers in the message. A typo in a key must be readable where it happens, not turned into
 * a {@code NullPointerException} far away, at the moment of use.
 */
public final class FlexReactive {

    private static volatile Map<String, FlexProvider> instance;

    private FlexReactive() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Returns the provider registered under this key.
     *
     * @param flexProviderStringKey the key given by {@link FlexProvider#name()}
     * @return the provider, never {@code null}
     * @throws NullPointerException  if the key is {@code null}
     * @throws IllegalStateException if no provider has been declared yet, or if the key is unknown —
     *                               the message lists the declared ones
     */
    public static FlexProvider get(String flexProviderStringKey) {
        Objects.requireNonNull(flexProviderStringKey, "flexProviderStringKey == null");

        Map<String, FlexProvider> current = instance;
        if (current == null) {
            throw new IllegalStateException(
                    "No provider declared: call FlexReactive.declare(...) in the first Activity of the "
                            + "application, before any get().");
        }

        FlexProvider provider = current.get(flexProviderStringKey);
        if (provider == null) {
            throw new IllegalStateException(
                    "Unknown provider: \"" + flexProviderStringKey + "\". Declared providers: "
                            + current.keySet());
        }
        return provider;
    }

    /**
     * Registers a provider during the application's life and returns it.
     *
     * <p>The table is created on demand if it does not exist yet. Registration is idempotent by key: if
     * a provider already holds this key, the existing one is kept and returned.
     *
     * @param flexProvider the provider to register
     * @return the provider effectively registered under that key
     * @throws NullPointerException if the provider is {@code null}, or if its
     *                              {@link FlexProvider#name()} is {@code null}
     */
    public static FlexProvider add(FlexProvider flexProvider) {
        Objects.requireNonNull(flexProvider, "flexProvider == null");

        String key = flexProvider.name();
        Objects.requireNonNull(key, "providerFlag() returned null for " + flexProvider.getClass().getName());

        return map().merge(key, flexProvider, (existing, candidate) -> existing);
    }

    /**
     * Registers every provider of the application at once.
     *
     * <p><b>One call by design.</b> This method initialises the table and only makes sense once, at
     * start-up. A second call while the table already exists is deliberately ignored: to add a provider
     * later, use {@link #add(FlexProvider)}. It is stated here so that the behaviour is not mistaken for
     * a bug.
     *
     * @param providers the providers of the application; {@code null} entries and {@code null} keys are
     *                  skipped
     * @throws NullPointerException if the list itself is {@code null}
     */
    public static void declare(List<FlexProvider> providers) {
        Objects.requireNonNull(providers, "providers == null");

        synchronized (FlexReactive.class) {
            if (instance != null) {
                return; // already declared: the existing table is kept (see the class documentation).
            }
            Map<String, FlexProvider> created = new ConcurrentHashMap<>();
            for (FlexProvider provider : providers) {
                if (provider == null) continue;
                String key = provider.name();
                if (key == null) continue;
                created.put(key, provider);
            }
            instance = created;
        }
    }

    /** Current table, created on demand. */
    private static Map<String, FlexProvider> map() {
        Map<String, FlexProvider> current = instance;
        if (current == null) {
            synchronized (FlexReactive.class) {
                current = instance;
                if (current == null) {
                    current = new ConcurrentHashMap<>();
                    instance = current;
                }
            }
        }
        return current;
    }

    /**
     * Releases, in every provider, the consumers of the given scope whose identity matches
     * {@code scopeId}.
     *
     * @param scope   the scope to purge
     * @param scopeId the identity to purge; {@code null} does nothing
     * @throws NullPointerException if the scope is {@code null}
     */
    public static void clean(FlexScope scope, String scopeId) {
        Map<String, FlexProvider> current = instance;
        if (current == null) {
            return;
        }

        Objects.requireNonNull(scope, "scope == null");

        // Iterating over a snapshot: a release must never disturb the loop.
        for (FlexProvider provider : List.copyOf(current.values())) {
            switch (scope) {
                case FlexActivity:
                    provider.unSubscribeActivityScope(scopeId);
                    break;
                case FlexFragment:
                    provider.unSubscribeFragScope(scopeId);
                    break;
                case FlexBackground:
                    provider.unSubscribeBackgroundScope(scopeId);
                    break;
            }
        }
    }

    /**
     * Prints the number of subscribers of each provider, per scope.
     *
     * <p>A diagnostic tool: it is the quickest way to check that a screen released its subscriptions.
     * Not meant for production code.
     */
    public static void info() {
        Map<String, FlexProvider> current = instance;
        if (current == null) {
            return;
        }
        current.forEach((key, provider) ->
                System.out.println("Name-> " + provider.name() + " =:::::= Size-> {"
                        + provider.size(FlexScope.FlexFragment.name()) + ","
                        + provider.size(FlexScope.FlexActivity.name()) + ","
                        + provider.size(FlexScope.FlexBackground.name()) + "}"));
    }
}
