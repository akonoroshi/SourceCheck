package edu.isnap.eval;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.AssignmentAttempt;
import edu.isnap.dataset.AttemptAction;
import edu.isnap.datasets.csc200.Spring2016;
import edu.isnap.eval.util.Prune;
import edu.isnap.hint.util.SimpleNodeBuilder;
import edu.isnap.node.Node;
import edu.isnap.parser.Store.Mode;

public class StudentEval {

	public final static int MAX = 100;

	public static void main(String[] args) throws IOException {
		eval(Spring2016.GuessingGame1);
	}

	private static void eval(Assignment assignment) throws IOException {

		Map<String, AssignmentAttempt> paths = assignment.load(Mode.Use, true);

		for (int i = 0; i < 2; i++) {
			int max = MAX;

			HashSet<String> seen = new HashSet<>();
			HashSet<String> solutions = new HashSet<>();
			int totalNoDouble = 0;
			int total = 0;
			int students = 0;

			int pass0 = 0;

			double totalGrade = 0;
			int perfectGrade = 0;
			double minGrade = 1;

			for (String student : paths.keySet()) {
				if (assignment.ignore(student)) continue;
				if (--max < 0) break;

				AssignmentAttempt solutionPath = paths.get(student);
				List<Node> nodes = new LinkedList<>();
				for (AttemptAction r : solutionPath) nodes.add(SimpleNodeBuilder.toTree(r.snapshot, true));

				if (i == 1) nodes = Prune.removeSmallerScripts(nodes);
				HashSet<String> studentSet = new HashSet<>();


				Node solution = nodes.get(nodes.size() - 1);
				double grade;
				if (solutionPath.researcherGrade != null) {
					grade = solutionPath.researcherGrade.average();
					if (solutionPath.researcherGrade.passed("Welcome player")) pass0++;
				} else {
					grade = AutoGrader.numberGrade(solution);
					if (AutoGrader.graders[0].pass(solution)) pass0++;
				}
				totalGrade += grade;
				minGrade = Math.min(grade, minGrade);
				if (grade == 1) perfectGrade++;

				for (Node node : nodes) {
					String ns = node.toCanonicalString();
					seen.add(ns);
					studentSet.add(ns);
				}

				total += nodes.size();
				totalNoDouble += studentSet.size();
				solutions.add(nodes.get(nodes.size() - 1).toCanonicalString());
				students++;

			}

			System.out.println("Pass0: " + pass0 + "/" + students);
			System.out.println("Unique: " + seen.size() + "/" + total);
			System.out.println("Unique no double: " + seen.size()  + "/" +  totalNoDouble);
			System.out.println("Unique solutions: " + solutions.size()  + "/" +  students);
			System.out.printf("Mean grade: %.03f\n", (totalGrade / students));
			System.out.println("Perfect grades: " + perfectGrade);
			System.out.println("Min grade: " + minGrade);
		}
	}
}
