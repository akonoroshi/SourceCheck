package edu.isnap.datasets;

import java.util.Date;

import edu.isnap.ctd.hint.HintConfig;
import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.Dataset;
import edu.isnap.hint.ConfigurableAssignment;

public class BJCSolutions2017 extends Dataset {

	public final static Date start = null;
	public final static String dataDir = "../data/bjc/solutions2017";
	public final static String dataFile = dataDir + ".csv";
	public final static BJCSolutions2017 instance = new BJCSolutions2017();

	private static class BJCAssignment extends ConfigurableAssignment {

		public BJCAssignment(String name) {
			super(instance, name, null, false);
		}

		@Override
		public HintConfig getConfig() {
			HintConfig config = new HintConfig();
			config.progressMissingFactor = 0.1f;
			return config;
		}

	}

	public final static Assignment U1_L1_Alonzo = new BJCAssignment("U1_L1_Alonzo");

	public static final Assignment U1_L2_Gossip = new BJCAssignment("U1_L2_Gossip");

	public static final Assignment U1_L2_P4_GreetPlayer = new BJCAssignment("U1_L2_P4_GreetPlayer");

	public final static Assignment U1_P1_LineArt = new BJCAssignment("U1_P1_LineArt");

	public final static Assignment[] All = {
			U1_L1_Alonzo,
			U1_L2_Gossip,
			U1_L2_P4_GreetPlayer,
			U1_P1_LineArt
	};

	private BJCSolutions2017() {
		super(start, dataDir);
	}

	@Override
	public Assignment[] all() {
		return All;
	}
}