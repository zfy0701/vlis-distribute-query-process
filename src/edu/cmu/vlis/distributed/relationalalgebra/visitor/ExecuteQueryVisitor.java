package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.sql.SQLException;
import java.util.*;

import org.gibello.zql.ast.*;
import org.gibello.zql.ast.visitor.ClearTableNameVisitor;

import edu.cmu.vlis.distributed.*;
import edu.cmu.vlis.distributed.relationalalgebra.*;

public class ExecuteQueryVisitor implements AVisitor<Integer> {
	private Machine core = null;
	
	public ExecuteQueryVisitor(Machine core) {
		this.core = core;
	}
	
	private Set<AlgebraNode> used = new HashSet<AlgebraNode>();

	@Override
	public Integer visitNode(Join node) {
		if (used.contains(node)) return null;
		else used.add(node);
		
		AlgebraNode left = node.getLeft();
		AlgebraNode right = node.getRight();
		left.accept(this);
		right.accept(this);
		String cond = left.getName() + "." + node.getLeftColumn().getColumn() + " = " + right.getName() + "." + node.getRightColumn().getColumn();
		try {
			core.join(left.getName(), right.getName(), node.getName(), cond, node.getLeft().getColumns(), node.getRight().getColumns());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Integer visitNode(Selection node) {
		if (used.contains(node)) return null;
		else used.add(node);

		node.getChild().accept(this);
		try {
			//String cond = node.getCondition().toString().replace(arg0, arg1);
			String cond = node.getCondition().accept(new ClearTableNameVisitor()).toString();
			core.select(node.getChild().getName(), node.getName(), cond);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Integer visitNode(Relation node) {
		return null;
	}

	@Override
	public Integer visitNode(Union node) {
		if (used.contains(node)) return null;
		else used.add(node);

		List<String> src = new ArrayList<String>();
		for (AlgebraNode child : node.getChildren()) {
			child.accept(this);
			src.add(child.getName());
		}
		try {
			core.union(src, node.getName());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Integer visitNode(Fragment node) {
		if (used.contains(node)) return null;
		else used.add(node);

		try {
			node.getMachine().exchangeTo(core, node.getRelation().getTable(), node.getName());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Integer visitNode(Projection node) {
		if (used.contains(node)) return null;
		else used.add(node);

		node.getChild().accept(this);
		try {
			core.project(node.getChild().getName(), node.getName(), node.getColumns());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
