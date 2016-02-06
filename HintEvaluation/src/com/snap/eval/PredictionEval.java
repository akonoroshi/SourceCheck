package com.snap.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.snap.data.Snapshot;
import com.snap.eval.policy.DirectEditPolicy;
import com.snap.eval.policy.HintFactoryPolicy;
import com.snap.eval.policy.HintPolicy;
import com.snap.eval.util.PrintUpdater;
import com.snap.graph.SimpleNodeBuilder;
import com.snap.graph.data.HintFactoryMap;
import com.snap.graph.data.Node;
import com.snap.graph.subtree.SnapSubtree;
import com.snap.graph.subtree.SubtreeBuilder;
import com.snap.parser.Grade;

import distance.RTED_InfoTree_Opt;
import util.LblTree;

public class PredictionEval {

	private final static int SKIP = 1, MAX = 100, LOOK_AHEAD = 5, STEP = 5;
	
	public static void main(String[] args) throws IOException {
		
		String dir = "../data/csc200/fall2015";
		String assignment = "guess1Lab";
		
//		predictionEval(dir, assignment);
		distanceEval(dir, assignment);
	}
	
	public static void predictionEval(String dir, String assignment) throws FileNotFoundException, IOException {
		Snapshot solution = Snapshot.parse(new File(dir + "/solutions/", assignment + ".xml"));
		Node solutionNode = SimpleNodeBuilder.toTree(solution, true);
		DirectEditPolicy solutionPolicy = new DirectEditPolicy(solutionNode);
				
		eval(dir, assignment, "prediction", new ScoreConstructor() {
			@Override
			public Score[] construct(String student, List<Node> nodes, SnapSubtree subtree) {
				SubtreeBuilder builder0 = subtree.buildGraph(student, 0);
				SubtreeBuilder builder1 = subtree.buildGraph(student, 1);
				return new Score[] {
						new BinaryScore("Hint All", new HintFactoryPolicy(builder0)),
						new BinaryScore("Hint Exemplar", new HintFactoryPolicy(builder1)),
						new BinaryScore("Direct Ideal", solutionPolicy),
						new BinaryScore("Direct Student", new DirectEditPolicy(nodes.get(nodes.size() - 1))),
				};
			}

			@Override
			public String[] headers() {
				return BinaryScore.headers();
			}
		});
	}
	
	public static void distanceEval(String dir, String assignment) throws FileNotFoundException, IOException {
		Snapshot solution = Snapshot.parse(new File(dir + "/solutions/", assignment + ".xml"));
		Node solutionNode = SimpleNodeBuilder.toTree(solution, true);
		DirectEditPolicy solutionPolicy = new DirectEditPolicy(solutionNode);
				
		eval(dir, assignment, "distance", new ScoreConstructor() {
			@Override
			public Score[] construct(String student, List<Node> nodes, SnapSubtree subtree) {
				SubtreeBuilder builder0 = subtree.buildGraph(student, 0);
				SubtreeBuilder builder1 = subtree.buildGraph(student, 1);
				return new Score[] {
						new DistanceScore("Hint All", new HintFactoryPolicy(builder0)),
						new DistanceScore("Hint Exemplar", new HintFactoryPolicy(builder1)),
						new DistanceScore("Direct Ideal", solutionPolicy),
						new DistanceScore("Direct Student", new DirectEditPolicy(nodes.get(nodes.size() - 1))),
				};
			}

			@Override
			public String[] headers() {
				return DistanceScore.headers();
			}
		});
	}
	
	private static void eval(String dir, String assignment, String test, ScoreConstructor constructor) throws IOException {
		
		Date maxTime = new GregorianCalendar(2015, 8, 18).getTime();
		SnapSubtree subtree = new SnapSubtree(dir, assignment, maxTime, new HintFactoryMap());
		
		File outFile = new File(dir + "/anlysis/" + assignment + "/" + test + ".csv");
		outFile.getParentFile().mkdirs();
		CSVPrinter printer = new CSVPrinter(new PrintStream(outFile), CSVFormat.DEFAULT.withHeader(constructor.headers()));
				
		HashMap<String,Grade> gradeMap = subtree.gradeMap();
		
		int skip = SKIP;
		int max = MAX;
		
		HashMap<String,List<Node>> nodeMap = subtree.nodeMap();
		for (String student : nodeMap.keySet()) {
			if (skip-- > 0) {
				continue;
			}

			Grade grade = gradeMap.get(student);
			if (grade == null) continue;
			
			if (--max < 0) break;
			
			System.out.println(student);
			
			List<Node> nodes = nodeMap.get(student);
			
			Score[] scores = constructor.construct(student, nodes, subtree);
			
			AtomicInteger count = new AtomicInteger(0);
			int total = 0;
			
			for (int i = 0; i < nodes.size() - 1; i++) {
				Node node = nodes.get(i);
				Node next = nodes.get(i + 1);
				if (node.equals(next)) continue;
				
				for (Score score : scores) {
//					score.update(nodes, i);
					score.updateAsync(nodes, i, count);
					total++;
				}
			}
			
			PrintUpdater updater = new PrintUpdater(40);
			while (count.get() > 0) {
				try {
					Thread.sleep(100);
					updater.update((total - (double)count.get()) / total);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println();

			for (int i = 0; i < scores.length; i++) {
				Score score = scores[i];
				score.writeRow(printer, student, grade.average());
			}
		}
		
		printer.close();
	}
	
	public interface ScoreConstructor {
		String[] headers();
		Score[] construct(String student, List<Node> nodes, SnapSubtree subtree);
	}
	
	protected static class DistanceScore extends Score {

		private double nodeStepDis, nodeFinalDis, hintStepDis, hintFinalDis;
		private double nodeStepDisN, nodeFinalDisN, hintStepDisN, hintFinalDisN;
		private int closerStep, closerFinal, closerStepN, closerFinalN;
		private int totalHints, totalActions;
		
		public DistanceScore(String name, HintPolicy policy) {
			super(name, policy);
		}

		public static String[] headers() {
			return new String[] {
				"policy", "student", "grade", "normalized", "target",
				"nodeDis", "hintDis", "closer", "totalHints", "totalAction"
			};
		}
	
		@Override
		public void writeRow(CSVPrinter printer, String student, double grade) throws IOException {
			printer.printRecord(student, name, grade, "FALSE", "step",
					nodeStepDis, hintStepDis, closerStep,
					totalHints, totalActions);
			printer.printRecord(student, name, grade, "TRUE", "step",
					nodeStepDisN, hintStepDisN, closerStepN,
					totalHints, totalActions);
			printer.printRecord(student, name, grade, "FALSE", "final",
					nodeFinalDis, hintFinalDis, closerFinal,
					totalHints, totalActions);
			printer.printRecord(student, name, grade, "TRUE", "final",
					nodeFinalDisN, hintFinalDisN, closerFinalN,
					totalHints, totalActions);
		}

		@Override
		public void update(List<Node> nodes, int index) {
			Set<Node> hints = policy.nextSteps(nodes.get(index));
			int nHints = hints.size();
			if (nHints == 0) return;
			
			RTED_InfoTree_Opt opt = new RTED_InfoTree_Opt(1, 1, 1);
			
			Node now = nodes.get(index);
			int end = nodes.size() - 1;
			Node fin = nodes.get(end);
			Node step = nodes.get(Math.min(end, index + STEP));
			
			LblTree nowTree = now.toTree();
			LblTree finTree = fin.toTree();
			LblTree stepTree = step.toTree();
			
			int nowSize = now.size();
			
			double nodeStepDis = opt.nonNormalizedTreeDist(nowTree, stepTree);
			double nodeStepDisN = nodeStepDis / nowSize;
			double nodeFinDis = opt.nonNormalizedTreeDist(nowTree, finTree);
			double nodeFinDisN = nodeFinDis / nowSize;
			
			double hintStepDis = 0;
			double hintStepDisN = 0;
			double hintFinDis = 0;
			double hintFinDisN = 0;
			
			int closerStep = 0, closerFinal = 0, closerStepN = 0, closerFinalN = 0;
			
			for (Node hint : hints) {
				LblTree hintTree = hint.toTree();
				
				double hintStepDis1 = opt.nonNormalizedTreeDist(hintTree, stepTree);
				double hintFinDis1 = opt.nonNormalizedTreeDist(hintTree, finTree);
				
				hintStepDis += hintStepDis1;
				hintStepDisN += hintStepDis1 / nowSize;
				hintFinDis += hintFinDis1;
				hintFinDisN += hintFinDis1 / nowSize;
				
				if (hintStepDis < nodeStepDis) closerStep++;
				if (hintStepDisN < nodeStepDisN) closerStepN++;
				if (hintFinDis < nodeFinDis) closerFinal++;
				if (hintFinDisN < nodeFinDisN) closerFinalN++;
			}
			
			synchronized (this) {
				this.nodeStepDis += nodeStepDis;
				this.nodeStepDisN += nodeStepDisN;
				this.nodeFinalDis += nodeFinDis;
				this.nodeFinalDisN += nodeFinDisN;
				
				this.hintStepDis += hintStepDis / nHints;
				this.hintStepDisN += hintStepDisN / nHints;
				this.hintFinalDis += hintFinDis / nHints;
				this.hintFinalDisN += hintFinDisN / nHints;
				
				this.closerStep += closerStep;
				this.closerStepN += closerStepN;
				this.closerFinal += closerFinal;
				this.closerFinalN += closerFinalN;
				
				this.totalActions++;
				this.totalHints += nHints;
			}
		}		
	}
	
	protected static class BinaryScore extends Score {

		public BinaryScore(String name, HintPolicy policy) {
			super(name, policy);
		}

		private int predicted;
		private int totalHints;
		private int totalActions;
		
		public static String[] headers() {
			return new String[] {
				"policy", "student", "grade", "predicted", "hints", "actions"	
			};
		}
		
		public void writeRow(CSVPrinter printer, String student, double grade) throws IOException {
			printer.printRecord(name, student, grade, predicted, totalHints, totalActions);
			printer.flush();
		}
		
		@Override
		public void update(List<Node> nodes, int index) {
			Set<Node> steps = policy.nextSteps(nodes.get(index));
			
			int pred = 0;
			int hints = steps.size();
			
			int end = Math.min(index + LOOK_AHEAD + 1, nodes.size());
			for (int i = index + 1; i < end; i++) {
				Node node = nodes.get(i);
				if (steps.contains(node)) {
					steps.remove(node);
					pred++;
					break;
				}
			}
			
			synchronized (this) {
				totalHints += hints;
				totalActions++;
				predicted += pred;
			}
		}
	}
	
	protected static abstract class Score {
		public final HintPolicy policy;
		public final String name;
		
		public Score(String name, HintPolicy policy) {
			this.name = name;
			this.policy = policy;
		}
		
		public abstract void update(List<Node> nodes, int index);
		public abstract void writeRow(CSVPrinter printer, String student, double grade) throws IOException;
		
		public void updateAsync(final List<Node> nodes, final int index, AtomicInteger count) {
			count.incrementAndGet();
			new Thread(new Runnable() {
				@Override
				public void run() {
					update(nodes, index);
					count.decrementAndGet();
				}
			}).start();
		}
		
		
	}
}
