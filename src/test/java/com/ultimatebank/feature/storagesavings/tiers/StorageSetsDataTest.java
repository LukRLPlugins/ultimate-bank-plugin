package com.ultimatebank.feature.storagesavings.tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class StorageSetsDataTest
{
	static final class WikiSet
	{
		String set;
		List<Integer> ids;
	}

	@Test
	public void everyTieredStorageHasSetsWithIds() throws Exception
	{
		// Test-only Gson; plugin code uses the injected one
		Map<String, List<WikiSet>> data;
		try (InputStreamReader in = new InputStreamReader(
			getClass().getResourceAsStream("/com/ultimatebank/feature/storagesavings/storage-sets.json"), StandardCharsets.UTF_8))
		{
			data = new Gson().fromJson(in, new TypeToken<Map<String, List<WikiSet>>>()
			{
			}.getType());
		}
		for (PohStorage storage : PohStorage.values())
		{
			List<WikiSet> sets = data.get(storage.name());
			assertTrue(storage + " has sets", sets != null && !sets.isEmpty());
			long withIds = sets.stream().filter(s -> s.ids != null && !s.ids.isEmpty()).count();
			assertTrue(storage + ": nearly every set resolves to item IDs", withIds >= sets.size() - 2);
		}
		assertEquals(8, data.get("FANCY_DRESS_BOX").size());
	}
}
