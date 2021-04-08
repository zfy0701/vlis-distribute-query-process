package org.gibello.zql.ast.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

public class NameReplaceVisitor implements ZExpVisitor<ZExp> {

	private String oldName, newName;

	public NameReplaceVisitor(String oldName, String newName) {
		this.oldName = oldName;
		this.newName = newName;
	}

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
		if (node.getType() == ZConstant.COLUMNNAME && node.getTable().equals(oldName)) {
						
			try {
				ZConstant newNode = node.clone();
				newNode.setTable(newName);
				return newNode;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
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
