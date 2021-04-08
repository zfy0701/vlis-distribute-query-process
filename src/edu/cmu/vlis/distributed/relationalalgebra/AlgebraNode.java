package edu.cmu.vlis.distributed.relationalalgebra;

import java.util.List;

import org.gibello.zql.ast.ZSelectItem;

import edu.cmu.vlis.distributed.IdGenerator;
import edu.cmu.vlis.distributed.relationalalgebra.visitor.*;

public abstract class AlgebraNode implements Cloneable {

	/*
	 * The name of the node, if this is not a table or alias, it will get a random generated name
	 */
	protected String name;
	
	public AlgebraNode() {
		name = IdGenerator.generatorId();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	private AlgebraNode father = null;
	
	/*
	 * get the root of the tree
	 */
	public AlgebraNode getRoot() {
		AlgebraNode res = this;
		while (res.getFather() != null) {
			res = res.getFather();
		}
		return res;
	}
	
	public AlgebraNode getFather() {
		return father;
	}
	
	public void setFather(AlgebraNode father) {
		this.father = father;
	}
		
    public AlgebraNode clone() throws CloneNotSupportedException {
    	AlgebraNode res = (AlgebraNode) super.clone();
    	res.name = IdGenerator.generatorId();
    	return res;
    }
    
    /*
     * get all the columns of the relation
     */
    abstract public List<ZSelectItem> getColumns();
    
	abstract public <T> T accept(AVisitor<T> visitor);
	
	abstract public <T> void visitChildren(AVisitor<T> visitor);
	
	/*
	 * test if this node has a leave node of Relation name
	 */
	abstract public boolean containsRelation(String name);
	
	
	//TODO should also write hashCode in the future
	
	@Override
	abstract public boolean equals(Object o);
}
