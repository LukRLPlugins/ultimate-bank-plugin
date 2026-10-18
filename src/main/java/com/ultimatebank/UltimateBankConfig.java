package com.ultimatebank;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

/**
 * One {@code @ConfigSection} per feature. Each section starts with an enable toggle,
 * followed by that feature's settings. Never rename a key without a migration.
 */
@ConfigGroup(UltimateBankConfig.GROUP)
public interface UltimateBankConfig extends Config
{
	String GROUP = "ultimatebank";
}
