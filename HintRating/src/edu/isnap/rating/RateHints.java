package edu.isnap.rating;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import edu.isnap.ctd.graph.ASTNode;
import edu.isnap.ctd.util.map.ListMap;
import edu.isnap.rating.EditExtractor.Edit;
import edu.isnap.rating.EditExtractor.NodeReference;
import edu.isnap.rating.TutorHint.Priority;
import edu.isnap.rating.TutorHint.Validity;

public class RateHints {

	public final static String GS_SPREADSHEET = "gold-standard.csv";
	public final static String ALGORITHMS_DIR = "algorithms";
	public final static String TRAINING_DIR = "training";

	public final static String DATA_ROOT_DIR = "../data/hint-rating/";
	public final static String ISNAP_DATA_DIR = DATA_ROOT_DIR + "isnap2017/";
	public final static String ITAP_DATA_DIR = DATA_ROOT_DIR + "itap2016/";

	public static void rateDir(String path, RatingConfig config)
			throws FileNotFoundException, IOException {
		GoldStandard standard = GoldStandard.parseSpreadsheet(path + GS_SPREADSHEET);
		File algorithmsFolder = new File(path, ALGORITHMS_DIR);
		if (!algorithmsFolder.exists() || !algorithmsFolder.isDirectory()) {
			throw new RuntimeException("Missing algorithms folder");
		}
		for (File algorithmFolder : algorithmsFolder.listFiles(file -> file.isDirectory())) {
			HintSet hintSet = HintSet.fromFolder(algorithmFolder.getName(), config,
					algorithmFolder.getPath());
			System.out.println(hintSet.name);
			rate(standard, hintSet);
		}
	}

	public static void rate(GoldStandard standard, HintSet hintSet) {
		EditExtractor extractor = new EditExtractor(hintSet.config.areNodeIDsConsistent());
		for (String assignment : standard.getAssignmentIDs()) {
			System.out.println("----- " + assignment + " -----");

			ListMap<String, HintRating> ratings = new ListMap<>();
			double totalWeightedPriority = 0;
			double[] totalWeightedValidity = new double[3];
			for (String requestID : standard.getRequestIDs(assignment)) {
				List<TutorHint> validEdits = standard.getValidEdits(assignment, requestID);
				if (validEdits.size() == 0) continue;
				List<HintOutcome> hints = hintSet.getOutcomes(requestID);

//				if (snapshotID == 145234) {
//					validEdits.forEach(e -> System.out.println( e.to.prettyPrint(true)));
//					System.out.println("!");
//				}

				// TODO: do both this and not this
//				double maxWeight = hints.stream().mapToDouble(h -> h.weight).max().orElse(0);
//				hints = hints.stream().
//						filter(h -> h.weight == maxWeight).
//						collect(Collectors.toList());

				if (hints == null || hints.size() == 0) continue;

				double[] weightedValidity = new double[3];

				for (HintOutcome hint : hints) {
//					if (hints.size() > 8 && hint == hints.get(8)) {
//						System.out.println("!!");
//					}
					HintRating rating = new HintRating(hint);
					TutorHint exactMatch = findMatchingEdit(validEdits, hint, hintSet.config,
							extractor);

					rating.validity = Validity.NoTutors;
					if (exactMatch != null) {
						rating.validity = exactMatch.validity;
						rating.priority = exactMatch.priority;
						for (int i = 0; i < exactMatch.validity.value; i++) {
							weightedValidity[i] += hint.weight();
						}
					}
//					System.out.println(rating);
					ratings.add(requestID, rating);
				}
				List<HintRating> snapshotRatings = ratings.get(requestID);
				double weight = snapshotRatings.stream().mapToDouble(r -> r.hint.weight()).sum();
				// TODO: figure out whether to count 0-priority items
				double priority = snapshotRatings.stream()
						.filter(r -> r.priority != null)
						.mapToDouble(r -> r.hint.weight() * r.priority.points())
						.sum() / weight;

				for (int i = 0; i < weightedValidity.length; i++) {
					weightedValidity[i] /= weight;
					totalWeightedValidity[i] += weightedValidity[i];
				}
				totalWeightedPriority += priority;

				System.out.printf("%s: [%.03f, %.03f, %.03f]v / %.03fp\n", requestID,
						weightedValidity[0], weightedValidity[1], weightedValidity[2],
						priority);
			}

			int nSnapshots = ratings.size();
			for (int i = 0; i < totalWeightedValidity.length; i++) {
				totalWeightedValidity[i] /= nSnapshots;
			}
			System.out.printf("TOTAL: [%.03f, %.03f, %.03f]v / %.03fp\n",
					totalWeightedValidity[0], totalWeightedValidity[1], totalWeightedValidity[2],
					totalWeightedPriority / nSnapshots);
		}
	}

	private static ASTNode pruneImmediateChildren(ASTNode node, Predicate<String> condition) {
		for (int i = 0; i < node.children().size(); i++) {
			ASTNode child = node.children().get(i);
			if (condition.test(child.type) && child.children().isEmpty()) {
				node.removeChild(i--);
			}
		}
		return node;
	}

	/**
	 * Prunes nodes from the given "to" AST based on the settings in the config.
	 * Note: For efficiency this modifies with given node - it does not return a copy.
	 */
	public static void pruneNewNodesTo(ASTNode from, ASTNode to, RatingConfig config) {
		// Create a list of nodes before iteration, since we'll be modifying children
		List<ASTNode> toNodes = new ArrayList<>();
		to.recurse(node -> toNodes.add(node));
		// Reverse the order so children are pruned before parents
		Collections.reverse(toNodes);

		// Remove nodes that should be pruned if childless
		for (ASTNode node : toNodes) {
			if (node.parent() == null) continue;
			if (node.children().size() == 0 && config.trimIfChildless(node.type())) {
				node.parent().removeChild(node.index());
			}
		}

		// If node IDs are consistent, we can identify new nodes and prune their children.
		// Note that we could theoretically do this even if they aren't, using the EditExtractor's
		// TED algorithm, but currently only Snap needs this feature, and it has consistent IDs.
		if (config.areNodeIDsConsistent()) {
			// Get a list of node reference in the original AST, so we can see which have been added
			Set<NodeReference> fromRefs = new HashSet<>();
			from.recurse(node -> fromRefs.add(EditExtractor.getReference(node)));

			for (ASTNode node : toNodes) {
				if (node.parent() == null) continue;

				// If this node was created in the hint, prune its immediate children for nodes
				// that are added automatically, according to the config, e.g. literal nodes in Snap
				NodeReference toRef = EditExtractor.getReference(node);
				if (!fromRefs.contains(toRef)) {
					pruneImmediateChildren(node, config::trimIfParentIsAdded);
				}
			}
		}
	}

	public static ASTNode normalizeNewValuesTo(ASTNode from, ASTNode to, RatingConfig config) {
		to = to.copy();

		// Get a set of all the node values used in the original AST. We don't differentiate values
		// by type, since multiple types can share values (e.g. varDecs and vars)
		Set<String> usedValues = new HashSet<>();
		from.recurse(node -> usedValues.add(node.value));

		// Create a list of nodes before iteration, since we'll be modifying children
		List<ASTNode> toNodes = new ArrayList<>();
		to.recurse(node -> toNodes.add(node));

		for (ASTNode node : toNodes) {
			if (node.parent() == null) continue;

			if (node.value != null && !usedValues.contains(node.value)) {
				// If this node's value is new, we may normalize it
				boolean normalize = true;
				// First check if it's a new numeric literal and the config wants to normalize it
				if (config.useSpecificNumericLiterals()) {
					try {
						Double.parseDouble(node.value);
						normalize = false;
					} catch (NumberFormatException e) { }
				}
				if (normalize) {
					// If so, we replace its value with a standard value, so all new values appear
					// the same
					ASTNode parent = node.parent();
					ASTNode replacement = new ASTNode(node.type, "[NEW_VALUE]", node.id);
					int index = node.index();
					parent.removeChild(index);
					parent.addChild(index, replacement);
					for (ASTNode child : node.children()) {
						replacement.addChild(child);
					}
					node.clearChildren();
				}
			}
		}

		return to;
	}

	public static TutorHint findMatchingEdit(List<TutorHint> validHints, HintOutcome outcome,
			RatingConfig config, EditExtractor extractor) {
		if (validHints.isEmpty()) return null;
		ASTNode fromNode = validHints.get(0).from;
		ASTNode outcomeNode = normalizeNewValuesTo(fromNode, outcome.result, config);
		pruneNewNodesTo(fromNode, outcomeNode, config);
		for (TutorHint tutorHint : validHints) {
			ASTNode tutorOutcomeNode = normalizeNewValuesTo(fromNode, tutorHint.to, config);
			pruneNewNodesTo(fromNode, tutorOutcomeNode, config);
			if (outcomeNode.equals(tutorOutcomeNode)) return tutorHint;

			if (outcome.result.equals(tutorHint.to)) {
				System.out.println("Matching hint:");
				System.out.println(ASTNode.diff(tutorHint.from, tutorHint.to, config));
				System.out.println("Difference in normalized nodes:");
				System.out.println(ASTNode.diff(tutorOutcomeNode, outcomeNode, config, 2));
				System.out.println("Tutor normalizing:");
				System.out.println(ASTNode.diff(tutorHint.to, tutorOutcomeNode, config, 2));
				System.out.println("Outcome normalizing:");
				System.out.println(ASTNode.diff(outcome.result, outcomeNode, config, 2));
				throw new RuntimeException("Normalized nodes should be equal if nodes are equal!");
			}
		}

		outcomeNode = normalizeNewValuesTo(fromNode, outcome.result, config);
		Set<Edit> outcomeEdits = extractor.getEdits(fromNode, outcomeNode);
		Set<Edit> bestOverlap = new HashSet<>();
		TutorHint bestHint = null;
		// TODO: sort by validity and priority first
//		Collections.sort(validHints);
		for (TutorHint tutorHint : validHints) {
			ASTNode tutorOutcomeNode = normalizeNewValuesTo(fromNode, tutorHint.to, config);
			// TODO: Figure out what to do if nodes don't have IDs (i.e. Python)
			// TODO: Also figure confirm if this is over-generous with Python
			Set<Edit> tutorEdits = extractor.getEdits(fromNode, tutorOutcomeNode);
			Set<Edit> overlap = new HashSet<>(tutorEdits);
			overlap.retainAll(outcomeEdits);
			if (overlap.size() == outcomeEdits.size() || overlap.size() == tutorEdits.size()) {
				bestOverlap = overlap;
				bestHint = tutorHint;
				break;
			}
		}
		if (bestHint != null) {
//			ASTNode tutorOutcomeNode = normalizeNewValuesTo(fromNode, bestHint.to, config, false);
//			System.out.println("Tutor Hint:");
//			System.out.println(Diff.diff(
//					fromNode.prettyPrint(true, config),
//					tutorOutcomeNode.prettyPrint(true, config)));
//			System.out.println("Alg Hint:");
//			System.out.println(Diff.diff(
//					fromNode.prettyPrint(true, config),
//					outcomeNode.prettyPrint(true, config), 2));
//			Set<Edit> tutorEdits = extractor.getEdits(bestHint.from, bestHint.to);
//			EditExtractor.printEditsComparison(tutorEdits, outcomeEdits, "Tutor Hint", "Alg Hint");
//			if (bestOverlap.size() == tutorEdits.size() &&
//					bestOverlap.size() == outcomeEdits.size() &&
//					bestOverlap.size() > 0) {
//				throw new RuntimeException("Edits should not match if hint outcomes did not!");
//			}
//			System.out.println("-------------------");
			// TODO: Treat this differently
//			return bestHint;
		}
		return null;
	}

	public static class HintRating {
		public final HintOutcome hint;

		public Validity validity;
		public Priority priority;

		public HintRating(HintOutcome hint) {
			this.hint = hint;
		}

		@Override
		public String toString() {
			return validity + " / " + priority + "\n" + hint;
		}
	}
}
