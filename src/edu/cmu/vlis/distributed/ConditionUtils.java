package edu.cmu.vlis.distributed;

import org.gibello.zql.ast.*;

//suport operator =, >, >=, <, <= to do and each other
//TODO add !=
public class ConditionUtils {
	private static ZExpression e1, e2;
	private static ZConstant f1, f2;
	private static ZConstant v1, v2;
	private static Comparable c1, c2;
	private static String op1, op2;

	private static void swapAll() {
		ZExpression et = e1;
		e1 = e2;
		e2 = et;
		ZConstant ft = f1;
		f1 = f2;
		f2 = ft;
		ZConstant vt = v1;
		v1 = v2;
		v2 = vt;
		Comparable ct = c1;
		c1 = c2;
		c2 = ct;
		String opt = op1;
		op1 = op2;
		op2 = opt;
	}

	// assume this two ZExpressions are not "AND" expression themselves
	//so it should like a : id > 9 b: id = 7
	public static ZExp mergeCondition(ZExpression a, ZExpression b) {
		ZExp res = SimplifyTwo(a, b);
		if (res == null) {
			ZExpression res2 = new ZExpression("AND");
			res2.addOperand(a);
			res2.addOperand(b);
			return res2;
		} else {
			return res;
		}
	}

	// if cannot simplify two, return null
	public static ZExp SimplifyTwo(ZExpression a, ZExpression b) {
		e1 = a;
		f1 = (ZConstant) a.getOperand(0);
		v1 = (ZConstant) a.getOperand(1);
		op1 = a.getOperator();

		e2 = b;
		f2 = (ZConstant) b.getOperand(0);
		v2 = (ZConstant) b.getOperand(1);
		op2 = b.getOperator();

		if (v1.getType() == ZConstant.NUMBER) {
			c1 = Integer.valueOf(v1.getValue());
			c2 = Integer.valueOf(v2.getValue());
		} else if (v1.getType() == ZConstant.STRING) {
			c1 = v1.getValue();
			c2 = v2.getValue();
		}

		if (!f1.toString().equals(f2.toString()))
			return null;

		if (op1.equals("=")) {
			return EqAndOther();
		} else if (op2.equals("=")) {
			return OtherAndEq();
		} else if (op1.equals(">") && op2.equals(">")) {
			return GreaterAndGreater();
		} else if (op1.equals(">") && op2.equals(">=")) {
			return GreaterAndGreaterEq();
		} else if (op1.equals(">") && op2.equals("<")) {
			return GreaterAndLess();
		} else if (op1.equals(">") && op2.equals("<=")) {
			return GreaterEqAndLessEq();
		} else if (op1.equals(">=") && op2.equals(">")) {
			return GreaterEqAndGreater();
		} else if (op1.equals(">=") && op2.equals(">=")) {
			return GreaterEqAndGreaterEq();
		} else if (op1.equals(">=") && op2.equals("<")) {
			return GreaterEqAndLess();
		} else if (op1.equals(">=") && op2.equals("<=")) {
			return GreaterEqAndLessEq();
		} else if (op1.equals("<") && op2.equals(">")) {
			return LessAndGreater();
		} else if (op1.equals("<") && op2.equals(">=")) {
			return LessAndGreaterEq();
		} else if (op1.equals("<") && op2.equals("<")) {
			return LessAndLess();
		} else if (op1.equals("<") && op2.equals("<=")) {
			return LessAndLessEq();
		} else if (op1.equals("<=") && op2.equals(">")) {
			return LessEqAndGreater();
		} else if (op1.equals("<=") && op2.equals(">=")) {
			return LessEqAndGreaterEq();
		} else if (op1.equals("<=") && op2.equals("<")) {
			return LessEqAndLess();
		} else if (op1.equals("<=") && op2.equals("<=")) {
			return LessEqAndLessEq();
		}
		return null;
	}

	public static ZExp EqAndOther() {
		if (c1.compareTo(c2) == 0) {
			if (op2.contains("="))
				return e1;
		} else if (c1.compareTo(c2) > 0) {
			if (op2.contains(">"))
				return e1;
		} else {
			if (op2.contains("<"))
				return e1;
		}
		return ZConstant.FALSE;
	}

	public static ZExp OtherAndEq() {
		swapAll();
		return EqAndOther();
	}

	public static ZExp GreaterAndGreater() {
		if (c1.compareTo(c2) >= 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp GreaterEqAndGreaterEq() {
		if (c1.compareTo(c2) >= 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp GreaterEqAndGreater() {
		if (c1.compareTo(c2) > 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp GreaterAndGreaterEq() {
		swapAll();
		return GreaterAndGreaterEq();
	}

	public static ZExp LessAndLess() {
		if (c1.compareTo(c2) <= 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp LessEqAndLessEq() {
		if (c1.compareTo(c2) <= 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp LessEqAndLess() {
		if (c1.compareTo(c2) < 0) {
			return e1;
		} else {
			return e2;
		}
	}

	public static ZExp LessAndLessEq() {
		swapAll();
		return LessEqAndLess();
	}

	public static ZExp GreaterAndLess() {
		if (c1.compareTo(c2) >= 0) {
			return ZConstant.FALSE;
		} else {
			return null;
		}
	}

	public static ZExp LessAndGreater() {
		swapAll();
		return GreaterAndLess();
	}

	public static ZExp GreaterEqAndLessEq() {
		if (c1.compareTo(c2) > 0) {
			return ZConstant.FALSE;
		} else if (c1.compareTo(c2) < 0) {
			return null;
		} else {
			ZExpression eq = new ZExpression("=");
			eq.addOperand(f1);
			eq.addOperand(v1);
			return eq;
		}
	}

	public static ZExp LessEqAndGreaterEq() {
		swapAll();
		return GreaterEqAndLessEq();
	}

	public static ZExp GreaterEqAndLess() {
		if (c1.compareTo(c2) >= 0) {
			return ZConstant.FALSE;
		} else {
			return null;
		}
	}

	public static ZExp LessAndGreaterEq() {
		swapAll();
		return GreaterEqAndLess();
	}

	public static ZExp GreaterAndLessEq() {
		if (c1.compareTo(c2) >= 0) {
			return ZConstant.FALSE;
		} else {
			return null;
		}
	}

	public static ZExp LessEqAndGreater() {
		swapAll();
		return GreaterAndLessEq();
	}
}
