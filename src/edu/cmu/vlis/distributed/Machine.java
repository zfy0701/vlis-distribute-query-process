package edu.cmu.vlis.distributed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.gibello.zql.ast.ZSelectItem;
import org.postgresql.util.PSQLException;

import edu.cmu.vlis.distributed.relationalalgebra.Fragment;

public class Machine {
	private int id;
	private String connectionString;
	private String userName;
	private String password;
	private String jdbcDriver = "org.postgresql.Driver";
	public List<Fragment> fragmentList = new ArrayList<Fragment>();

	Connection conn = null;

	public Machine(int id) {
		this.id = id;
	}

	public Machine(int id, String connectionString, String userName, String password) {
		this.id = id;
		this.connectionString = connectionString;
		this.userName = userName;
		this.password = password;
	}

	public int getId() {
		return id;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getConnectionString() {
		return connectionString;
	}

	public void setConnectionString(String connectionString) {
		this.connectionString = connectionString;
	}

	public Connection getSqlConnection() throws Exception {
		if (conn == null) {
			Class.forName(jdbcDriver);
			conn = DriverManager.getConnection(this.connectionString, this.userName, this.password);
		}
		return conn;
	}

	public void dropTableIfExists(String item) {
		Connection con;
		try {
			con = this.getSqlConnection();
			String commandView = "Drop View "+item+" Cascade ";
			String commandTable = "Drop Table "+item+" Cascade ";
			
			try{
			con.equals(commandView);
			Statement st = con.createStatement();
			st.executeUpdate(commandView);
			st.close();
			}catch (Exception e) {
				if (!e.getMessage().contains("does not exist") && !e.getMessage().contains("is not a view")) {
					e.printStackTrace();
				}
			}
			
			try{
			con.equals(commandTable);
			Statement st = con.createStatement();
			st.executeUpdate(commandTable);
			st.close();
			}catch (Exception e) {
				if (!e.getMessage().contains("does not exist")  && !e.getMessage().contains("is not a table")) {
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {

				e.printStackTrace();
			}
	}

	public void executeUpdate(String sql) {

		Connection con;
		try {
			con = this.getSqlConnection();
			con.equals(sql);
			Statement st = con.createStatement();
			st.executeUpdate(sql);
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(sql);

		}
	}

	public ResultSet executeQuery(String sql) {

		Connection con;
		try {
			con = this.getSqlConnection();
			con.equals(sql);
			Statement st = con.createStatement();
			return st.executeQuery(sql);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void storeResult(ResultSet rs, String dst) throws SQLException {
		ResultSetMetaData rsMetaData = rs.getMetaData();
		String sqlCommandDes = "Insert into " + dst + " Values( ";
		int numberOfColumns = rsMetaData.getColumnCount();

		boolean isChar[] = new boolean[numberOfColumns];
		for (int i = 0; i < numberOfColumns; i++) {

			if (rsMetaData.getColumnTypeName(i + 1).equalsIgnoreCase("Varchar")
					|| rsMetaData.getColumnTypeName(i + 1).equalsIgnoreCase("char"))
				isChar[i] = true;
			else
				isChar[i] = false;
		}

		while (rs.next()) {
			StringBuilder strBuilder = new StringBuilder(sqlCommandDes);
			for (int i = 0; i < numberOfColumns; i++) {
				if (i > 0)
					strBuilder.append(",");
				if (isChar[i])
					strBuilder.append("'");

				strBuilder.append(rs.getString(i + 1));

				if (isChar[i])
					strBuilder.append("'");
			}

			strBuilder.append(')');
			this.executeUpdate(strBuilder.toString());
		}

	}

	public void select(String src, String dst, String exp) throws SQLException {

		//this.dropTableIfExists(dst);
		String sqlCommandSrc = "Create View " + dst + " as Select * from " + src + " where " + exp;
		this.executeUpdate(sqlCommandSrc);

	}

	public void project(String src, String dst, List<ZSelectItem> cols) throws SQLException {
		StringBuilder strBuilder = new StringBuilder("Create View " + dst + " as Select ");
		for (int index = 0; index < cols.size(); index++) {
			strBuilder.append(cols.get(index).getColumn());

			if (index != cols.size() - 1)
				strBuilder.append(",");

		}

		strBuilder.append(" from " + src);
		this.executeUpdate(strBuilder.toString());
	}

	public void union(List<String> src, String dst) throws SQLException {
		String command = "Select * from ";

		StringBuilder unionCommand = new StringBuilder("Create View " + dst + " as " + command + src.get(0));

		for (int index = 1; index < src.size(); index++) {
			unionCommand.append(" UNION ALL ");
			unionCommand.append(command + src.get(index));
		}
		this.executeUpdate(unionCommand.toString());

	}

	private Set<String> exchanged = new HashSet<String>();
	
	public void clearExchange() {
		exchanged.clear();
	}
	
	public void exchangeTo(Machine destination, String src, String dst) throws SQLException {
		if (destination.exchanged.contains(dst))
			return;
		destination.exchanged.add(dst);
		
		List<Fragment> listFragment = this.fragmentList;

		for (int index = 0; index < listFragment.size(); index++)
			if (listFragment.get(index).getRelation().getTable().equals(src))
				destination.executeUpdate("Create Table " + dst + listFragment.get(index).getTableSchema());

		String sqlCommandSrc = "Select * from " + src;

		ResultSet rs = this.executeQuery(sqlCommandSrc);
		destination.storeResult(rs, dst);
	}

	public void join(String src1, String src2, String dst, String exp, List<ZSelectItem> lcols, List<ZSelectItem> rcols) throws SQLException {
		StringBuilder strBuilder = new StringBuilder("Create view " + dst + " as Select ");

		Set<String> hash = new HashSet<String>();
		
		boolean first = true;
		for (int index = 0; index < lcols.size(); index++) {
			String colname = lcols.get(index).getColumn();
			if (!hash.contains(colname)) {
				hash.add(colname);
			} else {
				continue;
			}
			if (first) {
				first = false;
			} else 
				strBuilder.append(",");
			strBuilder.append(src1 + "." + colname);

		}

		for (int index = 0; index < rcols.size(); index++) {
			String colname = rcols.get(index).getColumn();
			if (!hash.contains(colname)) {
				hash.add(colname);
			} else {
				continue;
			}
			strBuilder.append(",");
			strBuilder.append(src2 + "." + colname);
		}
		
		strBuilder.append(" from " + src1 + "," + src2 + " where " + exp);
		this.executeUpdate(strBuilder.toString());
	}

	public void write(String src, PrintStream out) throws SQLException, IOException {
		String sqlCommandSrc = "Select * from " + src;
		ResultSet rs = this.executeQuery(sqlCommandSrc);

		int numberOfColumns = rs.getMetaData().getColumnCount();

		for (int i = 1; i <= numberOfColumns; i++) {

			out.print(rs.getMetaData().getColumnName(i));
			out.print("\t");
		}
		out.println();

		
		while (rs.next()) {
			for (int i = 0; i < numberOfColumns; i++) {
				if (i > 0)
					out.print("\t");

				out.print(rs.getString(i + 1));

			}
			out.println();
		}
	}
}
