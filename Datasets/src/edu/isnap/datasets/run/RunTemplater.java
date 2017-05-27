package edu.isnap.datasets.run;

import java.io.IOException;

import edu.isnap.ctd.graph.Node;
import edu.isnap.dataset.Assignment;
import edu.isnap.datasets.BJCSolutions2017;
import edu.isnap.template.parse.Parser;

public class RunTemplater {
	public static void main(String[] args) throws IOException {
		Node.PrettyPrintSpacing = 4;
//		Parser.parseTemplate(BJCSolutions2017.U2_L3_P3_WordPuzzleSolver);
		for (Assignment assignment : BJCSolutions2017.All) {
			Parser.parseTemplate(assignment);
		}
	}
}
