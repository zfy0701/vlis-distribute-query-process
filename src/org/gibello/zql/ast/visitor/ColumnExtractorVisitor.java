package org.gibello.zql.ast.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

public class ColumnExtractorVisitor implements ZExpVisitor<ZExp> {
	private List<ZConstant> cols = new ArrayList<ZConstant>();
	
	public List<ZConstant> getResult() {
		return cols;
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
		if (node.getType() == ZConstant.COLUMNNAME) {
			if (!cols.contains(node)) {
				cols.add(node);
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
