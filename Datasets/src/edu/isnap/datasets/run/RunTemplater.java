package edu.isnap.datasets.run;

import java.io.IOException;

import edu.isnap.ctd.graph.Node;
import edu.isnap.datasets.MTurkSolutions;
import edu.isnap.template.parse.TemplateParser;

public class RunTemplater {
	public static void main(String[] args) throws IOException {
		Node.PrettyPrintSpacing = 4;
		Node.PrettyPrintUseColon = true;
		TemplateParser.parseSnapTemplate(MTurkSolutions.DrawTriangles);
//		for (Assignment assignment : CSC200Solutions.All) {
//			TemplateParser.parseSnapTemplate(assignment);
//		}
	}
}