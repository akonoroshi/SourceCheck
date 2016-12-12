package edu.isnap.ctd.util;

import java.util.LinkedList;
import java.util.List;

import edu.isnap.ctd.graph.Node;
import edu.isnap.ctd.graph.Node.Action;
import edu.isnap.ctd.hint.HintMap;
import edu.isnap.ctd.util.map.BiMap;
import edu.isnap.ctd.util.map.ListMap;
import edu.isnap.ctd.util.map.MapFactory;

public class NodeAlignment {

	public final Node from, to;

	public final BiMap<Node, Node> mapping = new BiMap<>(MapFactory.IdentityHashMapFactory);

	private int cost = 0;

	public NodeAlignment(Node from, Node to) {
		this.from = from;
		this.to = to;
	}

	public int calculateCost(DistanceMeasure distanceMeasure) {
		ListMap<String, Node> fromMap = getChildMap(from);
		ListMap<String, Node> toMap = getChildMap(to);

		mapping.clear();

		for (String key : fromMap.keySet()) {
			List<Node> fromNodes = fromMap.get(key);
			List<Node> toNodes = toMap.get(key);

			if (toNodes == null) {
				for (Node node : fromNodes) {
					mapping.put(node, null);
				}
				continue;
			}

			align(fromNodes, toNodes, distanceMeasure);
		}
		return cost;
	}

	public interface DistanceMeasure {
		public double measure(String type, String[] a, String[] b);
	}

	public static class ProgressDistanceMeasure implements DistanceMeasure {

		public final int inOrderReward, outOfOrderReward;
		public final double missingCost;
		public final String scriptType;

		public ProgressDistanceMeasure(int inOrderReward, int outOfOrderReward,
				double missingCost, String scriptType) {
			this.inOrderReward = inOrderReward;
			this.outOfOrderReward = outOfOrderReward;
			this.missingCost = missingCost;
			this.scriptType = scriptType;
		}

		@Override
		public double measure(String type, String[] a, String[] b) {
			if (scriptType.equals(type)) {
				return Alignment.getMissingNodeCount(a, b) * missingCost -
						Alignment.getProgress(a, b, inOrderReward, outOfOrderReward);
			} else {
				int cost = -inOrderReward;
				return Alignment.normAlignCost(a, b, cost, cost, cost);
			}
		}
	};

	private void align(List<Node> fromNodes, List<Node> toNodes,
			DistanceMeasure distanceMeasure) {
		String[][] fromStates = stateArray(fromNodes);
		String[][] toStates = stateArray(toNodes);

		double minCost = Integer.MAX_VALUE;
		double[][] costMatrix = new double[fromStates.length][toStates.length];
		for (int i = 0; i < fromStates.length; i++) {
			for (int j = 0; j < toStates.length; j++) {
				double cost = distanceMeasure.measure(fromNodes.get(i).type(),
						fromStates[i], toStates[j]);
				costMatrix[i][j] = cost;
				minCost = Math.min(minCost, cost);
			}
		}

		// Ensure all costs are non-negative
		for (int i = 0; i < fromStates.length; i++) {
			for (int j = 0; j < toStates.length; j++) {
				costMatrix[i][j] = costMatrix[i][j] - minCost;
			}
		}

		HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
		int[] matching = alg.execute();

		for (int i = 0; i < fromStates.length; i++) {
			int j = matching[i];
			if (j == -1) continue;
			cost += costMatrix[i][j] + minCost;
			Node from = fromNodes.get(i), to = toNodes.get(j);
			mapping.put(from, to);

			// Try to align the children of these paired nodes and add them to the mapping,
			// but don't worry about score, since that's accounted for in the progress score
			// These mappings may be overwritten later if the nodes themselves are matched as
			// parents, but this is not a wholly consistent alignment algorithm
			List<int[]> childPairs = Alignment.alignPairs(fromStates[i], toStates[j], 1, 1, 100);
			for (int[] pair : childPairs) {
				if (pair[0] >= 0 && pair[1] >= 0) {
					mapping.put(from.children.get(pair[0]), to.children.get(pair[1]));
				}
			}

		}
	}

	private String[][] stateArray(List<Node> nodes) {
		String[][] states = new String[nodes.size()][];
		for (int i = 0; i < states.length; i++) {
			states[i] = nodes.get(i).getChildArray();
		}
		return states;
	}

	private ListMap<String, Node> getChildMap(Node node) {
		final ListMap<String, Node> map = new ListMap<>();
		node.recurse(new Action() {
			@Override
			public void run(Node node) {
				if (node.children.isEmpty()) return;
				String key = HintMap.toRootPath(node).root().toCanonicalString();
				map.add(key, node);
			}
		});
		return map;
	}

	public static List<Node> findBestMatches(Node from, List<Node> matches,
			DistanceMeasure distanceMeasure) {
		List<Node> best = new LinkedList<>();
		int bestCost = 0;
		for (Node to : matches) {
			NodeAlignment align = new NodeAlignment(from, to);
			int cost = align.calculateCost(distanceMeasure);
			if (cost < bestCost) {
				best.clear();
			}
			if (cost <= bestCost) {
				bestCost = cost;
				best.add(to);
			}
		}
		return best;
	}

	public static Node findBestMatch(Node from, List<Node> matches,
			DistanceMeasure distanceMeasure) {
		Node best = null;
		int smallest = Integer.MAX_VALUE;
		List<Node> bestMatches = findBestMatches(from, matches, distanceMeasure);
		System.out.println("Size: " + bestMatches.size());
		for (Node node : bestMatches) {
			int size = node.size();
			if (size < smallest) {
				best = node;
				smallest = size;
			}
		}
		return best;
	}
}