package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.*;

import org.gibello.zql.ast.ZSelectItem;

import edu.cmu.vlis.distributed.relationalalgebra.visitor.AVisitor;

public class Union extends AlgebraNode {
	private List<AlgebraNode> children = new ArrayList<AlgebraNode>(); //TODO changeTo TreeSet in the future

	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}

	public void addChildren(AlgebraNode child) {
		children.add(child);
		child.setFather(this);
	}

	public List<AlgebraNode> getChildren() {
		return children;
	}

	public void setChildren(List<AlgebraNode> children) {
		this.children = children;
		for (AlgebraNode child : children) {
			child.setFather(this);
		}
	}

	@Override
	public String toString() {
		return "Union[" + this.getName() + "]";
	}

	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
		for (AlgebraNode node : children) {
			node.accept(visitor);
		}
	}

	@Override
	public boolean containsRelation(String name) {
		if (this.name.equals(name))
			return true;
		for (AlgebraNode node : children) {
			if (node.containsRelation(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<ZSelectItem> getColumns() {
		return children.get(0).getColumns();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Union) {
			Union u = (Union) o;
			return this.children.equals(u.children);
		}
		return false;
	}
}
