package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.relationalalgebra.visitor.AVisitor;

public class Join extends AlgebraNode {

	private AlgebraNode left;
	private AlgebraNode right;
	private ZSelectItem leftColumn = null;
	private ZSelectItem rightColumn = null;
	
	public Join(AlgebraNode left, AlgebraNode right) {
		//this is the natural join
		setLeft(left);
		setRight(right);
	}
	
	public Join(AlgebraNode left, ZSelectItem leftColumn, AlgebraNode right, ZSelectItem rightColumn) {
		this.leftColumn = leftColumn;
		this.rightColumn = rightColumn;
		setLeft(left);
		setRight(right);
	}
	
	public AlgebraNode getLeft() {
		return left;
	}

	public AlgebraNode getRight() {
		return right;
	}
	
	public void setLeft(AlgebraNode left) {
		this.left = left;
		this.left.setFather(this);
	}

	public void setRight(AlgebraNode right) {
		this.right = right;
		this.right.setFather(this);
	}	

	public ZSelectItem getLeftColumn() {
		return leftColumn;
	}

	public ZSelectItem getRightColumn() {
		return rightColumn;
	}
	
	@Override
	public String toString() {
		return "Join[" + this.getName() + "]: " + leftColumn + " = " + rightColumn  ;
	}

	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}

	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
		left.accept(visitor);
		right.accept(visitor);
	}

	@Override
	public boolean containsRelation(String name) {
		if (this.name.equals(name))
			return true;
		return left.containsRelation(name) || right.containsRelation(name);
	}

	//this field would be true if this is the place where vertical fragment happen
	private boolean verticalFragmented = false;
	
	/*
	 * if this join node represent the vertical fragment, then it will return true
	 */
	public boolean isVerticalFragmented() {
		return verticalFragmented;
	}

	public void setVerticalFragmented(boolean containFragment) {
		this.verticalFragmented = containFragment;
	}

	@Override
	public List<ZSelectItem> getColumns() {
		List<ZSelectItem> res = new ArrayList<ZSelectItem>();
		
		Set<String> hash = new HashSet<String>();
		for (ZSelectItem z : left.getColumns()) {
			if (!hash.contains(z.getColumn())) {
				hash.add(z.getColumn());
				res.add(z);
			}
		}
		
		for (ZSelectItem z : right.getColumns()) {
			if (!hash.contains(z.getColumn())) {
				hash.add(z.getColumn());
				res.add(z);
			}
		}
		return res;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Join) {
			Join j = (Join) o;
			if (left.equals(j.left) && right.equals(j.right) && leftColumn.equals(j.leftColumn) && rightColumn.equals(j.rightColumn))
				return true;
		}
		return false;	
	}
}
