package edu.cmu.vlis.distributed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gibello.zql.ast.ZConstant;
import org.gibello.zql.ast.ZSelectItem;
import org.gibello.zql.parser.ParseException;
import org.gibello.zql.parser.ZqlParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.vlis.distributed.relationalalgebra.AlgebraNode;
import edu.cmu.vlis.distributed.relationalalgebra.Fragment;
import edu.cmu.vlis.distributed.relationalalgebra.Fragment.PartitionType;
import edu.cmu.vlis.distributed.relationalalgebra.Join;
import edu.cmu.vlis.distributed.relationalalgebra.Relation;
import edu.cmu.vlis.distributed.relationalalgebra.Union;

public class Configuration {
	private ZqlParser parser = ZqlParser.getInstance();
	List<Machine> machineList = null;
	HashMap<String, AlgebraNode> algebraicNodeMap = null;
	HashMap<String, HashMap<String, String>> schemaStore = new HashMap<String, HashMap<String, String>>();
	Document doc = null;
	private Machine coordinateMachine;

	private Configuration() {
	}
	
	private static Configuration conf = new Configuration();
	
	static public Configuration getInstance() {
		return conf;
	}
	
	public Document readXMLDoc() {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document doc = null;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File("res" + File.separator + "FragmentConfig.xml"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return doc;
	}

	private List<Machine> getMachineList() {
		if (doc == null)
			doc = readXMLDoc();

		machineList = new ArrayList<Machine>();
		NodeList listOfMachines = doc.getElementsByTagName("Machine");
		int totalMachines = listOfMachines.getLength();

		for (int m = 0; m < totalMachines; m++) {

			Node machineNode = listOfMachines.item(m);
			if (machineNode.getNodeType() == Node.ELEMENT_NODE) {

				Element machineElement = (Element) machineNode;

				int machineId = Integer.parseInt(machineElement.getElementsByTagName("ID").item(0).getFirstChild()
						.getNodeValue().trim());

				String connectionString = machineElement.getElementsByTagName("ConnectionString").item(0)
						.getFirstChild().getNodeValue().trim();

				String userName = machineElement.getElementsByTagName("UserName").item(0).getFirstChild()
						.getNodeValue().trim();
				String password = machineElement.getElementsByTagName("Password").item(0).getFirstChild()
						.getNodeValue().trim();

				Machine machine = new Machine(machineId, connectionString, userName, password);
				machine.setConnectionString(connectionString);
				
				machineList.add(machine);

			}
		}
		return machineList;

	}

	private String createTable(List<ZSelectItem> list, Relation relation, int machineId) {
		HashMap schemaTable = schemaStore.get(relation.getName());

		String temp = "CREATE Table " + relation.getName();
		StringBuilder sql = new StringBuilder("( ");

		for (int index = 0; index < list.size() - 1; index++) {
			sql.append(list.get(index).getColumn()).append(" ").append(schemaTable.get(list.get(index).getColumn()))
					.append(" , ");
		}
		sql.append(list.get(list.size() - 1).getColumn()).append(" ")
				.append(schemaTable.get(list.get(list.size() - 1).getColumn())).append(")");

		String createCommand = sql.toString();

		machineList.get(machineId).dropTableIfExists(relation.getName());
		machineList.get(machineId).executeUpdate(temp + createCommand);

		return createCommand;
	}

	private AlgebraNode getFragment(Node node, Relation relation, boolean isVertical, List<ZSelectItem> prevlist,
			String prevCondition) {
		if (node.getNodeType() != Node.ELEMENT_NODE)
			return null;

		Element element = (Element) node;

		String values = element.getElementsByTagName("Values").item(0).getFirstChild().getNodeValue().trim();

		NodeList nodeList = element.getChildNodes();
		List<ZSelectItem> list = new ArrayList<ZSelectItem>();
		String condition = null;

		if (isVertical) // If Vertical Fragment
		{

			String columns[] = values.split(",");

			for (int i = 0; i < columns.length; i++)
				list.add(new ZSelectItem(columns[i].trim()));

			condition = prevCondition;

		} else // If Horizontal Fragment
		{
			condition = values;

			if (prevCondition != null)
				condition = condition + " AND " + prevCondition;

			list = prevlist;
		}

		for (int index = 0; index < nodeList.getLength(); index++) {
			if (!nodeList.item(index).getNodeName().equals("Fragments"))
				continue;

			return getAlgebraicNode(nodeList.item(index), relation, list, condition);

		}

		// In case it is not nested fragment
		int machineId = Integer.parseInt(element.getElementsByTagName("MachineID").item(0).getFirstChild()
				.getNodeValue().trim());

		PartitionType p = (isVertical) ? PartitionType.Vertical : PartitionType.Horizontal;
		Fragment frag = new Fragment(relation, machineList.get(machineId), p);

		if (list != null)
			frag.setTableSchema(createTable(list, relation, machineId));

		try {
			//System.out.println("sql Condition " + condition);
			if (condition != null) {
				parser.initParser(new ByteArrayInputStream(condition.getBytes()));
				frag.setCondition(parser.readExpression());
			} else {
				frag.setCondition(ZConstant.TRUE);
			}
			//System.out.println(frag.getCondition());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (list != null)
			for (int index = 0; index < list.size(); index++) {
				frag.addColumn(list.get(index));
				//System.out.println(list.get(index).getColumn());
			}

		machineList.get(machineId).fragmentList.add(frag);
		return frag;

	}

	private AlgebraNode getAlgebraicNode(Node fragmentNode, Relation rel, List<ZSelectItem> list, String condition) {

		if (fragmentNode.getNodeType() == Node.ELEMENT_NODE) {

			Element tableElement = (Element) fragmentNode;

			String type = tableElement.getAttributeNode("type").getNodeValue();
			boolean isVertical = type.equals("Vertical") ? true : false;

			String key = null;

			if (isVertical)
				key = tableElement.getAttributeNode("key").getNodeValue();

			NodeList nodeList = tableElement.getChildNodes();
			List<AlgebraNode> fragmentList = new ArrayList<AlgebraNode>();

			for (int index = 0; index < nodeList.getLength(); index++) {
				if (!nodeList.item(index).getNodeName().equals("Fragment"))
					continue;

				AlgebraNode f = getFragment(nodeList.item(index), rel, isVertical, list, condition);

				fragmentList.add(f);
			}

			if (fragmentList.size() == 1)
				return fragmentList.get(0);

			AlgebraNode result = null;

			if (isVertical) {
				result = new Join(fragmentList.get(0), new ZSelectItem(key), fragmentList.get(1), new ZSelectItem(key));
				//result = new Join(fragmentList.get(0), new ZSelectItem(fragmentList.get(0).getName() + "." + key), fragmentList.get(1), new ZSelectItem(fragmentList.get(1).getName() + "." + key));

				for (int index = 2; index < fragmentList.size(); index++) {
					Join newJ = new Join(result, new ZSelectItem(key), fragmentList.get(index), new ZSelectItem(key));
					result = newJ;
				}

				((Join) result).setVerticalFragmented(true);

				return result;
			} else {
				result = new Union();

				for (int index = 0; index < fragmentList.size(); index++)
					((Union) result).addChildren(fragmentList.get(index));

				return result;
			}

		}

		return null;
	}

	public HashMap<String, AlgebraNode> getAlgebraicNodeMap() {
		if (machineList == null)
			this.getMachineList();

		if (algebraicNodeMap != null)
			return algebraicNodeMap;

		algebraicNodeMap = new HashMap<String, AlgebraNode>();
		NodeList listOfTables = doc.getElementsByTagName("Table");
		int total = listOfTables.getLength();

		for (int m = 0; m < total; m++) {

			Node table = listOfTables.item(m);

			if (table.getNodeType() == Node.ELEMENT_NODE) {

				Element tableElement = (Element) table;
				NodeList attributes = tableElement.getElementsByTagName("attribute");
				String tableName = tableElement.getAttributeNode("name").getNodeValue();

				HashMap<String, String> tableSchema = new HashMap<String, String>();

				List<ZSelectItem> listColumns = new ArrayList<ZSelectItem>();

				for (int att = 0; att < attributes.getLength(); att++) {
					String values[] = attributes.item(att).getFirstChild().getNodeValue().trim().split("\\s+");
					tableSchema.put(values[0].trim(), values[1].trim());
					listColumns.add(new ZSelectItem(tableName + "." + values[0].trim()));
				}

				schemaStore.put(tableName, tableSchema);

				AlgebraNode algebraicNode = getAlgebraicNode(tableElement.getElementsByTagName("Fragments").item(0),
						new Relation(tableName), listColumns, null);

				algebraicNodeMap.put(tableName, algebraicNode);

			}

		}
		return algebraicNodeMap;
	}

	public Machine getCoordinateMachine() {
		if (doc == null)
			doc = readXMLDoc();

		Node cMachine = doc.getElementsByTagName("CoordinateMachine").item(0);

		Element machineElement = (Element) cMachine;

		String connectionString = machineElement.getElementsByTagName("ConnectionString").item(0).getFirstChild()
				.getNodeValue().trim();

		String userName = machineElement.getElementsByTagName("UserName").item(0).getFirstChild().getNodeValue().trim();
		String password = machineElement.getElementsByTagName("Password").item(0).getFirstChild().getNodeValue().trim();

		coordinateMachine = new Machine(-1, connectionString, userName, password);
		return coordinateMachine;

	}

	public static void main(String args[]) throws SQLException {

		Configuration xmlP = new Configuration();
		Machine m = xmlP.getCoordinateMachine();
		xmlP.getAlgebraicNodeMap();

		/*for(int index =0 ;index < xmlP.machineList.size(); index++ )
		{
			System.out.println("\n\nmachine"+index);
			Machine m1 = xmlP.machineList.get(index);
			List<Fragment> lf = m1.fragmentList;
			for(int pos =0 ;pos < lf.size();pos++)
			{
				System.out.println(lf.get(pos).getRelation().getTable());
				System.out.println(lf.get(pos).getTableSchema());
			}
		}*/
			
		
		// checking exchange operator
		xmlP.machineList.get(0).exchangeTo(xmlP.machineList.get(1), "item", "temp");

	}
	

}
