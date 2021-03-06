package edu.isnap.eval.tutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.Dataset;
import edu.isnap.datasets.csc200.CSC200Solutions;
import edu.isnap.datasets.csc200.Fall2016;
import edu.isnap.datasets.csc200.Fall2017;
import edu.isnap.datasets.csc200.Spring2017;
import edu.isnap.eval.python.PythonHintConfig;
import edu.isnap.hint.HintConfig;
import edu.isnap.hint.SnapHintConfig;
import edu.isnap.rating.ColdStart;
import edu.isnap.rating.ColdStart.IHintGenerator;
import edu.isnap.rating.HintRater;
import edu.isnap.rating.HintRater.HintRatingSet;
import edu.isnap.rating.RatingConfig;
import edu.isnap.rating.data.GoldStandard;
import edu.isnap.rating.data.HintRequestDataset;
import edu.isnap.rating.data.HintSet;
import edu.isnap.rating.data.TraceDataset;
import edu.isnap.rating.data.TrainingDataset;
import edu.isnap.rating.data.TutorHint;
import edu.isnap.rating.data.TutorHint.Validity;
import edu.isnap.util.Diff;
import edu.isnap.util.Diff.ColorStyle;
import edu.isnap.util.Spreadsheet;
import edu.isnap.util.map.ListMap;

@SuppressWarnings("unused")
public class RunTutorEdits extends TutorEdits {

	public final static String CONSENSUS_GG_SQ = "consensus-gg-sq.csv";
	public static final String ITAP_DIR = "../data/itap/";
	public static final String ANALYSIS_DIR = "analysis";

	static {
		Diff.colorStyle = ColorStyle.ANSI;
		HintRater.DataRootDir = "../QualityScore/data/";
	}

	private final static Validity targetValidity = Validity.MultipleTutors;

	private enum Source {
		StudentData,
		Template,
		SingleExpert,
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		RatingDataset dataset = iSnapF16F17;
//		dataset = ITAPS16;
		Source source = Source.StudentData;
		HintAlgorithm algorithm = SourceCheck;
		boolean debug = true;
		boolean writeHints = false;

//		writeAllHintSets(iSnapF16F17, ITAPS16);

		dataset.runHintRating(algorithm, source, debug, writeHints);

//		dataset.exportTrainingAndTestData(true);
//		dataset.writeGoldStandard();
//		dataset.analyzeGoldStandard();
//		dataset.writeHintSet(algorithm, source);

		// Leave this on for a while, until you've rewritten it for all local repos
//		dataset.verifyGoldStandard();
//		dataset.printData();

//		dataset.runTutorHintBenchmarks(debug);
//		dataset.writeTutorHintBenchmark();

//		dataset.testKValues(algorithm, source, debug, writeHints, 1, 20);
//		dataset.writeColdStart(algorithm, 200, 1);

		// Tutor consensus hint generation
//		compareHintsSnap(Fall2016.instance, 10000);
//		compareHintsSnap(Spring2017.instance, 10000);
//		compareHintsSnap(Fall2017.instance, 30000);
//		compareHintsPython(ITAP_DIR, 10000);

		// Test with Fall 2017 preliminary tutor hints
//		testFall2017Pelim();

//		dataset.writeAllInAlgorithmsFolder();
	}

	public static void writeAllHintSets(RatingDataset... datasets) throws IOException {
		for (RatingDataset dataset : datasets) {
			System.out.println("Writing " + dataset.getDataDir() + ":");
			for (HintAlgorithm algorithm : dataset.validHintAlgorithms()) {
				System.out.println("\t" + algorithm.getName());
				dataset.writeHintSet(algorithm, Source.StudentData);
			}
			dataset.writeTutorHintBenchmark();
			dataset.writeAllInAlgorithmsFolder();
		}
	}

	public final static RatingDataset iSnapF16F17 = new SnapRatingDataset() {

		private final Dataset[] datasets = new Dataset[] {
				Fall2016.instance, Spring2017.instance, Fall2017.instance
		};

		@Override
		String getDataDir() {
			return HintRater.isnapF16F17Dir();
		}

		@Override
		protected Dataset[] getDatasets() {
			return datasets;
		}
	};

	public final static RatingDataset iSnapF16S17 = new SnapRatingDataset() {

		private final Dataset[] datasets = new Dataset[] {
				Fall2016.instance, Spring2017.instance
		};

		@Override
		String getDataDir() {
			return HintRater.isnapF16S17Dir();
		}

		@Override
		protected Dataset[] getDatasets() {
			return datasets;
		}
	};

	private static abstract class SnapRatingDataset extends RatingDataset {
		protected abstract Dataset[] getDatasets();

		@Override
		GoldStandard generateGoldStandard() throws FileNotFoundException, IOException {
			Dataset[] datasets = getDatasets();
			GoldStandard[] standards = new GoldStandard[datasets.length];
			for (int i = 0; i < datasets.length; i++) {
				standards[i] = readConsensusSnap(datasets[i], CONSENSUS_GG_SQ);
			}
			return GoldStandard.merge(standards);
		}

		@Override
		HintConfig createHintConfig() {
			return new SnapHintConfig();
		}

		@Override
		String getTemplateDir(Source source) {
			String dir = source == Source.Template ? "" : "/single";
			return CSC200Solutions.dataDir + dir;
		}

		@Override
		void addTrainingAndTestData(TraceDataset training, TraceDataset requests)
				throws IOException {
			Dataset[] datasets = getDatasets();
			for (int i = 0; i < datasets.length; i++) {
				Map<String, Assignment> assignmentMap = datasets[i].getAssignmentMap();
				Assignment guessingGame = assignmentMap.get("guess1Lab");
				Assignment squiral = assignmentMap.get("squiralHW");
				buildSnapDatasets(datasets[i], CONSENSUS_GG_SQ, training, requests,
						guessingGame, squiral);
			}
		}

		@Override
		HintAlgorithm[] validHintAlgorithms() {
			return new HintAlgorithm[] {
				SourceCheck,
				CTD,
				PQGram,
			};
		}

		@Override
		Map<String, TutorHintSet> getTutorHintSets() throws IOException {
			Dataset[] datasets = getDatasets();
			ListMap<String, TutorHintSet> hintSets = new ListMap<>();
			for (int i = 0; i < datasets.length; i++) {
				Map<String, TutorHintSet> hintSetMap = readTutorHintSetsSnap(datasets[i]);
				for (String tutor : hintSetMap.keySet()) hintSets.add(tutor, hintSetMap.get(tutor));
			}
			if (!hintSets.values().stream().allMatch(list -> list.size() == datasets.length)) {
				throw new RuntimeException("Not all tutors present in all datasets");
			}
			return hintSets.keySet().stream().collect(Collectors.toMap(
					tutor -> tutor,
					tutor -> TutorHintSet.combine(tutor,
							hintSets.get(tutor).toArray(new TutorHintSet[datasets.length]))));
		}
	}

	public final static RatingDataset ITAPS16 = new RatingDataset() {


		@Override
		GoldStandard generateGoldStandard() throws FileNotFoundException, IOException {
			GoldStandard standard = readConsensusPython(ITAP_DIR, "spring2016");
			return standard;
		}

		@Override
		HintConfig createHintConfig() {
			return new PythonHintConfig();
		}

		@Override
		String getDataDir() {
			return HintRater.itapS16Dir();
		}

		@Override
		String getTemplateDir(Source source) {
			String dir = source == Source.Template ? "" : "single/";
			return ITAP_DIR + dir + "templates";
		}


		@Override
		void addTrainingAndTestData(TraceDataset training, TraceDataset requests)
				throws IOException {
			buildPythonDatasets("../../PythonAST/data", ITAP_DIR, training, requests);
		}

		@Override
		HintAlgorithm[] validHintAlgorithms() {
			return new HintAlgorithm[] {
				SourceCheck,
				CTD,
				PQGram,
				ITAP_History,
			};
		}

		@Override
		Map<String, TutorHintSet> getTutorHintSets() throws IOException {
			return readTutorHintSetsPython(ITAP_DIR, "spring2016");
		}
	};

	public static abstract class RatingDataset {

		protected HintConfig hintConfig = createHintConfig();

		private GoldStandard goldStandard;
		private TrainingDataset trainingDataset;

		abstract GoldStandard generateGoldStandard() throws FileNotFoundException, IOException;
		abstract HintConfig createHintConfig();
		abstract String getDataDir();
		abstract String getTemplateDir(Source source);
		abstract void addTrainingAndTestData(TraceDataset training, TraceDataset requests)
				throws IOException;
		abstract HintAlgorithm[] validHintAlgorithms();
		abstract Map<String, TutorHintSet> getTutorHintSets() throws IOException;

		private String getAnalysisDir() {
			return getDataDir() + ANALYSIS_DIR + "/";
		}

		void exportTrainingAndTestData(boolean toSpreadsheet) throws IOException {
			TraceDataset training = new TraceDataset("training");
			TraceDataset requests = new TraceDataset("requests");
			addTrainingAndTestData(training, requests);
			String dataDir = getDataDir();
			if (toSpreadsheet) {
				training.writeToSpreadsheet(dataDir + HintRater.TRAINING_FILE,
						HintRater.TRAINING_FILE.endsWith(".gz"));
				requests.writeToSpreadsheet(dataDir + HintRater.REQUEST_FILE,
						HintRater.REQUEST_FILE.endsWith(".gz"));
			} else {
				training.writeToFolder(dataDir + "training");
				requests.writeToFolder(dataDir + "requests");
			}
		}

		public GoldStandard getGoldStandard() throws IOException {
			if (goldStandard == null) {
				goldStandard = GoldStandard.parseSpreadsheet(
						getDataDir() + HintRater.GS_SPREADSHEET);
			}
			return goldStandard;
		}

		public HintSet getHintSet(HintAlgorithm algorithm, Source source,
				GoldStandard standard) throws IOException {
			HintSet hintSet;
			if (source == Source.StudentData) {
				hintSet = algorithm.getHintSetFromTrainingDataset(
						hintConfig, getTrainingDataset());
			} else {
				hintSet = algorithm.getHintSetFromTemplate(hintConfig, getTemplateDir(source));
			}
			if (hintSet instanceof HintDataHintSet) {
				((HintDataHintSet) hintSet).addHints(getRequestDataset());
			}
			return hintSet;
		}

		public void runHintRating(HintAlgorithm algorithm, Source source, boolean debug,
				boolean write) throws FileNotFoundException, IOException {
			GoldStandard standard = getGoldStandard();
			HintSet hintSet = getHintSet(algorithm, source, standard);
			HintRatingSet rate = new HintRater(targetValidity, debug).rate(standard, hintSet);
			if (write) {
				String name = getSourceName(algorithm, source) + ".csv";
				rate.writeAllHints(getDataDir() + HintRater.ALGORITHMS_DIR + "/" + name);
				Spreadsheet spreadsheet = new Spreadsheet();
				rate.writeAllRatings(spreadsheet);
				spreadsheet.write(getAnalysisDir() + "ratings-" + name);
			}
		}

		public void testKValues(HintAlgorithm algorithm, Source source, boolean debug,
				boolean write, int minK, int maxK) throws FileNotFoundException, IOException {
			GoldStandard standard = getGoldStandard();
			Spreadsheet spreadsheet = new Spreadsheet();
			for (int k = minK; k <= maxK; k++) {
				System.out.println("------ k = " + k + " ------");
				hintConfig.votingK = k;
				HintSet hintSet = getHintSet(algorithm, source, standard);
				HintRatingSet rate = new HintRater(targetValidity, debug).rate(standard, hintSet);
				if (write) {
					spreadsheet.setHeader("k", k);
					rate.writeAllRatings(spreadsheet);
				}
			}
			if (write) {
				String name = getSourceName(algorithm, source) + ".csv";
				spreadsheet.write(getAnalysisDir() + "k-test-" + name);
			}
		}

		public static String getSourceName(HintAlgorithm algorithm, Source source) {
			String name = algorithm.getName();
			if (source == Source.Template) name += "-template";
			if (source == Source.SingleExpert) name += "-expert1";
			return name;
		}

		public void writeHintSet(HintAlgorithm algorithm, Source source)
				throws FileNotFoundException, IOException {
			GoldStandard standard = getGoldStandard();
			HintSet hintSet = getHintSet(algorithm, source, standard);
			String name = getSourceName(algorithm, source);
			hintSet.writeToFolder(new File(getDataDir() + HintRater.ALGORITHMS_DIR,
					getSourceName(algorithm, source)).getPath(), true);
		}

		public void writeGoldStandard() throws FileNotFoundException, IOException {
			generateGoldStandard().writeSpreadsheet(getDataDir() + HintRater.GS_SPREADSHEET);
		}

		public void analyzeGoldStandard() throws IOException {
			GSAnalysis.writeAnalysis(getAnalysisDir() + "gold-standard-analysis.csv",
					getGoldStandard(), getTrainingDataset(), getRequestDataset(),
					HighlightHintSet.getRatingConfig(hintConfig));
		}

		public void writeColdStart(HintAlgorithm algorithm, int rounds, int step)
				throws IOException {
			ColdStart coldStart = getColdStart(algorithm);
			coldStart.writeTest(String.format("%scold-start-%03d-%d.csv",
					getAnalysisDir(), rounds, step), rounds, step);
		}

		public void writeSingleTraces(HintAlgorithm algorithm)
				throws FileNotFoundException, IOException {
			ColdStart coldStart = getColdStart(algorithm);
			coldStart.testSingleTraces().write(getAnalysisDir() + "traces.csv");
		}

		public void writeCostsSpreadsheet() throws IOException {
			GoldStandard standard = getGoldStandard();
			TrainingDataset dataset = getTrainingDataset();
			HighlightHintGenerator.getCostsSpreadsheet(dataset, standard, hintConfig)
			.write(getAnalysisDir() + "distances.csv");
		}

		protected TrainingDataset getTrainingDataset() throws IOException {
			if (trainingDataset == null) {
				trainingDataset = TrainingDataset.fromSpreadsheet("training",
						getDataDir() + HintRater.TRAINING_FILE);
//				trainingDataset = TrainingDataset.fromDirectory("training",
//					getDataDir() + "training/");
			}
			return trainingDataset;
		}

		protected HintRequestDataset getRequestDataset() throws IOException {
			return HintRequestDataset.fromSpreadsheet("requests",
					getDataDir() + HintRater.REQUEST_FILE);
		}

		private ColdStart getColdStart(HintAlgorithm algorithm)
				throws FileNotFoundException, IOException {
			GoldStandard standard = getGoldStandard();
			TrainingDataset dataset = getTrainingDataset();
			HintRequestDataset requests = getRequestDataset();
			IHintGenerator hintGenerator = algorithm.getHintGenerator(hintConfig);
			ColdStart coldStart = new ColdStart(standard, dataset, requests, hintGenerator,
					targetValidity, HighlightHintSet.getRatingConfig(hintConfig));
			return coldStart;
		}

		private void verifyGoldStandard() throws IOException {
			GoldStandard gs1 = getGoldStandard();
			GoldStandard gs2 = generateGoldStandard();

			for (String assignmentID : gs1.getAssignmentIDs()) {
				for (String requestID : gs1.getRequestIDs(assignmentID)) {
					List<TutorHint> edits1 = gs1.getValidHints(assignmentID, requestID);
					List<TutorHint> edits2 = gs2.getValidHints(assignmentID, requestID);

					for (int i = 0; i < edits1.size(); i++) {
						TutorHint e1 = edits1.get(i);
						TutorHint e2 = edits2.get(i);

						if (!e1.to.equals(e2.to, true, true)) {
							System.out.println("Differ: " + e1.requestID);
							System.out.println(Diff.diff(
									e1.to.prettyPrint(true, RatingConfig.Snap),
									e2.to.prettyPrint(true, RatingConfig.Snap)));
						}
					}
				}
			}
		}

		public void printData() throws IOException {
			RatingConfig config = HighlightHintSet.getRatingConfig(hintConfig);
			getTrainingDataset().print(config);
			getRequestDataset().print(config);
		}

		public void writeAllInAlgorithmsFolder() throws IOException {
			new HintRater(targetValidity, false)
			.rateDir(getDataDir(), HighlightHintSet.getRatingConfig(hintConfig), true);
		}

		public void runTutorHintBenchmarks(boolean debug) throws IOException {
			Map<String, TutorHintSet> tutorHintSets = getTutorHintSets();
			GoldStandard standard = getGoldStandard();
			for (String tutor : tutorHintSets.keySet()) {
				System.out.println("#### " + tutor + " ####");
				new HintRater(targetValidity, debug).rate(standard, tutorHintSets.get(tutor));
			}
			TutorHintSet allTutors = createAllTutorsHintSet(tutorHintSets);
			new HintRater(targetValidity, debug).rate(standard, allTutors);
		}

		public void writeTutorHintBenchmark() throws IOException {
			Map<String, TutorHintSet> tutorHintSets = getTutorHintSets();
			TutorHintSet allTutors = createAllTutorsHintSet(tutorHintSets);
			allTutors.writeToFolder(new File(getDataDir() + HintRater.ALGORITHMS_DIR,
					allTutors.name).getPath(), true);
		}

		private TutorHintSet createAllTutorsHintSet(Map<String, TutorHintSet> tutorHintSets) {
			TutorHintSet[] sets = tutorHintSets.values().stream().toArray(l -> new TutorHintSet[l]);
			TutorHintSet allTutors = TutorHintSet.combine("AllTutors", sets);
			return allTutors;
		}
	}

	private static HintAlgorithm SourceCheck = new HintAlgorithm() {

		@Override
		public HintDataHintSet getHintSetFromTrainingDataset(HintConfig config,
				TrainingDataset dataset) throws IOException {
			return new ImportHighlightHintSet(getName(), config, dataset);
		}

		@Override
		public HintDataHintSet getHintSetFromTemplate(HintConfig config, String directory) {
			return new TemplateHighlightHintSet(getName(), config, directory);
		}

		@Override
		public IHintGenerator getHintGenerator(HintConfig config) {
			return new HighlightHintGenerator(config);
		}

		@Override
		public String getName() {
			return "SourceCheck";
		}
	};

	private static HintAlgorithm CTD = new HintAlgorithm() {

		@Override
		public HintDataHintSet getHintSetFromTrainingDataset(HintConfig config,
				TrainingDataset dataset) throws IOException {
			return new CTDHintSet(getName(), config, dataset);
		}

		@Override
		public HintDataHintSet getHintSetFromTemplate(HintConfig config, String directory) {
			throw new UnsupportedOperationException("CTD does not fully support templates.");
		}

		@Override
		public IHintGenerator getHintGenerator(HintConfig config) {
			// TODO: refactor HighlightHintGenerator to support both algorithms
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			return "CTD";
		}
	};

	private static HintAlgorithm PQGram = new HintAlgorithm() {

		@Override
		public HintDataHintSet getHintSetFromTrainingDataset(HintConfig config,
				TrainingDataset dataset) throws IOException {
			return new PQGramHintSet(getName(), config, dataset);
		}

		@Override
		public HintDataHintSet getHintSetFromTemplate(HintConfig config, String directory) {
			throw new UnsupportedOperationException("PQGram does not support templates.");
		}

		@Override
		public IHintGenerator getHintGenerator(HintConfig config) {
			// TODO: refactor HighlightHintGenerator to support both algorithms
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			return "PQGram";
		}
	};

	private static HintAlgorithm ITAP_History = new HintAlgorithm() {

		@Override
		public HintSet getHintSetFromTrainingDataset(HintConfig config,
				TrainingDataset dataset) throws IOException {
			ListMap<String, PrintableTutorHint> tutorEdits =
					readTutorEditsPython(ITAP_DIR + "handmade_hints_itap_ast.csv", null);
			HintSet hintSet = new HintSet(getName(), HintDataHintSet.getRatingConfig(config));
			for (String assignmentID : tutorEdits.keySet()) {
				for (PrintableTutorHint edit : tutorEdits.get(assignmentID)) {
					hintSet.add(edit.toOutcome());
				}
			}
			return hintSet;
		}

		@Override
		public HintDataHintSet getHintSetFromTemplate(HintConfig config, String directory) {
			throw new UnsupportedOperationException("ITAP does not support templates.");
		}

		@Override
		public IHintGenerator getHintGenerator(HintConfig config) {
			throw new UnsupportedOperationException("ITAP does not a hint generator.");
		}

		@Override
		public String getName() {
			return "ITAP";
		}
	};

	private static interface HintAlgorithm {
		HintSet getHintSetFromTrainingDataset(HintConfig config, TrainingDataset dataset)
				throws IOException;
		HintSet getHintSetFromTemplate(HintConfig config, String directory);
		IHintGenerator getHintGenerator(HintConfig config);
		String getName();
	}

	private static void printTutorInternalRatings(Dataset dataset)
			throws FileNotFoundException, IOException {
		GoldStandard standard = readConsensusSnap(dataset, CONSENSUS_GG_SQ);
		// Compare tutor hints to each other
		Map<String, TutorHintSet> hintSets = readTutorHintSetsSnap(dataset);
		for (HintSet hintSet : hintSets.values()) {
			System.out.println("------------ " + hintSet.name + " --------------");
			new HintRater(targetValidity, false).rate(standard, hintSet);
		}
	}

	private static void testFall2017Pelim() throws FileNotFoundException, IOException {

		ListMap<String,PrintableTutorHint> fall2017 =
				readTutorEditsSnap(Fall2017.instance);
		fall2017.values().forEach(list -> list.forEach(
				hint -> hint.validity = EnumSet.of(Validity.OneTutor)));
//		fall2017.remove("guess1Lab");
		GoldStandard standard = new GoldStandard(fall2017);
		HighlightHintSet hintSet = new TemplateHighlightHintSet(
				"template", CSC200Solutions.instance);
		hintSet.addHints(standard);
		new HintRater(targetValidity, true).rate(standard, hintSet);
//		runConsensus(Fall2016.instance, standard)
//		.writeAllHints(Fall2017.GuessingGame1.exportDir() + "/fall2016-rating.csv");
//		runConsensus(Spring2017.instance, standard);
	}

	protected static void writeHighlight(String dataDirectory, String name, GoldStandard standard,
			HintConfig hintConfig)
			throws FileNotFoundException, IOException {
		TrainingDataset dataset = TrainingDataset.fromSpreadsheet("",
				new File(dataDirectory, HintRater.TRAINING_FILE).getPath());
		HighlightHintSet hintSet = new ImportHighlightHintSet(name, hintConfig, dataset);
		hintSet.addHints(standard);
		hintSet.writeToFolder(new File(
				dataDirectory, HintRater.ALGORITHMS_DIR + File.separator + name).getPath(), true);
	}

	protected static HintRatingSet runConsensus(Dataset trainingDataset, GoldStandard standard)
			throws FileNotFoundException, IOException {
		HighlightHintSet hintSet = new DatasetHighlightHintSet(
			trainingDataset.getName(), new SnapHintConfig(), trainingDataset)
				.addHints(standard);
		return new HintRater(targetValidity, false).rate(standard, hintSet);
//		hintSet.toTutorEdits().forEach(e -> System.out.println(
//				e.toSQLInsert("handmade_hints", "highlight", 20000, false, true)));
	}
}
