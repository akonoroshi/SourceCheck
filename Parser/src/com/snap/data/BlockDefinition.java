package com.snap.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.snap.XML;

public class BlockDefinition extends Code implements IHasID {
	private static final long serialVersionUID = 1L;
	
	public final static String[] TOOLS_BLOCKS = new String[] {
       	"label %s of size %s",
       	"map %s over %s",
       	"empty? %s",
       	"keep items such that %s from %s",
       	"combine with %s items of %s",
       	"if %s then %s else %s",
       	"for %s = %s to %s %s",
       	"join words %s",
       	"list $arrowRight sentence %s",
       	"sentence $arrowRight list %s",
       	"catch %s %s",
       	"throw %s",
       	"catch %s %s",
       	"throw %s %s",
       	"for each %s of %s %s",
       	"if %s do %s and pause all $pause-1-255-220-0",
       	"word $arrowRight list %s",
       	"ignore %s",
       	"tell %s to %s",
       	"ask %s for %s",
       	"list $arrowRight word %s",
	};
	
	private final static Set<String> TOOLS_BLOCKS_SET = 
			new HashSet<String>(Arrays.asList(TOOLS_BLOCKS));
	
	public final String name, type, category, guid;
	public final boolean isToolsBlock;
	public final Script script;
	public final List<String> inputs = new ArrayList<String>();
	public final List<Script> scripts = new ArrayList<Script>();
	public String parentID;

	@SuppressWarnings("unused")
	private BlockDefinition() {
		this(null, null, null, null, null);
	}
	
	public static String steralizeName(String name) {
		if (name == null) return null;
		return name.replace("&apos;", "'").replaceAll("%'[A-Za-z0-9# ]*'", "%s");
	}
	
	public BlockDefinition(String name, String type, String category, String guid, Script script) {
		this.name = steralizeName(name);
		this.type = type;
		this.category = category;
		this.guid = guid;
		this.isToolsBlock = TOOLS_BLOCKS_SET.contains(this.name);
		this.script = script;
	}
	
	public static BlockDefinition parse(Element element) {
		String name = element.getAttribute("s");
		String type = element.getAttribute("type");
		String category = element.getAttribute("category");
		String guid = element.getAttribute("guid");
		Element scriptElement = XML.getFirstChildByTagName(element, "script");
		Script script = scriptElement == null ? new Script() : Script.parse(scriptElement);
		BlockDefinition def = new BlockDefinition(name, type, category, guid, script);
		for (Element e : XML.getGrandchildrenByTagName(element, "inputs", "input")) {
			def.inputs.add(e.getAttribute("type"));
		}
		for (Element e : XML.getGrandchildrenByTagName(element, "scripts", "script")) {
			def.scripts.add(Script.parse(e));
		}
		XML.ensureEmpty(element, "header", "code");
		return def;
	}
	
	private static <E> List<E> toList(Iterable<E> iter) {
	    List<E> list = new ArrayList<E>();
	    for (E item : iter) {
	        list.add(item);
	    }
	    return list;
	}
	
	public static BlockDefinition parseEditing(Element element) {
		String guid = element.getAttribute("guid");
		List<Element> scripts = toList(XML.getGrandchildrenByTagName(element, "scripts", "script"));
		Element mainScript = scripts.get(0);
		List<Element> blocks = toList(XML.getChildrenByTagName(mainScript, "block"));
		Element firstBlock = blocks.get(0);
		if (firstBlock.getAttribute("s").length() > 0) {
			throw new RuntimeException("Improper editing block");
		}
		Element definition = firstBlock;
		while (!definition.getTagName().equals("custom-block")) {
			definition = (Element) definition.getFirstChild();
		}
		
		String name = definition.getAttribute("s");
		
		Script script = new Script();
		for (int i = 1; i < blocks.size(); i++) {
			script.blocks.add((Block) XML.getCodeElement(blocks.get(i)));
		}
		
		BlockDefinition def = new BlockDefinition(name, null, null, guid, script);

		for (Element e : XML.getChildrenByTagName(definition, "l")) {
			def.inputs.add(e.getNodeValue());
		}

		for (int i = 1; i < scripts.size(); i++) {
			def.scripts.add(Script.parse(scripts.get(i)));
		}
		return def;
	}
	
	@Override
	public String toCode(boolean canon) {
		return new CodeBuilder(canon)
		.add(name, "customBlock")
		.addSParameters(canonicalizeVariables(inputs, canon))
		.add(" ")
		.add(script)
		.add("scripts:")
		.indent()
		.add(scripts)
		.close()
		.end();
	}

	@Override
	public String addChildren(boolean canon, Accumulator ac) {
		ac.add(script);
		ac.add(canonicalizeVariables(inputs, canon));
		ac.add(scripts);
		return canon ? "customBlock" : name;
	}
	
	@Override
	public String getID() {
		if (guid != null && guid.length() > 0) return guid;
		return String.format("%s[%s,%s,%s](%s)", parentID, name, type, category, inputs.toString());
	}
}
