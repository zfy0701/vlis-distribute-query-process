package edu.cmu.vlis.distributed;


import java.io.*;
import java.util.*;

import org.gibello.zql.ast.*;
import org.gibello.zql.parser.*;

import edu.cmu.vlis.distributed.relationalalgebra.*;
import edu.cmu.vlis.distributed.relationalalgebra.visitor.*;

public class Main {

	public static void main(String[] args) throws Exception {
		ZqlParser parser = ZqlParser.getInstance();
		parser.initParser(new FileInputStream(new File("input")));

		//generate SQL parse tree
		Vector<ZStatement> stats = parser.readStatements();

		for (ZStatement stat : stats) {
			Executor exec = Executor.getInstance();
			
			System.out.println(stat.toString());
			
			//execute queries or inserts
			if (stat instanceof ZQuery) {
				exec.ExecuteQuery((ZQuery) stat);
			} else {
				exec.ExceuteInsert((ZInsert) stat);
			}
		}
	}

}
