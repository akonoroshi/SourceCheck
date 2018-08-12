package edu.isnap.datasets.run;

import java.io.IOException;

import edu.isnap.ctd.graph.Node;
import edu.isnap.datasets.CSC200Solutions;
import edu.isnap.template.parse.TemplateParser;

public class RunTemplater {
	public static void main(String[] args) throws IOException {
		Node.PrettyPrintSpacing = 4;
		Node.PrettyPrintUseColon = true;
		TemplateParser.parseSnapTemplate(CSC200Solutions.PolygonMaker);
//		for (Assignment assignment : CampSolutions.All) {
//			TemplateParser.parseSnapTemplate(assignment);
//		}
	}
}