package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.*;

import org.gibello.zql.ast.ZExp;
import org.gibello.zql.ast.ZSelectItem;

import edu.cmu.vlis.distributed.relationalalgebra.visitor.AVisitor;

public class Projection extends AlgebraNode {
	
	private List<ZSelectItem> columns = new ArrayList<ZSelectItem>();  //TODO should change to TreeSet in the future
	private AlgebraNode child = null;
	
	public Projection() {
		
	}
	public Projection(AlgebraNode child, List<ZSelectItem> columns) {
		this.child = child;
		this.columns = columns;
	}
	
	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}
	
	public void addColumn(ZSelectItem item) {
		if (!containsColumn(item.toString()))
			columns.add(item);
	}
	
	public boolean containsColumn(String c) {
		for (ZSelectItem col : columns) {
			if (col.toString().equals(c)) {
				return true;
			}
		}
		return false;
	}
	
	public AlgebraNode getChild() {
		return child;
	}
	

	public void setChild(AlgebraNode child) {
		this.child = child;
		this.child.setFather(this);
	}

	@Override
	public List<ZSelectItem> getColumns() {
		return columns;
	}
	
	public ZSelectItem getColumn(int i) {
		return columns.get(i);
	}
	
	public void setColumns(List<ZSelectItem> columns) {
		this.columns = columns;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Projection[" + this.getName() + "]: ");
		for (ZSelectItem col: columns) {
			sb.append(col);
			sb.append(", ");
		}
		return sb.toString();
	}


	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
		child.accept(visitor);
	}

	@Override
	public boolean containsRelation(String name) {
		if (this.name.equals(name))
			return true;
		return child.containsRelation(name);
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof Projection) {
			Projection p = (Projection) o;
			return this.columns.equals(p.columns) && this.child.equals(p.child);
		}
		return false;
	}
}
