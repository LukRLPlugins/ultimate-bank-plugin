package com.ultimatebank.feature.storagesavings.tiers;

/**
 * A config dropdown value for a storage's tier: auto-detect, not built, or a tier index into
 * {@link PohStorage#getTiers()}. One enum per storage so each dropdown lists only that storage's tiers.
 */
public interface TierChoice
{
	int AUTO = -2;

	/**
	 * @return {@link #AUTO}, {@link PohStorage#NOT_BUILT}, or a tier index
	 */
	int tier();

	enum FancyDressBox implements TierChoice
	{
		AUTO_DETECT("Auto-detect", AUTO), NOT_BUILT("Not built", PohStorage.NOT_BUILT), OAK("Oak", 0), TEAK("Teak", 1),
		MAHOGANY("Mahogany", 2);

		private final String label;
		private final int tier;

		FancyDressBox(String label, int tier)
		{
			this.label = label;
			this.tier = tier;
		}

		@Override
		public int tier()
		{
			return tier;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum ArmourCase implements TierChoice
	{
		AUTO_DETECT("Auto-detect", AUTO), NOT_BUILT("Not built", PohStorage.NOT_BUILT), OAK("Oak", 0), TEAK("Teak", 1),
		MAHOGANY("Mahogany", 2);

		private final String label;
		private final int tier;

		ArmourCase(String label, int tier)
		{
			this.label = label;
			this.tier = tier;
		}

		@Override
		public int tier()
		{
			return tier;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum MagicWardrobe implements TierChoice
	{
		AUTO_DETECT("Auto-detect", AUTO), NOT_BUILT("Not built", PohStorage.NOT_BUILT), OAK("Oak", 0),
		CARVED_OAK("Carved oak", 1), TEAK("Teak", 2), CARVED_TEAK("Carved teak", 3), MAHOGANY("Mahogany", 4),
		GILDED("Gilded", 5), MARBLE("Marble", 6);

		private final String label;
		private final int tier;

		MagicWardrobe(String label, int tier)
		{
			this.label = label;
			this.tier = tier;
		}

		@Override
		public int tier()
		{
			return tier;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum CapeRack implements TierChoice
	{
		AUTO_DETECT("Auto-detect", AUTO), NOT_BUILT("Not built", PohStorage.NOT_BUILT), OAK("Oak", 0), TEAK("Teak", 1),
		MAHOGANY("Mahogany", 2), GILDED("Gilded", 3), MARBLE("Marble", 4), MAGICAL("Magical", 5);

		private final String label;
		private final int tier;

		CapeRack(String label, int tier)
		{
			this.label = label;
			this.tier = tier;
		}

		@Override
		public int tier()
		{
			return tier;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum TreasureChest implements TierChoice
	{
		AUTO_DETECT("Auto-detect", AUTO), NOT_BUILT("Not built", PohStorage.NOT_BUILT), OAK("Oak", 0), TEAK("Teak", 1),
		MAHOGANY("Mahogany", 2);

		private final String label;
		private final int tier;

		TreasureChest(String label, int tier)
		{
			this.label = label;
			this.tier = tier;
		}

		@Override
		public int tier()
		{
			return tier;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
