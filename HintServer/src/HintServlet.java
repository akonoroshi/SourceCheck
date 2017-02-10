import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import edu.isnap.ctd.graph.Node;
import edu.isnap.ctd.hint.Hint;
import edu.isnap.ctd.hint.HintGenerator;
import edu.isnap.ctd.hint.HintHighlighter;
import edu.isnap.ctd.hint.HintMap;
import edu.isnap.ctd.hint.HintMapBuilder;
import edu.isnap.hint.SnapHintBuilder;
import edu.isnap.hint.util.SimpleNodeBuilder;
import edu.isnap.parser.elements.Snapshot;
import edu.isnap.unittest.UnitTest;


@SuppressWarnings("serial")
@WebServlet(name="hints", urlPatterns="/hints")
public class HintServlet extends HttpServlet {

	private final static String DEFAULT_ASSIGNMENT = "guess1Lab";
	private final static int DEFAULT_MIN_GRADE = 100;

	private static HashMap<String, HintMap> hintMaps = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		loadHintMap(DEFAULT_ASSIGNMENT, null, DEFAULT_MIN_GRADE);
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
		String dataset = req.getParameter("dataset");
		String hintTypes = req.getParameter("hintTypes");

		if (hint != null) {
			out.println(UnitTest.saveUnitTest(assignment, xml, hint));
		} else {
			String hintJSON = getHintJSON(snapshot, assignment, dataset, minGrade, hintTypes);
			resp.setContentType("text/json");
			out.println(hintJSON);
		}
	}

	private String getHintJSON(Snapshot snapshot, String assignment, String dataset, int minGrade,
			String hintTypes) {
		HintMap hintMap = loadHintMap(assignment, dataset, minGrade);
		if (hintMap == null) {
			return "[]";
		}


		JSONArray array = new JSONArray();


//		System.out.println(snapshot.toCode(true));
		Node node = SimpleNodeBuilder.toTree(snapshot, true);

		List<Hint> hints = new LinkedList<>();
		// Return the hints for each type requested, or bubble hints if none is provided
		if (hintTypes != null) hintTypes = hintTypes.toLowerCase();
		if (hintTypes == null || hintTypes.contains("bubble")) {
			try {
				hints.addAll(new HintGenerator(hintMap).getHints(node));
			} catch (Exception e) {
				array.put(errorToJSON(e, true));
			}
		}
		if (hintTypes != null && hintTypes.contains("highlight")){
			try {
				hints.addAll(new HintHighlighter(hintMap).highlight(node));
			} catch (Exception e) {
				array.put(errorToJSON(e, true));
			}
		}

		for (Hint hint : hints) {
			array.put(hintToJSON(hint));
		}

		return array.toString();
	}

	public static JSONObject hintToJSON(Hint hint) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("from", hint.from());
			obj.put("to", hint.to());
			obj.put("type", hint.type());
			obj.put("data", hint.data());
			return obj;
		} catch (Exception e) {
			return errorToJSON(e, true);
		}
	}

	private static JSONObject errorToJSON(Exception e, boolean print) {
		if (print) e.printStackTrace();
		JSONObject error = new JSONObject();
		error.put("error", true);
		error.put("message", e.getClass().getName() + ": " +  e.getMessage());
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		error.put("stack", sw.toString());
		error.put("time", new Date().toString());
		return error;
	}

	private HintMap loadHintMap(String assignment, String dataset, int minGrade) {
		if (assignment == null || "test".equals(assignment)) {
			assignment = DEFAULT_ASSIGNMENT;
		}
		String key = assignment + dataset + minGrade;
		HintMap hintMap = hintMaps.get(key);
		if (hintMap == null) {
			Kryo kryo = SnapHintBuilder.getKryo();
			String path = String.format("/WEB-INF/data/%s-g%03d%s.cached", assignment, minGrade,
					dataset == null ? "" : ("-" + dataset));
			InputStream stream = getServletContext().getResourceAsStream(path);
			if (stream == null) return null;
			Input input = new Input(stream);
			HintMapBuilder builder = kryo.readObject(input, HintMapBuilder.class);
			input.close();

			hintMaps.put(key, hintMap = builder.hintMap);
		}
		return hintMap;
	}
}
