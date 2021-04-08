package edu.cmu.vlis.distributed;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.relationalalgebra.*;

public class ZExpUtils {
	public static String getTable(String col) {
		return col.split("\\.")[0];
	}
		
	public static boolean isAnd(ZExp e) {
		if (e instanceof ZExpression) {
			if (((ZExpression) e).getOperator().equals("AND"))
				return true;
		}
		return false;
	}

	/*
	 * return the the simplified ZExp for a AND b
	 * the result will be fully simplified, e.g. if you have (a > 5) and (a = 4), it will only return a = 4
	 * it will return null if a AND b cannot be simplified
	 */
	public static ZExp SimplifyAnd(ZExp a, ZExp b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		ZExpression res = new ZExpression("AND");
		res.addOperand(a);
		res.addOperand(b);
		return simplify(res);
	}
	

	/*
	 * remove Redandunt And expression
	 */
	public static ZExp removeRedanduntAnd(ZExpression and) {
		if (and.getOperands().size() == 0) {
			return null;
		} else if (and.getOperands().size() == 1) {
			ZExp e = and.getOperand(0);
			if (isAnd(e)) {
				return removeRedanduntAnd((ZExpression)e);
			}
			return e;
		} else {
			ZExpression res = new ZExpression("AND");		
			for (ZExp e : and.getOperands()) {
				if (isAnd(e)) {
					ZExpression e2 = (ZExpression) removeRedanduntAnd((ZExpression) e);
					res.getOperands().addAll(e2.getOperands());
				} else {
					res.addOperand(e);
				}
			}
			return res;
		}
	}

	/*
	 * simplify a ZExp, return null if nothing to be simplfied
	 */
	public static ZExp simplify(ZExp exp) {
		if (exp instanceof ZExpression) {
			ZExpression expr = (ZExpression) exp;
			if (!expr.getOperator().equals("AND")) {
				return exp;
			}

			List<ZExpression> exprs	= new ArrayList<ZExpression>();
			
			ZExp norAnd = removeRedanduntAnd(expr);
			if (!isAnd(norAnd)) {
				return norAnd;
			}
			expr = (ZExpression) norAnd;

			for (ZExp cond : expr.getOperands()) {
				if (cond instanceof ZExpression) {	
					exprs.add((ZExpression)cond);
				} else if (cond == ZConstant.FALSE) {
					return ZConstant.FALSE;
				} else if (cond == ZConstant.TRUE) {
					//Do not add true
				} else {
					System.err.println("violate the rule: should be expression here");
				}
			}			
						
			boolean flag = true;
			while (flag) {
				flag = false;
				
				for (int i = 0; i < exprs.size(); i++) {
					for (int j = i + 1; j < exprs.size(); j++) {
						ZExpression a = exprs.get(i);
						ZExpression b = exprs.get(j);
						ZExp tmp = ConditionUtils.SimplifyTwo(a, b);
						if (tmp != null) {
							flag = true;
							i = exprs.size();
							j = exprs.size();
							
							exprs.remove(a);
							exprs.remove(b);
							if (tmp == ZConstant.FALSE) {
								return tmp;
							} else {
								exprs.add((ZExpression)tmp);
							}
						}
					}
				}

			}
			if (exprs.size() == 1)
				return exprs.get(0);
			if (exprs.size() == 0)
				return null;
			
			ZExpression res = new ZExpression("AND");
			for (ZExpression e : exprs) {
				res.addOperand(e);
			}
			return res;			
		} else {
			return exp;
		}
	}
}
