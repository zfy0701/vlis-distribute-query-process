package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.*;
import edu.cmu.vlis.distributed.relationalalgebra.*;
import edu.cmu.vlis.distributed.relationalalgebra.Fragment.PartitionType;

public class AlgebraReductionVisitor implements AVisitor<AlgebraNode> {

	// rule 3's sub function
	private boolean possibleUselessProjection(Projection node) {
		if (node.getColumns().size() == 1)
			return true;
		AlgebraNode child = node.getChild();
		if (child instanceof Selection) {
			child = ((Selection) child).getChild();
		}
		
		if (child instanceof Fragment) {
			Fragment frag = (Fragment) child;
			if (frag.getPartitionType() == PartitionType.Vertical) {
				if (node.getColumns().size() == 1
						&& node.getColumn(0).equals(frag.getColumn(0))) {
					return true;
				}
			}
		} else {
			System.err.println("there should have a Fragment");
		}
		return false;
	}
	
	@Override
	public AlgebraNode visitNode(Join node) {
		AlgebraNode newLeft = node.getLeft().accept(this);
		if (newLeft == null)
			return null;
		node.setLeft(newLeft);

		AlgebraNode newRight = node.getRight().accept(this);
		if (newRight == null)
			return null;
		node.setRight(newRight);

		if (newLeft instanceof Fragment && newRight instanceof Fragment) {
			// Rule 2
			Fragment left = (Fragment) newLeft;
			Fragment right = (Fragment) newRight;

			ZExp and = ZExpUtils.SimplifyAnd(left.getCondition(),
					right.getCondition());
			if (and == ZConstant.FALSE) {
				return null;
			}
		}

		if (node.isVerticalFragmented()) {
			//Finish rule 3 here
			Projection pLeft = (Projection) newLeft;
			Projection pRight = (Projection) newRight;
			
			if (possibleUselessProjection(pLeft)) {
				return pRight;
			}
			
			if (possibleUselessProjection(pRight)) {
				return pLeft;
			}
			
			if (!(newRight instanceof Projection)) {
				System.err
						.println("in this phase, left and right should have same type");
			}
		}

		return node;
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
		if (node.getChild() instanceof Fragment) {
			// RULE 1
			Fragment child = (Fragment) node.getChild();
			ZExp newCondition = ZExpUtils.SimplifyAnd(child.getCondition(),
					node.getCondition());
			if (newCondition == ZConstant.FALSE) {
				return null;
			}
		} else {
			AlgebraNode child = node.getChild().accept(this);
			if (child == null) {
				return null;
			}
			node.setChild(child);
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
			AlgebraNode newChild = child.accept(this);
			if (newChild != null) {
				newChildren.add(newChild);
			}
		}
		node.setChildren(newChildren);
		if (newChildren.size() == 1) {
			return newChildren.get(0);
		}
		if (newChildren.size() == 0) {
			return null;
		}
		return node;
	}

	@Override
	public AlgebraNode visitNode(Fragment node) {
		return node;
	}

	@Override
	public AlgebraNode visitNode(Projection node) {
		AlgebraNode newChild = node.getChild().accept(this);

		if (newChild == null) {
			return null;
		}

		node.setChild(newChild);
		return node;
	}

}
