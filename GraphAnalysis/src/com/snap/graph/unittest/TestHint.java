package com.snap.graph.unittest;

import java.io.PrintStream;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.snap.graph.data.HintFactoryMap.VectorHint;

public class TestHint {
	public final HintPart main, old;
	public final boolean caution, expectedFailure;
	
	
	public TestHint(String json) {
		JSONObject obj = new JSONObject(json);
		JSONObject data = obj.getJSONObject("data");
		main = new HintPart(data, null);
		old = data.has("oldRoot") ? new HintPart(data, "old") : null;
		caution = data.getBoolean("caution");
		expectedFailure = data.has("expectedFailure") ? 
				data.getBoolean("expectedFailure") : false;
	}
	
	public TestHint(VectorHint hint) {
		this("{data: " + hint.data() + "}");
	}
	
	public boolean sharesRoot(TestHint hint) {
		return displayRoot().equals(hint.displayRoot());
	}
	
	public Root displayRoot() {
		return (old == null ? main : old).root;
	}

	public boolean test(TestHint hint, PrintStream out) {
		boolean pass = true;
		if (caution != hint.caution) {
			out.println("Caution should be: " + caution);
			pass = false;
		}
		pass &= main.test(hint.main, out);
		if (old != null) pass &= old.test(hint.old, out);
		
		return pass;
	}
	
	public static class HintPart {
		public final String[] from, to, goal;
		public final Root root;
		
		public HintPart(JSONObject obj, String keyPrefix) {
			from = getArray(obj, keyPrefix, "from");
			to = getArray(obj, keyPrefix, "to");
			goal = getArray(obj, keyPrefix, "goal");
			root = new Root(obj.getJSONObject(key(keyPrefix, "root")));
		}
		
		public boolean test(HintPart hint, PrintStream out) {
			if (hint == null) {
				out.println("Missing hint part");
				return false;
			}
			boolean pass = testArray("from", from, hint.from, out);
			pass &= testArray("to", to, hint.to, out);
			pass &= testArray("goal", goal, hint.goal, out);
			return pass;
		}
		
		private boolean testArray(String name, String[] a1, String[] a2, 
				PrintStream out) {
			if (Arrays.equals(a1, a2)) return true;
			out.printf("%s mismatch: %s vs %s\n", name, Arrays.toString(a1), 
					Arrays.toString(a2));
			return false;
		}

		private static String key(String keyPrefix, String key) {
			if (keyPrefix == null || key == null) return key;
			return keyPrefix + key.substring(0, 1).toUpperCase() + key.substring(1);
		}
		
		public static String[] getArray(JSONObject obj, String keyPrefix, 
				String name) {
			String key = key(keyPrefix, name);
			if (!obj.has(key)) return null;
			JSONArray a = obj.getJSONArray(key);
			for (int i = 0; i < a.length(); i++) {
				// These get added to the hint by the client
				if ("prototypeHatBlock".equals(a.get(i))) {
					a.remove(i--);
				}
			}
			String[] array = new String[a.length()];
			for (int i = 0; i < a.length(); i++) {
				array[i] = a.getString(i);;
			}
			return array;
		}
	}
	
	public static class Root {
		public final String label;
		public final int index;
		public final Root parent;
		
		public Root(JSONObject obj) {
			label = obj.getString("label");
			index = obj.getInt("index");
			if (obj.has("parent") && !obj.isNull("parent")) {
				parent = new Root(obj.getJSONObject("parent"));
			} else {
				parent = null;
			}
		}
		
		public boolean equals(Root root) {
			return root != null && label.equals(root.label) && index == root.index &&
					(parent == null || parent.equals(root.parent));
		}
	}
}
