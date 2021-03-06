package edu.isnap.eval.predict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import edu.isnap.ctd.graph.vector.VectorState;
import edu.isnap.ctd.hint.HintMap;
import edu.isnap.dataset.AssignmentAttempt;
import edu.isnap.hint.HintConfig;
import edu.isnap.node.Node;
import edu.isnap.node.Node.Action;
import edu.isnap.util.Spreadsheet;
import edu.isnap.util.map.ListMap;

public class RootPathAttributes implements AttributeGenerator {

	private final ListMap<String, String> components = new ListMap<>();
	private final List<String> keys = new ArrayList<>();

	@Override
	public String getName() {
		return "rootpath";
	}

	@Override
	public void init(Map<AssignmentAttempt, Node> attemptMap, HintConfig config) {
		for (AssignmentAttempt attempt : attemptMap.keySet()) {
			Node node = attemptMap.get(attempt);
			final String id = attempt.id;

			node.recurse(new Action() {
				@Override
				public void run(Node node) {
					Node root = HintMap.toRootPath(node).root();
					VectorState state = new VectorState(node.getChildArray());
//					VectorState goal = generator.getGoalState(node);

					String key = getKey(root, state);
					components.add(key, id);
				}

				private String getKey(Node root, VectorState state) {
					String key = getKey(root);
//					if (state.items.length > 0) {
//						key += ":" + StringUtills.join(Arrays.asList(state.items), "+");
//					}
					return key;
				}

				private String getKey(Node root) {
					String key = root.type();
					while (!root.children.isEmpty()) {
						root = root.children.get(0);
						key += "." + root.type();
					}
					return key;
				}
			});
		}

		keys.addAll(components.keySet());
		for (int i = 0; i < keys.size(); i++) {
			if (components.get(keys.get(i)).size() <= 2) {
				keys.remove(i--);
			}
		}
		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return -Integer.compare(components.get(o1).size(),
						components.get(o2).size());
			}
		});
	}

	@Override
	public void addAttributes(Spreadsheet spreadsheet, AssignmentAttempt attempt, Node node) {
		for (String key : keys) {
			spreadsheet.put(key, Collections.frequency(components.get(key), attempt.id));
		}
	}
}
