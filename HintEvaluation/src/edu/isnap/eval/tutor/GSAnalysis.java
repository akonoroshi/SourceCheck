package edu.isnap.eval.tutor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Bag;

import costmodel.CostModel;
import distance.APTED;
import edu.isnap.ctd.graph.ASTNode;
import edu.isnap.ctd.graph.ASTSnapshot;
import edu.isnap.ctd.util.map.ListMap;
import edu.isnap.hint.util.Spreadsheet;
import edu.isnap.rating.EditExtractor;
import edu.isnap.rating.EditExtractor.Edit;
import edu.isnap.rating.GoldStandard;
import edu.isnap.rating.RatingConfig;
import edu.isnap.rating.Trace;
import edu.isnap.rating.TrainingDataset;
import edu.isnap.rating.TutorHint;
import node.Node;

public class GSAnalysis {

	public static void writeAnalysis(String path, GoldStandard goldStandard,
			TrainingDataset training, RatingConfig config)
			throws FileNotFoundException, IOException {
		EditExtractor extractor = new EditExtractor(config, ASTNode.EMPTY_TYPE);
		FromStats fromStats = new FromStats(extractor, training);

		goldStandard.createHintsSpreadsheet((hint, spreadsheet) -> {
			spreadsheet.put("requestTreeSize", hint.from.treeSize());
			Bag<Edit> edits = extractor.getEdits(hint.from, hint.to);
			EditExtractor.addEditInfo(spreadsheet, edits);
			fromStats.addToSpreadsheet(hint, spreadsheet);
		}).write(path);
	}

	private static CostModel<ASTNode> costModel = new CostModel<ASTNode>() {
		@Override
		public float ren(Node<ASTNode> nodeA, Node<ASTNode> nodeB) {
			// If the nodes are equal, there is no cost to "rename"
			if (nodeA.getNodeData().shallowEquals(nodeB.getNodeData(), false)) {
				return 0;
			}
			return 1f;
		}

		@Override
		public float ins(Node<ASTNode> node) {
			return 1f;
		}

		@Override
		public float del(Node<ASTNode> node) {
			return 1f;
		}
	};

	private static class FromStats {

		final APTED<CostModel<ASTNode>, ASTNode> apted = new APTED<>(costModel);
		final ListMap<String, ASTSnapshot> solutionMap = new ListMap<>();
		final Map<ASTSnapshot, Node<ASTNode>> nodeMap = new IdentityHashMap<>();
		final EditExtractor extractor;

		ASTNode lastFrom;
		double minEdits, medEdits, minAPTED, medAPTED;

		public FromStats(EditExtractor extractor, TrainingDataset training) {
			this.extractor = extractor;
			for (String assignmentID : training.getAssignmentIDs()) {
				List<Trace> traces = training.getTraces(assignmentID);
				List<ASTSnapshot> solutions = traces.stream()
						.map(trace -> trace.getFinalSnapshot())
						.collect(Collectors.toList());
				solutionMap.put(assignmentID, solutions);
				solutions.forEach(solution ->
					nodeMap.put(solution, EditExtractor.toNode(solution)));
			}
		}

		private void addToSpreadsheet(TutorHint hint, Spreadsheet spreadsheet) {
			update(hint);
			spreadsheet.put("minEdits", minEdits);
			spreadsheet.put("medEdits", medEdits);
			spreadsheet.put("minAPTED", minAPTED);
			spreadsheet.put("medAPTED", medAPTED);
		}

		private void update(TutorHint hint) {
			if (hint.from.equals(lastFrom)) return;
			lastFrom = hint.from;
			List<ASTSnapshot> solutions = solutionMap.get(hint.assignmentID);
			int[] editCounts = solutions.stream()
				.mapToInt(solution ->
					extractor.extractEditsUsingCodeAlign(hint.from, solution).size())
				.sorted().toArray();
			medEdits = editCounts.length % 2 == 0 ?
					(editCounts[editCounts.length / 2] +
							editCounts[editCounts.length / 2 + 1]) / 2.0 :
						editCounts[editCounts.length / 2];
			minEdits = editCounts[0];

			Node<ASTNode> fromNode = EditExtractor.toNode(hint.from);
			double[] apteds = solutions.stream()
					.map(solution -> nodeMap.get(solution))
					.mapToDouble(solution ->
						apted.computeEditDistance(fromNode, solution))
					.sorted().toArray();
			medAPTED = apteds.length % 2 == 0 ?
					(apteds[apteds.length / 2] + apteds[apteds.length / 2 + 1]) / 2 :
					apteds[apteds.length / 2];
			minAPTED = apteds[0];

			System.out.printf("%.01f, %.01f, %.01f, %.01f\n",
					minEdits, medEdits, minAPTED, medAPTED);
		}
	}
}
