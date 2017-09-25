package edu.isnap.eval.agreement;

import static edu.isnap.dataset.AttemptAction.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.isnap.ctd.graph.Node;
import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.AssignmentAttempt;
import edu.isnap.dataset.AttemptAction;
import edu.isnap.datasets.Fall2016;
import edu.isnap.datasets.Spring2017;
import edu.isnap.hint.util.SimpleNodeBuilder;
import edu.isnap.parser.SnapParser;
import edu.isnap.parser.Store.Mode;

public class HintSelection {

	private final static double START = 0.2, CUTOFF = 0.6;

	public static void main(String[] args) {
//		printFall2016();
		printSpring2017();
	}

	protected static void printFall2016() {
		Assignment[] assignments = {
				Fall2016.PolygonMaker,
				Fall2016.Squiral,
				Fall2016.GuessingGame1,
				Fall2016.GuessingGame2,
		};
		for (Assignment assignment : assignments) {
			select(assignment, "hints", new SnapParser.Filter[] {
					new SnapParser.SubmittedOnly(),
			}, "twprice", "rzhi");
		}
	}

	protected static void printSpring2017() {
		Assignment[] assignments = {
				Spring2017.PolygonMaker,
				Spring2017.Squiral,
				Spring2017.GuessingGame1,
				Spring2017.GuessingGame2,
		};
		for (Assignment assignment : assignments) {
			select(assignment, "handmade_hints", new SnapParser.Filter[] {
					new SnapParser.SubmittedOnly(),
					new SnapParser.StartedAfter(Assignment.date(2017, 1, 29))
			}, "twprice", "rzhi");
		}
	}

	protected static void printFall2017() {
		Assignment[] assignments = {
				Spring2017.Squiral,
				Spring2017.GuessingGame1
		};
		for (Assignment assignment : assignments) {
			select(assignment, "handmade_hints", new SnapParser.Filter[] {
					new SnapParser.LikelySubmittedOnly(),
					new SnapParser.StartedAfter(Assignment.date(2017, 1, 29))
			}, "twprice", "rzhi");
		}
	}

	public static boolean isHintRow(AttemptAction action) {
		return SHOW_HINT_MESSAGES.contains(action.message) ||
				HIGHLIGHT_CHECK_WORK.equals(action.message) ||
				(HIGHLIGHT_TOGGLE_INSERT.equals(action.message) &&
						action.data.equals("true"));
	}


	public static void select(Assignment assignment, String table,
		SnapParser.Filter[] filters, String... users) {


		Random rand = new Random(assignment.name.hashCode() + 1234);

		Map<String, AssignmentAttempt> attempts = assignment.load(Mode.Use, false, true, filters);

		int studentCount = 0;
		for (AssignmentAttempt attempt : attempts.values()) {
			List<Integer> earlyHints = new ArrayList<>();
			List<Integer> lateHints = new ArrayList<>();
			HashSet<Node> added = new HashSet<>();
			long lastAddedTime = -1;
			for (AttemptAction action : attempt) {
				double percTime = (double) action.currentActiveTime / attempt.totalActiveTime;
				if (!isHintRow(action)) continue;
				if (percTime < START) continue;

				// Skip hints within 30 seconds of the last one we added
				if (lastAddedTime != -1 && action.timestamp.getTime() - lastAddedTime < 1000 * 30) {
					continue;
				}

				// Skip hints for code for which we've already added a hint
				Node node = SimpleNodeBuilder.toTree(action.lastSnapshot, true);
				if (!added.add(node)) {
					continue;
				}

				lastAddedTime = action.timestamp.getTime();
				if (percTime < CUTOFF) earlyHints.add(action.id);
				else lateHints.add(action.id);
			}

			if (earlyHints.size() + lateHints.size() == 0) continue;
			studentCount++;

			// Sample one from early if possible, and late if not
			if (!sample(table, earlyHints, users, rand)) sample(table, lateHints, users, rand);
			// Sample one from late if possible, and early if not
			if (!sample(table, lateHints, users, rand)) sample(table, earlyHints, users, rand);
		}
		System.out.printf("-- Finished %s: %d students\n", assignment.name, studentCount);
	}

	private static boolean sample(String table, List<Integer> list, String[] users, Random rand) {
		if (list.size() == 0) return false;

		int index = rand.nextInt(list.size());
		int id = list.remove(index);

		for (String user : users) {
			System.out.printf(
					"INSERT INTO `%s` (`userID`, `rowID`) VALUES ('%s', %d);\n",
					table, user, id);
		}

		return true;
	}
}
