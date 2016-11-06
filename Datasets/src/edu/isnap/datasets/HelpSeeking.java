package edu.isnap.datasets;

import java.util.Date;

import edu.isnap.ctd.hint.HintConfig;
import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.Dataset;
import edu.isnap.hint.ConfigurableAssignment;

	public class HelpSeeking extends Dataset {

		public final static HelpSeeking instance = new HelpSeeking();
		public final static Date start = Assignment.date(2016, 8, 10);
		public final static String dataDir = "../data/help-seeking/experts2016";
		public final static String dataFile = dataDir + ".csv";

		private final static HintConfig config = new HintConfig();
		static {
			config.pruneGoals = 1;
			config.pruneNodes = 0;
			config.stayProportion = 0.55;
		}

		public final static Assignment BrickWall = new ConfigurableAssignment(instance,
				"brickWall", null, true) {
			@Override
			public HintConfig getConfig() {
				return config;
			};
		};
		public final static Assignment GuessingGame1 = new Assignment(instance,
				"guess1Lab", null, true);

		public final static Assignment[] All = {
			BrickWall, GuessingGame1
		};

		private HelpSeeking() {
			super(start, dataDir);
		}

		@Override
		public Assignment[] all() {
			return All;
		}
	}