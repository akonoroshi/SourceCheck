package edu.isnap.datasets.run;

import java.util.HashMap;
import java.util.Scanner;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.Dataset;
import edu.isnap.datasets.BJC2017;
import edu.isnap.datasets.CampHS2018;
import edu.isnap.datasets.Demo;
import edu.isnap.datasets.HelpSeeking;
import edu.isnap.datasets.HelpSeekingExperts;
import edu.isnap.datasets.MTurk2018;
import edu.isnap.datasets.Samples;
import edu.isnap.datasets.TestDataset;
import edu.isnap.datasets.aggregate.CSC200;
import edu.isnap.datasets.csc110.CSC110Fall2019;
import edu.isnap.datasets.csc200.Fall2015;
import edu.isnap.datasets.csc200.Fall2016;
import edu.isnap.datasets.csc200.Fall2017;
import edu.isnap.datasets.csc200.Fall2018;
import edu.isnap.datasets.csc200.Spring2016;
import edu.isnap.datasets.csc200.Spring2017;
import edu.isnap.datasets.csc200.Spring2018;
import edu.isnap.parser.LogSplitter;
import edu.isnap.parser.ParseSubmitted;
import edu.isnap.parser.SnapParser;
import edu.isnap.parser.Store.Mode;
import edu.isnap.unittest.TestRunner;

public class Shell {

	public final static Dataset[] DATASETS = {
			CSC200.instance,
			Fall2015.instance,
			Spring2016.instance,
			Fall2016.instance,
			Spring2017.instance,
			Fall2017.instance,
			Spring2018.instance,
			Fall2018.instance,
			Demo.instance,
			HelpSeekingExperts.instance,
			HelpSeeking.instance,
			Samples.instance,
			BJC2017.instance,
			CampHS2018.instance,
			MTurk2018.instance,
			TestDataset.instance,
			CSC110Fall2019.instance
	};

	public static void main(String[] args) {
		HashMap<String, Dataset> datasets = new HashMap<>();
		for (Dataset dataset : DATASETS) {
			datasets.put(dataset.getClass().getSimpleName().toLowerCase(), dataset);
		}

		Scanner sc = new Scanner(System.in);
		String line;
		while (true) {
			System.out.println("Enter Command:");
			line = sc.nextLine().toLowerCase();
			if (line.isEmpty()) break;
			switch (line) {
			case "cleanall":
				SnapParser.clean("../data/csc200");
				continue;
			}

			String[] parts = line.split("\\.");
			try {
				Dataset dataset = datasets.get(parts[0]);
				if (dataset == null) {
					System.out.println("No dataset: " + parts[0]);
					continue;
				}

				if (parts.length == 2) {
					String command = parts[1];
					switch (command) {
					case "clean":
						SnapParser.clean(dataset.dataDir);
						break;
					case "split":
						System.out.println("Please turn off Windows Defender's real time "
								+ "protection for faster splitting");
						new LogSplitter().splitStudentRecords(dataset);
						break;
					case "test":
						TestRunner.run(dataset.all());
						break;
					case "parse":
						for (Assignment assignment : dataset.all()) {
							assignment.load(Mode.Overwrite, true);
						}
						break;
					case "build":
						for (Assignment assignment : dataset.all()) {
							RunHintBuilder.buildHints(assignment, 1);
							RunCopyData.copyHintDatabaseToServer(dataset.dataDir);
						}
						break;
					default:
						System.out.println("Unknown command: " + command);
					}

				} else if (parts.length == 3) {
					Assignment assignment = null;
					for (Assignment toCheck : dataset.all()) {
						if (toCheck.name.toLowerCase().equals(parts[1])) {
							assignment = toCheck;
							break;
						}
					}
					if (assignment == null) {
						System.out.println("No assignment: " + parts[1]);
						continue;
					}

					String command = parts[2];
					switch(command) {
					case "build":
						RunHintBuilder.buildHints(assignment, 1);
						RunCopyData.copyHintDatabaseToServer(dataset.dataDir);
						break;
					case "clean":
						SnapParser.clean(assignment.parsedDir());
						break;
					case "test":
						TestRunner.run(assignment);
						break;
					case "parse":
						assignment.load(Mode.Overwrite, true);
						break;
					case "parsesubmitted":
						ParseSubmitted.parseSubmitted(assignment);
						break;
					case "print":
						RunPrintSubmitted.print(assignment);
						break;
					default:
						System.out.println("Unknown command: " + command);
					}

				} else {
					System.out.println("Too many parts: " + parts.length);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sc.close();
	}

}
