package edu.cmu.vlis.distributed;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.gibello.zql.ast.*;
import org.gibello.zql.ast.visitor.FindJoinVisitor;
import org.gibello.zql.ast.visitor.NameReplaceVisitor;

import edu.cmu.vlis.distributed.relationalalgebra.*;
import edu.cmu.vlis.distributed.relationalalgebra.Fragment.PartitionType;
import edu.cmu.vlis.distributed.relationalalgebra.visitor.*;

public class Executor {

	private Configuration conf = Configuration.getInstance();

	static private Executor exec = new Executor();

	static public Executor getInstance() {
		return exec;
	}

	private Executor() {

	}
	
	private int cnt = 0;

	/*
	 * Execute a select statement
	 */
	public int ExecuteQuery(ZQuery select) {
		AlgebraNode root = Normalization(select);
		root = Localization(root);
		try {
			PrintStream ps = new PrintStream(new File("output" + File.separator + "res" + (cnt++)));
			ps.println(select.toString());
			ps.println();

			//root.accept(new AlegbraTreePrintVisitor(System.out));
			root.accept(new AlegbraTreePrintVisitor(ps));

			Machine core = conf.getCoordinateMachine();

			root.accept(new ClearUpVisitor(core));
			core.clearExchange();
			
			root.accept(new ExecuteQueryVisitor(core));
		
			//root.accept(new ClearUpVisitor(core));
			//core.clearExchange();

			ps.println();
			core.write(root.getName(), ps);
			core.write(root.getName(), System.out);
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// root.accept(new ClearUpVisitor(conf.getCoordinateMachine()));

		return 0;
	}

	private AlgebraNode Normalization(ZQuery select) {

		Map<String, AlgebraNode> nodeMap = new HashMap<String, AlgebraNode>();

		ZExp where = select.getWhere();

		Projection proj = new Projection();
		AlgebraNode sub = null;

		if (select.getFrom().size() == 1) {
			Vector<ZSelectItem> sels = select.getSelect();
			for (int i = 0; i < sels.size(); i++) {
				ZSelectItem sel = sels.get(i);
				if (sel.getTable() == null) {
					sels.set(i, new ZSelectItem(select.getFrom().get(0).toString() + "." + sel.getColumn()));
				}
			}
			for (ZSelectItem item : select.getSelect())
				proj.addColumn(item);

			sub = new Relation(select.getFrom().get(0));
		} else if (where instanceof ZExpression) {
			for (ZSelectItem item : select.getSelect())
				proj.addColumn(item);

			for (ZFromItem from : select.getFrom()) {
				AlgebraNode node = new Relation(from);
				nodeMap.put(node.getName(), node);
			}

			FindJoinVisitor find = new FindJoinVisitor();
			where = where.accept(find); // get a new copy whose join has been
										// removed

			List<ZExpression> joinList = find.getJoinList();
			for (ZExpression exp : joinList) {
				ZConstant left = (ZConstant) exp.getOperand(0);
				ZConstant right = (ZConstant) exp.getOperand(1);

				AlgebraNode leftNode = nodeMap.get(left.getTable());
				AlgebraNode rightNode = nodeMap.get(right.getTable());

				if (leftNode.getRoot() == rightNode.getRoot()) {
					continue;
				}

				sub = new Join(leftNode.getRoot(), new ZSelectItem(left.toString()), rightNode.getRoot(),
						new ZSelectItem(right.toString()));
				
				nodeMap.put(sub.getName(), sub);
			}
		} else {
			System.err.println("Cannot handle nested query currently");
			return null;
		}

		if (where != null)
			sub = new Selection(sub, where);
		proj.setChild(sub);
		return proj;
	}

	private AlgebraNode Localization(AlgebraNode root) {
		root = root.accept(new RelationToFragmentsVisitor(conf.getAlgebraicNodeMap()));
		root.setFather(null);
		root = root.accept(new PushUnionUpVisitor());
		root.setFather(null);
		root = root.accept(new PushProjectionDownVisitor());
		root.setFather(null);
		root = root.accept(new PushSelectionDownVisitor());
		root.setFather(null);
		root = root.accept(new AlgebraReductionVisitor());
		root.setFather(null);

		SimplifyVisitor simplfier = new SimplifyVisitor();
		while (true) {
			simplfier.setChanged(false);
			root = root.accept(simplfier);
			if (!simplfier.isChanged())
				break;
		}
		return root;
	}

	public int ExceuteInsert(ZInsert insert) {
		String table = insert.getTable();
		AlgebraNode loc = conf.getAlgebraicNodeMap().get(table);
		excInsert(insert, loc);
		return 1;
	}

	public void excInsert(ZInsert s, AlgebraNode root) {
		if (root instanceof Union) {
			Union u = (Union) root;
			for (AlgebraNode c : u.getChildren()) {
				excInsert(s, c);
			}
		} else if (root instanceof Join) {
			Join j = (Join) root;
			excInsert(s, j.getLeft());
			excInsert(s, j.getRight());
		} else if (root instanceof Fragment) {
			Fragment f = (Fragment) root;

			boolean flag = true;
			for (int i = 0; i < s.getValues().size(); i++) {
				ZExpression exp = new ZExpression("=");
				ZConstant con = new ZConstant(s.getTable() + "." + s.getColumns().get(i), ZConstant.COLUMNNAME);

				exp.addOperand(con);
				exp.addOperand(s.getValues().get(i));

				if (ZExpUtils.SimplifyAnd(exp, f.getCondition()) == ZConstant.FALSE) {
					flag = false;
				}
			}
			if (flag == false) { // Nothing to do with this fragment
				return;
			}

			try {
				ZInsert newInsert = s.clone();
				Vector<String> cols = new Vector<String>();
				ZExpression values = new ZExpression(",");

				Vector<String> oldcols = s.getColumns();
				for (int i = 0; i < oldcols.size(); i++) {
					String col = oldcols.get(i);
					if (f.containsColumn(s.getTable() + "." + col)) {
						cols.add(col);
						values.addOperand(s.getValues().get(i));
					}
				}
				if (cols.size() == 0) { // Nothing to do with this fragment
					return;
				}
				newInsert.setColumns(cols);
				newInsert.setValueSpec(values);

				f.getMachine().executeUpdate(newInsert.toString());

				// System.out.println(((Fragment) root).getMachine().getId() +
				// ":" + newInsert.toString());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Problem");
		}
	}
}
