package edu.cmu.vlis.distributed.relationalalgebra.visitor;

import edu.cmu.vlis.distributed.relationalalgebra.*;


public interface AVisitor<T> {
	public T visitNode(Join node);

	public T visitNode(Selection node);

	public T visitNode(Relation node);
	
	public T visitNode(Union node);
	
	public T visitNode(Fragment node);
	
	public T visitNode(Projection node);
}
