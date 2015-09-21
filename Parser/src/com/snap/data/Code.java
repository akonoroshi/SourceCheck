package com.snap.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Code {
	public abstract String toCode();
	public abstract String addChildren(boolean canon, Accumulator ac);
	
	public String name() {
		return addChildren(true, NOOP);
	}
	
	public List<Block> getAllBlocks(final boolean canon) {
		final List<Block> blocks = new ArrayList<>();
		if (this instanceof Block) {
			blocks.add((Block) this);
		}
		addChildren(canon, new Accumulator() {
			@Override
			public void add(List<String> codes) { }
			
			@Override
			public void add(String code) { }
			
			@Override
			public void add(Iterable<? extends Code> codes) {
				for (Code code : codes) {
					add(code);
				}
			}
			
			@Override
			public void add(Code code) {
				blocks.addAll(code.getAllBlocks(canon));
			}
		});
		return blocks;
	}
	
	public List<Code> getAllCode(final boolean canon) {
		final List<Code> codes = new ArrayList<>();
		codes.add(this);
		addChildren(canon, new Accumulator() {
			@Override
			public void add(List<String> codes) { }
			
			@Override
			public void add(String code) { }
			
			@Override
			public void add(Iterable<? extends Code> codes) {
				for (Code code : codes) {
					add(code);
				}
			}
			
			@Override
			public void add(Code code) {
				codes.addAll(code.getAllCode(canon));
			}
		});
		return codes;
	}
	
	public interface Accumulator {
		void add(Code code);
		void add(Iterable<? extends Code> codes);
		void add(String code);
		void add(List<String> codes);
	}
	
	protected final static Accumulator NOOP = new Accumulator() {
		@Override public void add(List<String> codes) { }
		@Override public void add(String code) { }
		@Override public void add(Iterable<? extends Code> codes) { }
		@Override public void add(Code code) { }
	};
	
	protected static List<String> canonicalizeVariables(List<String> variables, boolean canon) {
		if (!canon) return variables;
		List<String> vars = new ArrayList<String>();
		for (int i = 0; i < variables.size(); i++) vars.add("var" + i);
		return vars;
	}
	
	protected static List<Block> list(Block... blocks) { return Arrays.asList(blocks); }
}