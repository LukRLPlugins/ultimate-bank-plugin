package com.ultimatebank.feature.storagesavings.marker;

/**
 * How storable bank items are marked. Config enums for the storable marker.
 */
public enum MarkerStyle
{
	ITEM_OUTLINE("Item outline"),
	BOX_OUTLINE("Box outline"),
	CORNER_TRIANGLE("Corner triangle"),
	CORNER_ICON("Corner icon");

	private final String label;

	MarkerStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}

	public enum Corner
	{
		TOP_LEFT("Top left"),
		TOP_RIGHT("Top right"),
		BOTTOM_LEFT("Bottom left"),
		BOTTOM_RIGHT("Bottom right");

		private final String label;

		Corner(String label)
		{
			this.label = label;
		}

		public boolean isRight()
		{
			return this == TOP_RIGHT || this == BOTTOM_RIGHT;
		}

		public boolean isBottom()
		{
			return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public enum Icon
	{
		HOUSE("House"),
		CHEST("Chest"),
		STAR("Star"),
		EXCLAMATION("Exclamation mark"),
		ARROW("Arrow"),
		CUSTOM_ITEM("Custom item (by ID)");

		private final String label;

		Icon(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
