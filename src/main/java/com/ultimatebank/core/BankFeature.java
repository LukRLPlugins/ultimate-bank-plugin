package com.ultimatebank.core;

import com.ultimatebank.UltimateBankConfig;

/**
 * A self-contained feature of the plugin (e.g. bank value, tag layouts).
 * <p>
 * While enabled, the feature instance is registered on the EventBus, so it may declare
 * {@code @Subscribe} methods directly. Features are Guice singletons created by the plugin's
 * injector, so they may {@code @Inject} anything the plugin can.
 * <p>
 * To add a feature, follow .claude/skills/new-feature/SKILL.md.
 */
public interface BankFeature
{
	/**
	 * Whether the user has this feature switched on. Usually reads a single toggle from config.
	 */
	boolean isEnabled(UltimateBankConfig config);

	/**
	 * Called on the client thread after the feature is registered on the EventBus.
	 * Add overlays, build caches, etc. here.
	 */
	default void startUp()
	{
	}

	/**
	 * Called on the client thread before the feature is unregistered from the EventBus.
	 * Must undo everything startUp() did (remove overlays, restore widgets, clear caches).
	 */
	default void shutDown()
	{
	}
}
