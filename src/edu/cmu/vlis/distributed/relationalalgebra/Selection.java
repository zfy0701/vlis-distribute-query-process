package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.ArrayList;
import java.util.List;

import org.gibello.zql.ast.ZConstant;
import org.gibello.zql.ast.ZExp;
import org.gibello.zql.ast.ZSelectItem;
import org.gibello.zql.ast.visitor.ColumnExtractorVisitor;

import edu.cmu.vlis.distributed.relationalalgebra.visitor.AVisitor;

public class Selection extends AlgebraNode {
	private AlgebraNode child;
	private ZExp condition;
	
	public Selection() {
	}
	
	public Selection(AlgebraNode child, ZExp condition) {
		setChild(child);
		this.condition = condition;
	}

	public ZExp getCondition() {
		return condition;
	}

	public AlgebraNode getChild() {
		return child;
	}
	
	public void setChild(AlgebraNode child) {
		this.child = child;
		this.child.setFather(this);
	}
	
	@Override
	public String toString() {
		return "Selection" + "[" + getName() + "]: "  + (condition != null ? condition.toString() : "");
	}
	
	@Override
	public <T> T accept(AVisitor<T> visitor) {
		return visitor.visitNode(this);
	}

	public void setCondition(ZExp condition) {
		this.condition = condition;
	}



	@Override
	public <T> void visitChildren(AVisitor<T> visitor) {
		child.accept(visitor);
	}
	
	@Override
	public boolean containsRelation(String name) {
		if (this.name.equals(name))
			return true;
		return child.containsRelation(name);
	}

	@Override
	public List<ZSelectItem> getColumns() {
		return child.getColumns();
	}
	
	public List<ZSelectItem> getColumnsInCondition() {
		List<ZSelectItem> res = new ArrayList<ZSelectItem>();
		ColumnExtractorVisitor vis = new ColumnExtractorVisitor();
		this.condition.accept(vis);
		List<ZConstant> tmp = vis.getResult();
		
		for (ZConstant c : tmp) {
			res.add(new ZSelectItem(c.toString()));
		}
		return res;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Selection) {
			Selection s = (Selection) o;
			return this.child.equals(s.child) && this.condition.equals(s.condition);
		}
		return false;
	}
}
