package edu.isnap.datasets;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.AssignmentAttempt;
import edu.isnap.dataset.Dataset;
import edu.isnap.dataset.Grade;
import edu.isnap.parser.SnapParser;
import edu.isnap.parser.Store.Mode;

public class Fall2015 extends Dataset {

		public final static Fall2015 instance = new Fall2015();
		public final static Date start = Assignment.date(2015, 8, 10);
		public final static String dataDir = Assignment.BASE_DIR + "/fall2015";
		public final static String dataFile = dataDir + ".csv";

		// Used this submission for testing, so not using it in evaluation
		// For the comparison 2015/2016 study we should keep it, though
		public final static String GG1_SKIP = "3c3ce047-b408-417e-b556-f9406ac4c7a8";


		// Note: the first three assignments were not recorded in Fall 2015
		public final static Assignment LightsCameraAction = new FakeAssignment(instance,
				"lightsCameraActionHW", Assignment.date(2016, 9, 4));
		public final static Assignment PolygonMaker = new FakeAssignment(instance,
				"polygonMakerLab", Assignment.date(2015, 9, 4));
		public final static Assignment Squiral = new FakeAssignment(instance,
				"squiralHW", Assignment.date(2015, 9, 13));

		public final static Assignment GuessingGame1 = new Assignment(instance,
				"guess1Lab", Assignment.date(2015, 9, 18), false, true, null) {
			@Override
			public Assignment getLocationAssignment(String attemptID) {
				switch (attemptID) {
					// Did their GG1 work under none, but also did a short load/export under GG1
					// so the detection algorithm misses it
					case "0cc151f3-a9db-4a03-9671-d5c814b3bbbe": return None;
				}
				return super.getLocationAssignment(attemptID);
			}
		};

		public final static Assignment GuessingGame2 = new Assignment(instance,
				"guess2HW", Assignment.date(2015, 9, 25), false, true, GuessingGame1) {
			@Override
			public Assignment getLocationAssignment(String attemptID) {
				switch (attemptID) {
					// Did GG2 on GG1, but then edited some more on "none", so no complete file
					case "10e87347-75ca-4d07-9b65-138c47332aca":
					// Did GG2 under GG1, but also has "none" work so this is to override that
					case "0cc151f3-a9db-4a03-9671-d5c814b3bbbe":
						return GuessingGame1;
					// Did their GG2 work under GG3, which includes no GG3 work...
					case "94833254-29ae-428a-b800-4a7efc699ef4":
						return GuessingGame3;
				}
				return super.getLocationAssignment(attemptID);
			}

			@Override
			public Integer getSubmittedRow(String attemptID) {
				switch (attemptID) {
					// Accidentally submitted GG1 again for GG2, but did export GG2
					case "4c9d2e1c-7cac-4b66-9784-231a62e36c0d": return 28092;
					// Did GG2 work under GG1 but then made insignificant edit under GG2
					case "5efff483-3746-4a25-bf9d-90d42a585d08": return 38188;
				}
				return super.getSubmittedRow(attemptID);
			};
		};

		public final static Assignment GuessingGame3 = new Assignment(instance,
				"guess3Lab", Assignment.date(2015, 10, 2), false, true, null);

		public final static Assignment[] All = {
			GuessingGame1, GuessingGame2, GuessingGame3
		};

		public final static Assignment[] All_WITH_GRADES_ONLY = {
			LightsCameraAction, PolygonMaker, Squiral,
			GuessingGame1, GuessingGame2, GuessingGame3
		};

		private Fall2015() {
			super(start, dataDir);
		}

		@Override
		public Assignment[] all() {
			return All;
		}


		private static class FakeAssignment extends Assignment {
			FakeAssignment(Dataset dataset, String name, Date end) {
				super(dataset, name, end, false, true, null);
			}

			@Override
			public Map<String, AssignmentAttempt> load(Mode mode, boolean snapshotsOnly,
					boolean addMetadata) {
				HashMap<String, Grade> grades = new SnapParser(this, Mode.Ignore).parseGrades();
				Map<String, AssignmentAttempt> attempts = new TreeMap<>();
				for (Entry<String, Grade> grade : grades.entrySet()) {
					AssignmentAttempt attempt = new AssignmentAttempt(grade.getKey(),
							grade.getValue());
					attempt.exported = !grade.getValue().outlier;
					attempts.put(grade.getKey(), attempt);
				}
				return attempts;
			}
		}
	}