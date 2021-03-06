package edu.isnap.eval.tutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isnap.hint.HintConfig;
import edu.isnap.hint.HintData;
import edu.isnap.rating.data.HintRequest;
import edu.isnap.rating.data.Trace;
import edu.isnap.rating.data.TrainingDataset;
import edu.isnap.sourcecheck.HintHighlighter;

public class ImportHighlightHintSet extends HighlightHintSet {

	private final Map<String, HintHighlighter> highlighters = new HashMap<>();

	public ImportHighlightHintSet(String name, HintConfig hintConfig, TrainingDataset dataset) {
		super(name, hintConfig);
		for (String assignmentID : dataset.getAssignmentIDs()) {
			List<Trace> traces = dataset.getTraces(assignmentID);
			HintData hintData = createHintData(assignmentID, hintConfig, traces);
			highlighters.put(assignmentID, new HintHighlighter(hintData));
		}
	}

	@Override
	protected HintHighlighter getHighlighter(HintRequest request) {
		return highlighters.get(request.assignmentID);
	}

}
