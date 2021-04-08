package org.gibello.zql.ast.visitor;

import org.gibello.zql.ast.ZConstant;
import org.gibello.zql.ast.ZExpression;
import org.gibello.zql.ast.ZQuery;

/*
 * A visitor which could traverse the Expression tree
 * TODO add more Expression Node instead of using operator = "+"
 */
public interface ZExpVisitor<T> {
	public T visitNode(ZExpression node);

	public T visitNode(ZConstant node);

	public T visitNode(ZQuery node);
}
