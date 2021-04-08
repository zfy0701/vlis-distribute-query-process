package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import org.gibello.zql.ast.ZConstant;
import org.gibello.zql.ast.ZExp;
import org.gibello.zql.ast.ZExpression;
import org.gibello.zql.ast.ZSelectItem;
import org.gibello.zql.ast.visitor.ColumnExtractorVisitor;

import edu.cmu.vlis.distributed.ZExpUtils;
import edu.cmu.vlis.distributed.relationalalgebra.*;

public class PushSelectionDownVisitor implements AVisitor<AlgebraNode> {

	@Override
	public AlgebraNode visitNode(Join node) {
		node.setLeft(node.getLeft().accept(this));
		node.setRight(node.getRight().accept(this));
		return node;
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
		AlgebraNode newNode = node.getChild().accept(
				new PushSelectionDown(node));
		if (newNode != node) {// so this layer is going to change
			return newNode.accept(this);
		}

		node.setChild(node.getChild().accept(this));
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
		return node;
	}
	
	/*
	 * Sub class doing really action to push node down
	 */
	private class PushSelectionDown implements AVisitor<AlgebraNode> {

		private Selection nodeToPush;
		private Vector<ZExp> newConds = new Vector<ZExp>();
		
		public PushSelectionDown(Selection nodeToPush) {
			this.nodeToPush = nodeToPush;
		}
		
		// this expression can be only logic operations > < >= <=
		//if the condition can fully contain in root, then return true
		public boolean canPush(ZExpression expr, AlgebraNode root) {
//			ZConstant left = (ZConstant) expr.getOperand(0);
//			ZConstant right = (ZConstant) expr.getOperand(1);
//			boolean res = root.containsRelation(left.getTable());
//			if (right.getType() == ZConstant.COLUMNNAME) {
//				res = res && root.containsRelation(right.getTable());
//			}
			
//			List<ZSelectItem> res = new ArrayList<ZSelectItem>();
//			ColumnExtractorVisitor vis = new ColumnExtractorVisitor();
//			expr.accept(vis);
//			List<ZConstant> tmp = vis.getResult();
//			
//			for (ZConstant c : tmp) {
//				res.add(new ZSelectItem(c.toString()));
//			}
			
			
			for (ZExp exp : expr.getOperands()) {
				if (exp instanceof ZConstant) {
					ZConstant c = (ZConstant) exp;
					if (c.getType() == ZConstant.COLUMNNAME) {
						if (!root.getColumns().contains(new ZSelectItem(c.toString())))
							return false;
					}
				}
			}
			
			return true;
		}
		
		private Selection pushDown(AlgebraNode father) {		
			Selection child = new Selection();		
			
			if (nodeToPush.getCondition() instanceof ZExpression) {
				ZExpression expr = (ZExpression) nodeToPush.getCondition();
				if (!expr.getOperator().equals("AND")) {
					if (canPush(expr, father)) {
						child.setCondition(expr);
						newConds.remove(expr);
						return child;
					}
					return null;
				}

				ZExpression newCondChild = new ZExpression("AND");
							
				for (ZExp cond : expr.getOperands()) { //iterate all conditions of nodeToPush
					if (cond instanceof ZExpression) {
						if (canPush((ZExpression) cond, father)) {
							newCondChild.addOperand(cond);
							newConds.remove(cond);
						} 
					} else {
						System.err.println("violate the rule: should be expression here");
					}
				}
				
				if (newCondChild.getOperands().size() == 0)
					return null;
							
				child.setCondition(ZExpUtils.simplify(newCondChild));			
			} else {
				System.err.println("here should have an Expression");
			}
			return child;
		}

		@Override
		public AlgebraNode visitNode(Join node) {		
			if (!(nodeToPush.getCondition() instanceof ZExpression)) {
				System.err.println("here should have an Expression");
			}
			
			ZExpression oldCond = (ZExpression)nodeToPush.getCondition();		
			if (ZExpUtils.isAnd(oldCond)) { //in this case, add all original conditions
				newConds.addAll(oldCond.getOperands());
			} else { 
				newConds.add(oldCond);
			}
					
			Selection newLeft = pushDown(node.getLeft());
			Selection newRight = pushDown(node.getRight());
			if (newLeft != null) {
				newLeft.setChild(node.getLeft());
				node.setLeft(newLeft);
			}

			if (newRight != null) {
				newRight.setChild(node.getRight());
				node.setRight(newRight);
			}
			
			if (newConds.size() == 0) { //which mean the selection will be fully pushed to one side
				return node;		
			}
							
			ZExpression newCondForPushNode = new ZExpression("AND");
			newCondForPushNode.setOperands(newConds);		
			
			nodeToPush.setCondition(ZExpUtils.simplify(newCondForPushNode)); //if newConds.size() == 1, here will simplify it
			return nodeToPush;
		}

		@Override
		public AlgebraNode visitNode(Selection node) {
			node.setCondition(ZExpUtils.SimplifyAnd(node.getCondition(),
					nodeToPush.getCondition()));
			return node;
		}

		@Override
		public AlgebraNode visitNode(Relation node) {
			return nodeToPush;
		}

		@Override
		public AlgebraNode visitNode(Union node) {
			List<AlgebraNode> children = node.getChildren();
			List<AlgebraNode> newChildren = new ArrayList<AlgebraNode>();
			for (AlgebraNode child : children) {
				Selection newChild;
				try {
					newChild = (Selection) nodeToPush.clone();
					newChild.setChild(child);
					newChildren.add(newChild);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
			node.setChildren(newChildren);
			return node;
		}

		@Override
		public AlgebraNode visitNode(Fragment node) {
			return nodeToPush;
		}

		@Override
		public AlgebraNode visitNode(Projection node) {
			nodeToPush.setChild(node.getChild());
			node.setChild(nodeToPush);
			return node;
		}

	}
}
