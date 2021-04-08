package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.io.*;

import edu.cmu.vlis.distributed.relationalalgebra.*;

public class AlegbraTreePrintVisitor implements AVisitor<AlgebraNode> {
	
	private String indent = "";
	
	private PrintStream ps = System.out;
	
	public AlegbraTreePrintVisitor(PrintStream ps) {
		this.ps = ps;
	}

	private void enter(AlgebraNode node) {
		ps.println(indent + node.toString());
		indent += "  ";		
	}
	
	private void leave(AlgebraNode node) {
		indent = indent.substring(2);
	}
	
	
	@Override
	public AlgebraNode visitNode(Join node) {
		enter(node);
		node.visitChildren(this);
		leave(node);
		return null;
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
		enter(node);
		node.visitChildren(this);
		leave(node);
		return null;
	}

	@Override
	public AlgebraNode visitNode(Relation node) {
		enter(node);
		leave(node);
		return null;
	}

	@Override
	public AlgebraNode visitNode(Union node) {
		enter(node);
		node.visitChildren(this);
		leave(node);
		return null;
	}

	@Override
	public AlgebraNode visitNode(Fragment node) {
		enter(node);
		leave(node);
		return null;
	}

	@Override
	public AlgebraNode visitNode(Projection node) {
		enter(node);
		node.visitChildren(this);
		leave(node);
		return null;
	}

}
