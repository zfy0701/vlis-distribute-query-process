/*
 * This file is part of Zql.
 *
 * Zql is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zql is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Zql.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gibello.zql.ast;

import java.io.*;
import java.util.*;

import org.gibello.zql.ast.visitor.ZExpVisitor;

/**
 * ZConstant: a representation of SQL constants
 */
public class ZConstant implements ZExp, Cloneable {

	public static ZExp TRUE = new ZConstant("1", ZConstant.NUMBER);

	public static ZExp FALSE = new ZConstant("0", ZConstant.NUMBER);

	/**
	 * ZConstant types
	 */
	public static final int UNKNOWN = -1;
	public static final int COLUMNNAME = 0;
	public static final int NULL = 1;
	public static final int NUMBER = 2;
	public static final int STRING = 3;

	int type_ = ZConstant.UNKNOWN;
	String val_ = null;

	/**
	 * Create a new constant, given its name and type.
	 */
	public ZConstant(String v, int typ) {
		val_ = new String(v);
		type_ = typ;
	}

	/*
	 * @return the constant value
	 */
	public String getValue() {
		return val_;
	}

	/*
	 * @return the constant type
	 */
	public int getType() {
		return type_;
	}

	public String toString() {
		if (type_ == STRING)
			return '\'' + val_ + '\'';
		else
			return val_;
	}

	@Override
	public <T> T accept(ZExpVisitor<T> visitor) {
		return visitor.visitNode(this);
	}
	
	
	public String getTable() {
		if (this.getType() != ZConstant.COLUMNNAME)
			throw new RuntimeException("not column");
		return this.getValue().split("\\.")[0];
	}
	
	public void setTable(String newVal) {
		this.val_ = newVal + "." + getColumn();
	}
	
	public String getColumn() {
		if (this.getType() != ZConstant.COLUMNNAME)
			throw new RuntimeException("not column");
		return this.getValue().split("\\.")[1];
	}
	
    public ZConstant clone() throws CloneNotSupportedException {
        return (ZConstant) super.clone();
    }
};
