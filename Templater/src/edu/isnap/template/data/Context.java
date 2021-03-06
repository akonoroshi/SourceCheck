package edu.isnap.template.data;

import java.util.HashMap;
import java.util.LinkedHashMap;

import edu.isnap.hint.TextHint;
import edu.isnap.node.Node;
import edu.isnap.node.Node.Action;

public class Context {

	public HashMap<String, DefBlockNode> blocksDefs = new HashMap<>();
	public HashMap<String, String> varDefs = new LinkedHashMap<>();
	public HashMap<String, Integer> defaultAgs = new HashMap<>();
	public HashMap<String, TextHint> hints = new HashMap<>();

	public boolean addOptional = true;

	private int nextOrderGroup = 1;

	public int nextOrderGroup() {
		return nextOrderGroup++;
	}

	public boolean stopOptional(boolean isOptional) {
		return !addOptional && isOptional;
	}

	public Context withOptional(boolean addOptional) {
		this.addOptional = addOptional;
		return this;
	}

	public static Context fromSample(Node sample) {
		final Context context = new Context();
		sample.recurse(new Action() {
			@Override
			public void run(Node node) {
				int litArgs = 0;
				for (Node child : node.children) {
					if (!child.hasType("literal")) return;
					litArgs++;
				}
				Integer prevValue = context.defaultAgs.get(node.type());
				if (prevValue != null && prevValue != litArgs) {
					System.err.println("Multiple default arg values for " + node.type());
				}
				context.defaultAgs.put(node.type(), litArgs);
			}
		});
		return context;
	}

	@Override
	public String toString() {
		return varDefs.toString();
	}
}
