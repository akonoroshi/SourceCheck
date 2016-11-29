package edu.isnap.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONObject;

import com.esotericsoftware.kryo.Kryo;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.AssignmentAttempt;
import edu.isnap.dataset.AssignmentAttempt.ActionRows;
import edu.isnap.dataset.AttemptAction;
import edu.isnap.dataset.Grade;
import edu.isnap.parser.ParseSubmitted.Submission;
import edu.isnap.parser.Store.Mode;
import edu.isnap.parser.elements.BlockDefinitionGroup.BlockIndex;
import edu.isnap.parser.elements.Snapshot;

/**
 * Parser for iSnap logs.
 */
public class SnapParser {

	/**
	 * Removes all cached files at the given path.
	 * @param path
	 */
	public static void clean(String path) {
		for (String file : new File(path).list()) {
			File f = new File(path, file);
			if (f.isDirectory()) clean(f.getAbsolutePath());
			else if (f.getName().endsWith(".cached")) f.delete();
		}
	}

	private final Assignment assignment;
	private final Mode storeMode;

	/**
	 * Constructor for a SnapParser. This should rarely be called directly. Instead, use
	 * {@link Assignment#load()}.
	 *
	 * @param assignment The assignment to load
	 * @param cacheUse The cache {@link Mode} to use when loading data.
	 */
	public SnapParser(Assignment assignment, Mode cacheUse){
		this.assignment = assignment;
		this.storeMode = cacheUse;
		new File(assignment.dataDir).mkdirs();
	}

	public AssignmentAttempt parseSubmission(String id, boolean snapshotsOnly) throws IOException {
		return parseRows(new File(assignment.parsedDir(), id + ".csv"),
				null, null, false, null, null, snapshotsOnly, true);
	}

	private ActionRows parseActions(final File logFile) {
		final String attemptID = logFile.getName().replace(".csv", "");
		String cachePath = logFile.getAbsolutePath().replace(".csv", ".cached");
		return Store.getCachedObject(new Kryo(), cachePath, ActionRows.class, storeMode,
				new Store.Loader<ActionRows>() {
			@Override
			public ActionRows load() {
				ActionRows solution = new ActionRows();

				try {
					CSVParser parser = new CSVParser(new FileReader(logFile),
							CSVFormat.EXCEL.withHeader());

					DateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
					BlockIndex editingIndex = null;
					Snapshot lastSnaphot = null;

					for (CSVRecord record : parser) {
						String timestampString = record.get(1);
						Date timestamp = null;
						try {
							timestamp = format.parse(timestampString);
						} catch (ParseException e) {
							e.printStackTrace();
						}

						String action = record.get(2);
						String data = record.get(3);
						String session = record.get(6);
						String xml = record.get(8);
						String idS = record.get(0);
						int id = -1;
						try {
							id = Integer.parseInt(idS);
						} catch (NumberFormatException e) { }

						AttemptAction row = new AttemptAction(id, attemptID, timestamp, session,
								action, data, xml);
						if (row.snapshot != null) {
							lastSnaphot = row.snapshot;
						}

						if (AttemptAction.BLOCK_EDITOR_START.equals(action)) {
							JSONObject json = new JSONObject(data);
							String name = json.getString("spec");
							String type = json.getString("type");
							String category = json.getString("category");
							editingIndex = lastSnaphot.getEditingIndex(name, type, category);
							if (editingIndex == null) {
								System.err.println("Edit index not found");
							}
						} else if (AttemptAction.BLOCK_EDITOR_OK.equals(action)) {
							editingIndex = null;
						}
						if (row.snapshot != null) {
							if (row.snapshot.editing == null) editingIndex = null;
							else if (row.snapshot.editing.guid == null) {
								row.snapshot.setEditingIndex(editingIndex);
							}
						}

						solution.add(row);

					}
					parser.close();
					System.out.println("Parsed: " + logFile.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}

				Collections.sort(solution);

				return solution;
			}
		});
	}

	private AssignmentAttempt parseRows(File logFile, Grade grade, Integer startID,
			boolean knownSubmissions, Integer submittedActionID, Integer prequelEndID,
			boolean snapshotsOnly, boolean addMetadata)
					throws IOException {
		String attemptID = logFile.getName().replace(".csv", "");

		ActionRows actions = parseActions(logFile);

		Date minDate = assignment.start, maxDate = assignment.end;

		AssignmentAttempt attempt = new AssignmentAttempt(attemptID, grade);
		attempt.submittedActionID = knownSubmissions ?
				AssignmentAttempt.NOT_SUBMITTED : AssignmentAttempt.UNKNOWN;
		List<AttemptAction> currentWork = new ArrayList<>();

		int gradedRow = grade == null ? -1 : grade.gradedRow;
		boolean foundGraded = false;
		Snapshot lastSnaphot = null;

		for (int i = 0; i < actions.size(); i++) {
			AttemptAction action = actions.get(i);

			// Ignore actions outside of our time range
			if (addMetadata && action.timestamp != null && (
					(minDate != null && action.timestamp.before(minDate)) ||
					// Only check max date if we don't have a final submission ID (late work is
					// ok if we can verify it was submitted)
					(submittedActionID == null &&
					maxDate != null && action.timestamp.after(maxDate)))) {
				continue;
			}
			// If we're using log data from a prequel assignment, ignore rows before the prequel was
			// submitted
			if (prequelEndID != null && action.id <= prequelEndID) continue;
			// If we have a start ID, ignore rows that come before it
			if (startID != null && action.id < startID) continue;

			// If we're only concerned with snapshots, and this was a Block.grabbed action,
			// we skip it if the next action (presumably a Block.snapped) produces a snapshot
			// as well. This smooths out some of the quick delete/insert pairs into "moved" events.
			if (snapshotsOnly && action.snapshot != null &&
					AttemptAction.BLOCK_GRABBED.equals(action.message)) {
				if (i + 1 < actions.size() && actions.get(i + 1).snapshot != null) {
					continue;
				}

			}

			if (action.snapshot != null) lastSnaphot = action.snapshot;
			action.lastSnapshot = lastSnaphot;

			// Add this row unless it has not snapshot and we want snapshots only
			boolean addRow = !(snapshotsOnly && action.snapshot == null);

			if (addMetadata) {
				if (addRow) {
					currentWork.add(action);
				}

				// The graded ID is ideally the submitted ID, so this should be redundant
				if (action.id == gradedRow) {
					foundGraded = true;
				}

				boolean done = false;
				boolean saveWork = false;

				// If this is an export keep the work we've seen so far
				if (AttemptAction.IDE_EXPORT_PROJECT.equals(action.message)) {
					attempt.exported = true;
					saveWork = true;
					done |= foundGraded;
				}

				// If this is the submitted action, store that information and finish
				if (submittedActionID != null && action.id == submittedActionID) {
					attempt.submittedSnapshot = lastSnaphot;
					attempt.submittedActionID = action.id;
					done = true;
				}

				if (done || saveWork) {
					// Add the work we've seen so far to the attempt;
					attempt.rows.addAll(currentWork);
					currentWork.clear();
				}
				if (done) {
					break;
				}
			} else if (addRow){
				attempt.rows.add(action);
			}
		}

		if (addMetadata) {
			if (gradedRow >= 0 && !foundGraded) {
				System.err.println("No grade row for: " + logFile.getName());
			}

			// If the solution was exported and submitted, but the log data does not contain
			// the submitted snapshot, check to see what's wrong manually
			if (submittedActionID != null && attempt.submittedSnapshot == null) {
				if (attempt.size() > 0) System.out.println(attemptID + ": " + attempt.rows.getLast().id);
				else System.out.println(attemptID + ": 0 / " + actions.size() + " / " + prequelEndID);
				System.err.printf("Submitted id not found for %s: %s\n",
						attemptID, submittedActionID);
			}
		}

		return attempt;
	}

	/**
	 * Parses the attempts for a given assignment and returns them as a map of id-attempt pairs.
	 * @param snapshotsOnly Whether to include only {@link AttemptAction}s with a snapshot, or all
	 * actions.
	 * @param addMetadata Whether to add metadata, such as start and end dates, grades, submitted
	 * IDs, etc., and additionally filter on this data (e.g. include only actions within the date
	 * range).
	 *
	 * @return
	 */
	public Map<String, AssignmentAttempt> parseAssignment(boolean snapshotsOnly,
			boolean addMetadata) {
		Map<String, AssignmentAttempt> attempts = new TreeMap<>();


		HashMap<String, Grade> grades = new HashMap<>();
		HashMap<String, Integer> startIDs = new HashMap<>();
		Map<String, Submission> submissions = null;

		File assignmentDir = new File(assignment.parsedDir());
		if (!assignmentDir.exists()) {
			throw new RuntimeException("Assignment has not been added and parsed: " + assignment);
		}

		Map<String, String> attemptFiles = new TreeMap<>();
		for (File file : assignmentDir.listFiles()) {
			if (!file.getName().endsWith(".csv")) continue;
			String attemptID = file.getName().replace(".csv", "");
			String path = assignment.getLocationAssignment(attemptID).parsedDir() + "/" +
					attemptID + ".csv";
			attemptFiles.put(attemptID, path);
		}

		Map<String, Integer> prequelEndRows = new HashMap<>();
		if (addMetadata) {
			grades = parseGrades();
			startIDs = parseStartIDs();
			Map<String, Map<String, Submission>> allSubmissions =
					ParseSubmitted.getAllSubmissions(assignment.dataset);
			submissions = allSubmissions.get(assignment.name);

			if (submissions != null) {
				for (String attemptID : submissions.keySet()) {
					Submission submission = submissions.get(attemptID);
					if (submission.location == null) continue;

					// Loop through all earlier assignments and see if any have the same submission ID.
					for (Assignment prequel : assignment.dataset.all()) {
						if (prequel == assignment) break;
						Map<String, Submission> prequelSubmissions = allSubmissions.get(prequel.name);
						if (prequelSubmissions.containsKey(attemptID)) {
							// If so, and there's a submission row, we put (or update) that as the
							// prequel row. It makes no sense to process data that was logged before the
							// previous assignment was submitted.
							Integer submittedRowID = prequelSubmissions.get(attemptID).submittedRowID;
							if (submittedRowID != null) {
								prequelEndRows.put(attemptID, submittedRowID);
							}
						}
					}

					// Update the log path with the one specified in the submission file
					String path = assignment.dataDir + "/parsed/" + submission.location + "/" +
							attemptID + ".csv";
					attemptFiles.put(attemptID, path);
				}
			}
		}

		final AtomicInteger threads = new AtomicInteger();
		for (String attemptID : attemptFiles.keySet()) {
			File file = new File(attemptFiles.get(attemptID));
			if (!file.exists()) {
				throw new RuntimeException("Missing submission data: " + file.getPath());
			}
			Integer prequelEndRow = prequelEndRows.get(attemptID);
			boolean knownSubmissions = submissions != null;
			Submission submission = knownSubmissions ? submissions.get(attemptID) : null;
			Integer submittedRowID = submission != null ? submission.submittedRowID : null;
			parseCSV(file, snapshotsOnly, addMetadata, attempts, knownSubmissions, submittedRowID,
					grades.get(attemptID), startIDs.get(attemptID), prequelEndRow, threads);
		}
		waitForThreads(threads);
		return attempts;
	}

	private void waitForThreads(final AtomicInteger threads) {
		while (threads.get() != 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void parseCSV(final File file, final boolean snapshotsOnly, final boolean addMetadata,
			final Map<String, AssignmentAttempt> attempts, final boolean knownSubmissions,
			final Integer submittedRowID, final Grade grade, final Integer startID,
			final Integer prequelEndRow, final AtomicInteger threads) {
		final String guid = file.getName().replace(".csv", "");
		if (assignment.ignore(guid)) return;

		threads.incrementAndGet();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (grade == null || !grade.outlier) {
						AssignmentAttempt attempt = parseRows(file, grade, startID,
								knownSubmissions, submittedRowID, prequelEndRow, snapshotsOnly,
								addMetadata);
						if (attempt.size() > 3) {
							synchronized (attempts) {
								attempts.put(guid, attempt);
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				threads.decrementAndGet();
			}
		// TODO: Limit threads
		}).start();
	}

	public HashMap<String, Integer> parseStartIDs() {
		HashMap<String, Integer> starts = new HashMap<>();

		File file = new File(assignment.dataset.startsFile());
		if (!file.exists()) {
			return starts;
		}

		try {
			CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.DEFAULT.withHeader());

			for (CSVRecord record : parser) {
				String assignmentID = record.get("assignment");
				if (!assignmentID.equals(assignment.name)) continue;

				String attemptID = record.get("id");
				int codeStart = Integer.parseInt(record.get("code"));
				starts.put(attemptID, codeStart);
			}

			parser.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return starts;
	}

	public HashMap<String, Grade> parseGrades() {
		HashMap<String, Grade> grades = new HashMap<>();

		File file = new File(assignment.gradesFile());
		if (!file.exists()) {
			if (assignment.graded) System.err.println("No grades file for: " + assignment);
			return grades;
		}

		try {
			CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.DEFAULT.withHeader());
			Map<String, Integer> headerMap = parser.getHeaderMap();
			String[] header = new String[headerMap.size()];
			for (Entry<String, Integer> entry : headerMap.entrySet()) {
				header[entry.getValue()] = entry.getKey();
			}
			for (CSVRecord record : parser) {
				if (record.get(0).isEmpty()) break;
				Grade grade = new Grade(record, header);
				grades.put(grade.id, grade);
			}
			parser.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return grades;
	}
}




