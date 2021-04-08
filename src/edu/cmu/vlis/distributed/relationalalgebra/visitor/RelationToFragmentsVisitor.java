package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.*;
import edu.cmu.vlis.distributed.relationalalgebra.*;

public class RelationToFragmentsVisitor implements AVisitor<AlgebraNode> {

	private Map<String, AlgebraNode> relationMap = new HashMap<String, AlgebraNode>();
	
	public RelationToFragmentsVisitor(Map<String, AlgebraNode> relationMap) {
		this.relationMap = relationMap;
	}

	@Override
	public AlgebraNode visitNode(Relation node) {
		try {
			AlgebraNode sub = relationMap.get(node.getTable()).clone();

			if (node.getAlias() != null) {
				// because the sub algbraNode has no alias, we replace all the
				// old relation with new relation which has alias
				sub.accept(new ReplaceRelationWithAliasVisitor(node));
			}
			return sub;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

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
	public AlgebraNode visitNode(Union node) {
		List<AlgebraNode> children = node.getChildren();
		for (int i = 0; i < children.size(); i++) {
			children.set(i, children.get(i).accept(this));
		}
		return node;
	}

	@Override
	public AlgebraNode visitNode(Fragment node) {
		System.err.println("Should never be executed");
		return node;
	}

	@Override
	public AlgebraNode visitNode(Projection node) {
		node.setChild(node.getChild().accept(this));
		return node;
	}

	/*
	 * sub class used to replace the relation name in algebra tree with its alias
	 */
	private class ReplaceRelationWithAliasVisitor implements
			AVisitor<AlgebraNode> {
		private Relation newRelation;

		public ReplaceRelationWithAliasVisitor(Relation newRelation) {
			this.newRelation = newRelation;
		}

		@Override
		public AlgebraNode visitNode(Join node) {
			node.visitChildren(this);
			return null;
		}

		@Override
		public AlgebraNode visitNode(Selection node) {
			node.visitChildren(this);
			return null;
		}

		@Override
		public AlgebraNode visitNode(Relation node) {
			return null;
		}

		@Override
		public AlgebraNode visitNode(Union node) {
			node.visitChildren(this);
			return null;
		}

		@Override
		public AlgebraNode visitNode(Fragment node) {
			node.setRelation(newRelation);
			return null;
		}

		@Override
		public AlgebraNode visitNode(Projection node) {
			node.visitChildren(this);
			return null;
		}
	}
}
