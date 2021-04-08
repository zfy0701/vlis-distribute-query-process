package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import edu.cmu.vlis.distributed.relationalalgebra.*;

public class PushUnionUpVisitor implements AVisitor<AlgebraNode> {

	
	private AlgebraNode UnionJoinUnion(Union left, Union right, Join join) {
		Union res = new Union();
		
		for (AlgebraNode lc : left.getChildren()) {
			for (AlgebraNode rc : right.getChildren()) {
				try {
					Join newJoin = new Join(lc.clone(), join.getLeftColumn(), rc.clone(), join.getRightColumn());
					res.addChildren(newJoin);
					newJoin.setVerticalFragmented(join.isVerticalFragmented());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
		return res;
	}
	
	private AlgebraNode UnionJoinOther(Union left, AlgebraNode right, Join join, boolean switchside) {
		Union res = new Union();		
		for (AlgebraNode lc : left.getChildren()) {
			try {
				Join newJoin;
				if (switchside == false)
					newJoin = new Join(lc, join.getLeftColumn(), right.clone(), join.getRightColumn());
				else
					newJoin = new Join(right.clone(), join.getLeftColumn(), lc, join.getRightColumn());
				newJoin.setVerticalFragmented(join.isVerticalFragmented());
				res.addChildren(newJoin);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return res;
	}
		
	@Override
	public AlgebraNode visitNode(Join node) {
		node.setLeft(node.getLeft().accept(this));
		node.setRight(node.getRight().accept(this));
		
		// ..... below is for programming assignment
		
		
		if (node.getLeft() instanceof Union) {
			if (node.getRight() instanceof Union) {
				return UnionJoinUnion((Union)node.getLeft(), (Union)node.getRight(), node);			
			} else {
				return UnionJoinOther((Union)node.getLeft(), node.getRight(), node, false);
			}
		} else if (node.getRight() instanceof Union)  {
			return UnionJoinOther((Union)node.getRight(), node.getLeft(), node, true);
		} else {
			return node;
		}
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
		node.setChild(node.getChild().accept(this));
		if (node.getChild() instanceof Union) {
			Union u = (Union)node.getChild();
			List<AlgebraNode> newChildren = new ArrayList<AlgebraNode>();
			for (AlgebraNode child : u.getChildren()) {
				Selection newChild = new Selection(child, node.getCondition());
				newChildren.add(newChild);
			}
			u.setChildren(newChildren);
			return u;
		} else {
			return node;
		}
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
			if (newChild instanceof Union) {
				newChildren.addAll(((Union) newChild).getChildren());
			} else {
				newChildren.add(newChild);
			}
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
		if (node.getChild() instanceof Union) {
			Union u = (Union)node.getChild();
			List<AlgebraNode> newChildren = new ArrayList<AlgebraNode>();
			for (AlgebraNode child : u.getChildren()) {
				Projection newChild = new Projection(child, node.getColumns());
				newChildren.add(newChild);
			}
			u.setChildren(newChildren);
			return u;
		} else {
			return node;
		}
	}

}
