import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import edu.isnap.ctd.graph.Node;
import edu.isnap.ctd.hint.HintGenerator;
import edu.isnap.ctd.hint.HintMapBuilder;
import edu.isnap.ctd.hint.VectorHint;
import edu.isnap.hint.SnapHintBuilder;
import edu.isnap.hint.util.SimpleNodeBuilder;
import edu.isnap.parser.elements.Snapshot;
import edu.isnap.unittest.UnitTest;


@SuppressWarnings("serial")
@WebServlet(name="hints", urlPatterns="/hints")
public class HintServlet extends HttpServlet {

	private final static String DEFAULT_ASSIGNMENT = "guess1Lab";
	private final static int DEFAULT_MIN_GRADE = 100;

	private static HashMap<String, HintGenerator> generators = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		loadGenerator(DEFAULT_ASSIGNMENT, DEFAULT_MIN_GRADE);
		resp.setContentType("text");
		resp.getOutputStream().println("Loaded cache for " + DEFAULT_ASSIGNMENT);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String origin = req.getHeader("origin");
		if (origin != null) resp.setHeader("Access-Control-Allow-Origin", origin);
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
		resp.setHeader("Access-Control-Allow-Headers",
				"Content-Type, Authorization, X-Requested-With");

		Scanner sc = new Scanner(req.getInputStream());
		StringBuilder sb = new StringBuilder();
		while (sc.hasNextLine()) sb.append(sc.nextLine());
		sc.close();

		String xml = sb.toString();
		Snapshot snapshot = Snapshot.parse(null, xml);

		PrintStream out = new PrintStream(resp.getOutputStream());

		int minGrade = DEFAULT_MIN_GRADE;
		String mgs = req.getParameter("minGrade");
		if (mgs != null) {
			try {
				minGrade = Integer.parseInt(mgs);
			} catch (Exception e) { }
		}

		String hint = req.getParameter("hint");
		String assignment = req.getParameter("assignmentID");
		if (assignment == null){
			assignment = DEFAULT_ASSIGNMENT;
		}

		if (hint != null) {
			out.println(UnitTest.saveUnitTest(assignment, xml, hint));
		} else {
			String hintJSON = getHintJSON(snapshot, assignment, minGrade);
			resp.setContentType("text/json");
			out.println(hintJSON);
		}
	}

	private String getHintJSON(Snapshot snapshot, String assignment, int minGrade) {
		String json = "";

		HintGenerator generator = loadGenerator(assignment, minGrade);
		if (generator == null) {
			return "[]";
		}

//		long time = System.currentTimeMillis();

//		System.out.println(snapshot.toCode(true));
		Node node = SimpleNodeBuilder.toTree(snapshot, true);

		List<VectorHint> hints = generator.getHints(node);

		json += "[";
		for (int i = 0; i < hints.size(); i++) {
			if (i > 0) json += ",\n";
			json += hintToJson(hints.get(i));
		}
		json += "]";

//		long elapsed = System.currentTimeMillis() - time;
//		System.out.println(elapsed);

		return json;
	}

	public static String hintToJson(VectorHint hint) {
		return String.format("{\"from\": \"%s\", \"to\": \"%s\", \"data\": %s}",
				hint.from(), hint.to(), hint.data());
	}

	private HintGenerator loadGenerator(String assignment, int minGrade) {
		if (assignment == null || "test".equals(assignment)) {
			assignment = DEFAULT_ASSIGNMENT;
		}
		HintGenerator generator = generators.get(assignment);
		if (generator == null) {
			Kryo kryo = SnapHintBuilder.getKryo();
			String path = String.format("/WEB-INF/data/%s-g%03d.cached", assignment, minGrade);
			InputStream stream = getServletContext().getResourceAsStream(path);
			if (stream == null) return null;
			Input input = new Input(stream);
			HintMapBuilder builder = kryo.readObject(input, HintMapBuilder.class);
			input.close();

			generators.put(assignment, generator = builder.hintGenerator());
		}
		return generator;
	}
}
