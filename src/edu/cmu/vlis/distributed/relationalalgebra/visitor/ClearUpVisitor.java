package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import java.sql.SQLException;
import java.util.*;

import org.gibello.zql.ast.*;
import org.gibello.zql.ast.visitor.ClearTableNameVisitor;

import edu.cmu.vlis.distributed.*;
import edu.cmu.vlis.distributed.relationalalgebra.*;

public class ClearUpVisitor implements AVisitor<Integer> {
	private Machine core = null;
	
	public ClearUpVisitor(Machine core) {
		this.core = core;
	}

	@Override
	public Integer visitNode(Join node) {
		core.dropTableIfExists(node.getName());
		AlgebraNode left = node.getLeft();
		AlgebraNode right = node.getRight();
		left.accept(this);
		right.accept(this);
		return null;
	}

	@Override
	public Integer visitNode(Selection node) {
		core.dropTableIfExists(node.getName());
		node.getChild().accept(this);
		return null;
	}

	@Override
	public Integer visitNode(Relation node) {
		return null;
	}

	@Override
	public Integer visitNode(Union node) {
		core.dropTableIfExists(node.getName());
		for (AlgebraNode child : node.getChildren()) {
			child.accept(this);
		}
		return null;
	}

	@Override
	public Integer visitNode(Fragment node) {
		core.dropTableIfExists(node.getName());
		return null;
	}

	@Override
	public Integer visitNode(Projection node) {
		core.dropTableIfExists(node.getName());
		node.getChild().accept(this);
		return null;
	}
}
