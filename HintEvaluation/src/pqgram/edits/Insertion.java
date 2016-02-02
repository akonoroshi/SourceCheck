package pqgram.edits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.snap.graph.data.Node;

import astrecognition.model.Graph;
import astrecognition.model.Tree;

public class Insertion extends PositionalEdit {
	
	private static String INSERTION_STRING = "%d: Insert %s on to %s (%d, %d) {%s}";
	
	private int end;
	private Collection<String> inheritedChildren;
	
	public Insertion(String a, String b, Graph aG, Graph bG, int start, int end) {
		super(a, b, aG, bG, start);
		this.end = end;
		this.inheritedChildren = new ArrayList<String>();
	}
	
	public int getStart() {
		return super.getPosition();
	}
	
	public int getEnd() {
		return this.end;
	}
	
	public void addInheritedChild(String inheritedChild) {
		this.inheritedChildren.add(inheritedChild);
	}

	@Override
	public String toString() {
		String inheritedChildrenList = "";
		for (String inheritedChild : this.inheritedChildren) {
			inheritedChildrenList += inheritedChild + ", ";
		}
		return String.format(INSERTION_STRING, this.lineNumber, this.b, this.a, this.start, this.end, inheritedChildrenList);
	}
	
	@Override
	public Node outcome(Map<String, Tree> map) {
		Node copy = map.get(a).tag.copy(false);
		int index = start - 1;
		if (index >= 0 && index <= copy.children.size()) {
			Node ins = new Node(copy, bG.getLabel());
			copy.children.add(index, ins);
			return copy.root();
		}
		return null;
	}
}
