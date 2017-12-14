package edu.isnap.rating;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONObject;

import edu.isnap.ctd.graph.ASTNode;

public class HintOutcome implements Comparable<HintOutcome> {

	public final ASTNode outcome;
	public final int snapshotID;

	private final double weight;

	public double weight() {
		return weight;
	}

	public HintOutcome(ASTNode outcome, int snapshotID, double weight) {
		this.outcome = outcome;
		this.snapshotID = snapshotID;
		this.weight = weight;
		if (weight <= 0 || Double.isNaN(weight)) {
			throw new IllegalArgumentException("All weights must be positive: " + weight);
		}
	}

	@Override
	public int compareTo(HintOutcome o) {
		return Double.compare(weight(), o.weight());
	}

	public static HintOutcome parse(File file) throws IOException {
		String contents = new String(Files.readAllBytes(file.toPath()));
		JSONObject json = new JSONObject(contents);
		ASTNode root = ASTNode.parse(json);
		String name = file.getName().replace(".json", "");
		int snapshotID;
		try {
			int underscoreIndex = name.indexOf("_");
			snapshotID = Integer.parseInt(name.substring(0, underscoreIndex));
		} catch (Exception e) {
			throw new RuntimeException("Invalid outcome file name: " + file.getName());
		}
		if (json.has("error")) {
			double error = json.getDouble("error");
			return new HintWithError(root, snapshotID, error);
		}
		double weight = 1;
		if (json.has("weight")) {
			weight = json.getDouble("weight");
		}
		return new HintOutcome(root, snapshotID, weight);
	}

	public static class HintWithError extends HintOutcome {

		public final double error;

		private double calculatedWeight = -1;

		@Override
		public double weight() {
			if (calculatedWeight == -1) {
				throw new RuntimeException("Must calculate weight before accessing it.");
			}
			return calculatedWeight;
		}

		public HintWithError(ASTNode outcome, int snapshotID, double error) {
			super(outcome, snapshotID, 1);
			this.error = error;
		}

		public void calculateWeight(double minError, double beta) {
			calculatedWeight = Math.exp(-beta * (error - minError));
			if (Double.isNaN(calculatedWeight)) {
				throw new RuntimeException(String.format(
						"Weight is Nan: e=%.05f; beta=%.05f; e_min=%.05f",
						error, beta, minError));
			}
		}
	}
}