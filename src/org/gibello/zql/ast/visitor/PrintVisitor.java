package org.gibello.zql.ast.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

public class PrintVisitor implements ZExpVisitor<ZExp> {
	

	@Override
	public ZExp visitNode(ZExpression node) {
		System.out.println(node.toString());
		
		for (ZExp subexp : node.getOperands()) {
			subexp.accept(this);
		}
		
		return node;
	}

	@Override
	public ZExp visitNode(ZConstant node) {
		System.out.println(node.toString());
		return node;
	}

	@Override
	public ZExp visitNode(ZQuery node) {
		System.out.println(node.toString());
		node.getWhere().accept(this);
		return node;
	}

}
