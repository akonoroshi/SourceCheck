package com.snap.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import com.snap.data.Snapshot;
import com.snap.parser.Store.Mode;

public class Assignment {
	public final String dataDir, name;
	public final Date start, end;
	public final boolean hasIDs;
	public final boolean graded;

	public Assignment(String dataDir, String name, Date start, Date end, boolean hasIDs) {
		this(dataDir, name, start, end, hasIDs, false);
	}

	public Assignment(String dataDir, String name, Date start, Date end,
			boolean hasIDs, boolean graded) {
		this.dataDir = dataDir;
		this.name = name;
		this.start = start;
		this.end = end;
		this.hasIDs = hasIDs;
		this.graded = graded;
	}

	public String analysisDir() {
		return dataDir + "/analysis/" + name;
	}

	public String unitTestDir() {
		return dataDir + "/unittests/" + name;
	}

	public Snapshot loadSolution() throws FileNotFoundException {
		return Snapshot.parse(new File(dataDir + "/solutions/", name + ".xml"));
	}

	public Snapshot loadTest(String name) throws FileNotFoundException {
		return Snapshot.parse(new File(dataDir + "/tests/", name + ".xml"));
	}

	public SolutionPath loadSubmission(String id, Mode mode, boolean snapshotsOnly) {
		try {
			return new SnapParser(dataDir, mode).parseSubmission(this, id, snapshotsOnly);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean ignore(String student) {
		return false;
	}

	@Override
	public String toString() {
		return dataDir + "/" + name;
	}

	public Map<String, SolutionPath> load() {
		return load(Mode.Use, false);
	}

	public Map<String, SolutionPath> load(Mode mode, boolean snapshotsOnly) {
		return new SnapParser(dataDir, mode).parseAssignment(name, snapshotsOnly, start, end);
	}

	public final static String BASE_DIR = "../data/csc200";

	// Note: end dates are generally 2 days past last class due date

	public static class Fall2015 {
		public final static Date start = date(2015, 8, 10);
		public final static String dataDir = BASE_DIR + "/fall2015";

		// Used this submission for testing, so not using it in evaluation
		public final static String GG1_SKIP = "3c3ce047-b408-417e-b556-f9406ac4c7a8.csv";

		public final static Assignment LightsCameraAction = new Assignment(dataDir,
				"lightsCameraActionHW", start, date(2016, 9, 4), false);
		public final static Assignment PolygonMaker = new Assignment(dataDir,
				"polygonMakerLab", start, date(2015, 9, 4), false);
		public final static Assignment Squiral = new Assignment(dataDir,
				"squiralHW", start, date(2015, 9, 13), false);
		public final static Assignment GuessingGame1 = new Assignment(dataDir,
				"guess1Lab", start,
				date(2015, 9, 18), false, true) {
			@Override
			public boolean ignore(String id) {
				return GG1_SKIP.equals(id);
			};
		};
		public final static Assignment GuessingGame2 = new Assignment(dataDir,
				"guess2HW", start, date(2015, 9, 25), false);
		public final static Assignment GuessingGame3 = new Assignment(dataDir,
				"guess3Lab", start, date(2015, 10, 2), false);

		public final static Assignment[] All = {
			LightsCameraAction, PolygonMaker, Squiral,
			GuessingGame1, GuessingGame2, GuessingGame3
		};
	}

	public static class Spring2016 {
		public final static Date start = date(2016, 1, 1);
		public final static String dataDir = BASE_DIR + "/spring2016";

		public final static Assignment LightsCameraAction = new Assignment(dataDir,
				"lightsCameraActionHW", start, date(2016, 1, 29), true);
		public final static Assignment PolygonMaker = new Assignment(dataDir,
				"polygonMakerLab", start, date(2016, 2, 2), true);
		public final static Assignment Squiral = new Assignment(dataDir,
				"squiralHW", start, date(2016, 2, 9), true);
		public final static Assignment GuessingGame1 = new Assignment(dataDir,
				"guess1Lab", start, date(2016, 2, 9), true, true);
		public final static Assignment GuessingGame2 = new Assignment(dataDir,
				"guess2HW", start, date(2016, 2, 16), true);
		public final static Assignment GuessingGame3 = new Assignment(dataDir,
				"guess3Lab", start, date(2016, 2, 23), true);

		public final static Assignment[] All = {
			LightsCameraAction, PolygonMaker, Squiral,
			GuessingGame1, GuessingGame2, GuessingGame3
		};
	}

	public static Date date(int year, int month, int day) {
		// NOTE: GregorianCalendar months are 0-based, thus the 'month - 1'
		return new GregorianCalendar(year, month - 1, day).getTime();
	}
}
