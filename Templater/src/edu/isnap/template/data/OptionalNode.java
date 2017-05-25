package edu.isnap.template.data;

import java.util.LinkedList;
import java.util.List;

public class OptionalNode extends DefaultNode {

	@Override
	public boolean inline() {
		return true;
	}

	@Override
	public List<BNode> getVariants(Context context) {
		List<BNode> variants = new LinkedList<>();
		variants.add(new BNode(type, inline()));
		variants.addAll(super.getVariants(context));
		return variants;
	}
}