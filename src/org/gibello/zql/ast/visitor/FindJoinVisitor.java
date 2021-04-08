package org.gibello.zql.ast.visitor;

import java.util.*;

import org.gibello.zql.ast.*;

public class FindJoinVisitor implements ZExpVisitor<ZExp> {

	private List<ZExpression> joinList = new ArrayList<ZExpression>();

	@Override
	public ZExp visitNode(ZExpression node) {
		String op = node.getOperator();

		if (op.equals("=")) { //TODO: handle natural join in the future
			ZExp left = node.getOperand(0);
			ZExp right = node.getOperand(1);

			if (left instanceof ZConstant && right instanceof ZConstant) {
				ZConstant leftc = (ZConstant) left;
				ZConstant rightc = (ZConstant) right;

				if (leftc.getType() == ZConstant.COLUMNNAME
						&& rightc.getType() == ZConstant.COLUMNNAME) {
					
					try {
						joinList.add(node.clone());
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					return null;
				}
			}
		}

		// otherwise copy
		ZExpression exp = new ZExpression(op);
		for (int i = 0; i < node.getOperands().size(); i++) {
			ZExp sube = node.getOperand(i).accept(this);
			if (sube != null)
				exp.addOperand(sube);
		}
		
		if (op.equals("AND")) {
			if (exp.getOperands().size() == 1)
				return exp.getOperand(0);
			if (exp.getOperands().size() == 0)
				return null;
		}
		
		return exp;
	}

	@Override
	public ZExp visitNode(ZConstant node) {
		try {
			return node.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ZExp visitNode(ZQuery node) {
		node.getWhere().accept(this);
		return node;
	}

	public List<ZExpression> getJoinList() {
		return joinList;
	}
}
