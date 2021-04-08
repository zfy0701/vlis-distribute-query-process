package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.List;

import org.gibello.zql.ast.ZFromItem;
import org.gibello.zql.ast.ZSelectItem;

import edu.cmu.vlis.distributed.relationalalgebra.visitor.AVisitor;

public class Relation extends AlgebraNode {
	private String table;
	private String alias;
	
	public Relation(String table) {
		this.table = table;
		this.alias = null;
	}
	
	public Relation(String table, String alias) {
		this.table = table;
		this.alias = alias;
	}
	
	public Relation(ZFromItem fromItem) {		
		this.table = fromItem.getTable();
		this.alias = fromItem.getAlias();
	}

	public String getName() {
		if (alias != null)
			return alias;
		return table;
	}

	public String getTable() {
		return table;
	}

	public String getAlias() {
		return alias;
	}
	
	@Override
	public String toString() {
		if (alias != null) {
			return alias + "(" + table + ") [" + this.name + "]";
		} 
		return table + "[" + this.name + "]";
	}
	
	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}

	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
	
	}

	@Override
	public boolean containsRelation(String name) {
		return getName().equals(name);
	}

	@Override
	public List<ZSelectItem> getColumns() {
		//This return null, because only when the fragment is loaded, when know the schema
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Relation) {
			Relation r = (Relation) o;
			return this.table.equals(r.table) && this.alias.equals(r.alias);
		}
		return false;
	}
}
