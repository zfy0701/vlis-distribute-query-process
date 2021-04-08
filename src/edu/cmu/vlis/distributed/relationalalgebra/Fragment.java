package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.*;

import org.gibello.zql.ast.*;

import edu.cmu.vlis.distributed.*;
import edu.cmu.vlis.distributed.relationalalgebra.visitor.*;

public class Fragment extends AlgebraNode {
	
	public enum PartitionType {
		Horizontal,
		Vertical,
		None,
	}
	
	private PartitionType type = PartitionType.None;
	
	//schema
	private ZExp condition = ZConstant.TRUE;
	
	private Relation relation;
	
	private String tableSchema;
	
	private Machine machine;
	
	private List<ZSelectItem> columns = new ArrayList<ZSelectItem>();  //if it is not null, then this is a vertical fragment, and the first element should be the key
	
	public PartitionType getPartitionType() {
		return type;
	}
	
	public Fragment(Relation rel, Machine m, PartitionType type) {
		this.relation = rel;
		this.machine = m;
		this.type = type;
	}
	
	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(relation.getName() + machine.getId() + "[" + this.name + "]: {");
		for (ZSelectItem col : columns) {
			sb.append(col.toString());
			sb.append(", ");
		}
		sb.append("} ");
		
		sb.append(condition != null ? condition.toString() : "");
		return sb.toString();
	}

	public ZExp getCondition() {
		return condition;
	}

	public void setCondition(ZExp condition) {
		this.condition = condition;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}
	

	public String getTableSchema() {
		return this.tableSchema;
	}
	
	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
		
	}

	@Override
	public boolean containsRelation(String name) {
		return relation.getName().equals(name);
	}

	public Relation getRelation() {
		return relation;
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
	}

	public List<ZSelectItem> getColumns() {
		return columns;
	}

	public void setColumns(List<ZSelectItem> columns) {
		this.columns = columns;
	}
	
	public void addColumn(ZSelectItem item) {
		if (!containsColumn(item.toString()))
			columns.add(item);
	}
	
	public boolean containsColumn(String c) {
		for (ZSelectItem col : columns) {
			if (col.toString().equals(c)) {
				return true;
			}
		}
		return false;
	}
	
	public ZSelectItem getColumn(int i) {
		return columns.get(i);
	}
	
	public Machine getMachine() {
		return this.machine;
	}
	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Fragment) {
			Fragment f = (Fragment) o;
			if (this.relation.getName().equals(f.relation.getName()) && this.machine == f.machine) {
				return true;
			}
		}
		return false;
		
	}
}
