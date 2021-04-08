package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import org.gibello.zql.ast.ZSelectItem;

import edu.cmu.vlis.distributed.relationalalgebra.*;
import edu.cmu.vlis.distributed.relationalalgebra.Fragment.PartitionType;

public class PushProjectionDownVisitor implements AVisitor<AlgebraNode> {

	@Override
	public AlgebraNode visitNode(Join node) {
		node.setLeft(node.getLeft().accept(this));
		node.setRight(node.getRight().accept(this));
		return node;
	}

	@Override
	public AlgebraNode visitNode(Selection node) {
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

		AlgebraNode newNode = node.getChild().accept(new PushProjectionDown(node));
		if (newNode != node) {// so this layer is going to change
			return newNode.accept(this);
		}

		node.setChild(node.getChild().accept(this));
		return node;
	}

	/*
	 * Sub class doing really action to push node down
	 */
	private class PushProjectionDown implements AVisitor<AlgebraNode> {

		private Projection nodeToPush;

		public PushProjectionDown(Projection nodeToPush) {
			this.nodeToPush = nodeToPush;
		}

		private Projection pushDown(AlgebraNode father) {
			Projection child = new Projection();
			for (ZSelectItem col : nodeToPush.getColumns()) {
				if (father.containsRelation(col.getTable()) && father.getColumns().contains(col)
						&& !child.containsColumn(col.toString())) {
					child.addColumn(col);
				}
			}
			return child;
		}

		@Override
		public AlgebraNode visitNode(Join node) {

			Projection newLeft = pushDown(node.getLeft());
			newLeft.addColumn(new ZSelectItem(node.getLeftColumn().toString()));

			Projection newRight = pushDown(node.getRight());
			newRight.addColumn(new ZSelectItem(node.getRightColumn().toString()));

			newLeft.setChild(node.getLeft());
			newRight.setChild(node.getRight());
			node.setLeft(newLeft);
			node.setRight(newRight);

			boolean containAll = true;
			for (ZSelectItem col : newLeft.getColumns()) {
				if (!nodeToPush.containsColumn(col.toString()))
					containAll = false;
			}
			for (ZSelectItem col : newRight.getColumns()) {
				if (!nodeToPush.containsColumn(col.toString()))
					containAll = false;
			}

			if (containAll)
				return node;
			return nodeToPush;
		}

		@Override
		public AlgebraNode visitNode(Selection node) {
			Projection proj = new Projection();
			proj.getColumns().addAll(nodeToPush.getColumns());
			
			List<ZSelectItem> tmp = node.getColumnsInCondition();

			for (ZSelectItem col : tmp) {
				if (!proj.containsColumn(col.toString())) {
					proj.addColumn(col);
				}
			}

			proj.setChild(node.getChild());
			node.setChild(proj);
			if (nodeToPush.getColumns().size() == proj.getColumns().size()) {
				return node;
			}
			return nodeToPush;
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
				try {
					Projection newChild = (Projection) nodeToPush.clone();
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
			if (node.getPartitionType() == PartitionType.Vertical) {
				// Simplify the projection on vertical fragment
				List<ZSelectItem> newCols = new ArrayList<ZSelectItem>();
				for (ZSelectItem col : nodeToPush.getColumns()) {
					if (node.containsColumn(col.toString())) {
						newCols.add(col);
					}
				}

				if (newCols.size() == 0) {
					System.err.println("I think you have pass an wrong statement");
				}
				nodeToPush.setColumns(newCols);
			}

			return nodeToPush;
		}

		@Override
		public AlgebraNode visitNode(Projection node) {
			// Merge two projection
			List<ZSelectItem> cols = new ArrayList<ZSelectItem>();
			for (ZSelectItem col : nodeToPush.getColumns()) {
				if (node.containsColumn(col.toString())) {
					cols.add(col);
				}
			}
			node.setColumns(cols);
			return node;
		}
	}
}
