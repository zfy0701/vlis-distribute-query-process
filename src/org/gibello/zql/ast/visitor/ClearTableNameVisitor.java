package org.gibello.zql.ast.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

public class ClearTableNameVisitor implements ZExpVisitor<ZExp> {
	@Override
	public ZExp visitNode(ZExpression node) {
		ZExpression newNode = new ZExpression(node.getOperator());
		for (ZExp subexp : node.getOperands()) {
			newNode.addOperand(subexp.accept(this));
		}
		return newNode;
	}

	@Override
	public ZExp visitNode(ZConstant node) {
		if (node.getType() == ZConstant.COLUMNNAME) {
			return new ZConstant(node.getColumn(), ZConstant.COLUMNNAME);
		}
		return node;
	}

	@Override
	public ZExp visitNode(ZQuery node) {
		System.out.println(node.toString());
		node.getWhere().accept(this);
		return node;
	}

}
