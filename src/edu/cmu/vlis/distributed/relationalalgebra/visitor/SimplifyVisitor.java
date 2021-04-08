package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.ZExpUtils;
import edu.cmu.vlis.distributed.relationalalgebra.*;

/*
 * This pass is to remove redundant selection and projection 
 * this visitor should be wrong several times because one optimization might enable another one
 */
public class SimplifyVisitor implements AVisitor<AlgebraNode> {

	private boolean changed = false;
	
	@Override
	public AlgebraNode visitNode(Join node) {
		node.setLeft(node.getLeft().accept(this));
		node.setRight(node.getRight().accept(this));
		return node;
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
		node.setChild(node.getChild().accept(this));
		
		AlgebraNode child = node.getChild();
		if (child instanceof Selection) {
			child = ((Selection) child).getChild();
		}
		
		if (child instanceof Selection) {
			Selection sChild = (Selection) child;
			ZExp newCond = ZExpUtils.SimplifyAnd(node.getCondition(), sChild.getCondition());
			sChild.setCondition(newCond);
			changed = true;
			return node.getChild();
		} else if (child instanceof Fragment) {
			//Fragment fChild = (Fragment) child;
			//you might be able to simplify the expression but I think there is no real chance here
		}
		return node;
	}

	@Override
	public AlgebraNode visitNode(Relation node) {
		return node;
	}

	@Override
	public AlgebraNode visitNode(Union node) {
		List<AlgebraNode> newChildren = new ArrayList<AlgebraNode>();
		for (AlgebraNode child : node.getChildren()) {
			if (!newChildren.contains(child))
				newChildren.add(child.accept(this));
		}
		node.setChildren(newChildren);
		return node;
	}

	@Override
	public AlgebraNode visitNode(Fragment node) {
		return node;
	}

	@Override
	public AlgebraNode visitNode(Projection node) {
		node.setChild(node.getChild().accept(this));
		
		AlgebraNode child = node.getChild();
		if (child instanceof Selection) {
			child = ((Selection) child).getChild();
		}
		
		if (child instanceof Projection) {
			node.setChild(((Projection) child).getChild());
			changed = true;
			return node;
		} else if (child instanceof Fragment) {
			Fragment fChild = (Fragment) child;
			if (fChild.getColumns().size() == node.getColumns().size()) {
				changed = true;
				return node.getChild();  //don't directly return child because you want keep the middle layer in line 68
			}
		}
		
		return node;				
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

}
