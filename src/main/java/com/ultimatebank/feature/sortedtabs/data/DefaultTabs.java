package com.ultimatebank.feature.sortedtabs.data;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * The default skill tabs and their items, from {@code default-tabs.json} (converted from Bank Skill Sorting's
 * SkillBankData by Frailrain; see docs/attribution.md). Section placement comes from {@link SortData#itemMeta()}.
 */
@Slf4j
public final class DefaultTabs
{
	private static final String RESOURCE = "/com/ultimatebank/feature/sortedtabs/default-tabs.json";

	private DefaultTabs()
	{
	}

	@Value
	public static class Tab
	{
		/** Internal id, e.g. "melee" or "woodcutting_firemaking" */
		String id;
		/** Item shown as the tab icon */
		int icon;
		/** Every item that belongs in the tab by default */
		List<Integer> items;

		/**
		 * @return the user-facing name, e.g. "woodcutting + firemaking"
		 */
		public String displayName()
		{
			return id.replace("_", " + ");
		}
	}

	/**
	 * @param gson the injected Gson (plugins must not construct their own)
	 */
	public static List<Tab> load(Gson gson)
	{
		try (InputStream in = DefaultTabs.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("{} not found", RESOURCE);
				return Collections.emptyList();
			}
			File file = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), File.class);
			return file.tabs == null ? Collections.emptyList() : file.tabs;
		}
		catch (Exception e)
		{
			log.warn("Couldn't load {}", RESOURCE, e);
			return Collections.emptyList();
		}
	}

	private static final class File
	{
		List<Tab> tabs;
	}
}
