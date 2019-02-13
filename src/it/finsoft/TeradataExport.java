package it.finsoft;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class TeradataExport {

	/**
	 * Si aspetta due argomenti: l'url di connessione JDBC al database, e il
	 * nome della tabella
	 * 
	 * @param args
	 * @throws SQLException
	 */
	public static void main(String[] args) throws SQLException {
		//TODO TOGLIERE GLI APICI ALLE STRINGHE \'
		if (args.length != 2) {
			printUsage();
			return;
		}
		Connection conn = DriverManager.getConnection(args[0]);
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(args[1]);
		ResultSetMetaData md = rs.getMetaData();
		StringBuilder insertStatement = new StringBuilder();
		String part1Insert = creaPart1Insert(md);
		Map<String, String> dateFormat = getFormatoColonneData(conn, md);

		while (rs.next()) {

			StringBuilder part2Insert = new StringBuilder();

			for (Integer i = 1; i <= md.getColumnCount(); i++) {

				if (i == 1) {
					part2Insert.append(" VALUES (");
				}

				formattaCampo(part2Insert, md, rs, i, dateFormat);

				if (i != md.getColumnCount()) {
					part2Insert.append(",");
				}

				if (i == md.getColumnCount()) {
					part2Insert.append(" ); ");
					insertStatement.append(part1Insert);
					insertStatement.append(part2Insert + "\n");
				}
			}
		}
		System.out.println(insertStatement);
		conn.close();
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("     java -jar TeradataExport.jar <jdbc connection url> <query>");
		System.out.println("     java -jar TeradataExport.jar <jdbc connection url> <query>   >   file.sql");
		System.out.println("Es.");
		System.out.println("     java -jar TeradataExport.jar \"jdbc:teradata://<server>/USER=<user>,PASSWORD=<password>,DATABASE=<database>\"  \"SELECT * FROM MYTABLE\"");
	}

	private static Map<String, String> getFormatoColonneData(Connection conn, ResultSetMetaData md) throws SQLException {
		Map<String, String> dateFormat = new HashMap<String, String>();
		Statement stmt = conn.createStatement();
		String tableName = md.getTableName(1);
		String queryTabella = "Select ColumnName,ColumnFormat from dbc.columns where databasename='TBDTD0D' and TableName = '"
				+ tableName + "' and columntype like '%DA%'";
		ResultSet rsTabella = stmt.executeQuery(queryTabella);
		while (rsTabella.next()) {
			dateFormat.put(rsTabella.getString("ColumnName").trim(), rsTabella.getString("ColumnFormat").trim());
		}
		return dateFormat;
	}

	@SuppressWarnings("null")
	private static void formattaCampo(StringBuilder part2Insert, ResultSetMetaData md, ResultSet rs, int i,
			Map<String, String> dateFormat) throws SQLException {

		switch (md.getColumnTypeName(i)) {

		case "INTEGER":// 1
		case "DECIMAL": // 0.1726537942886353

			if (rs.getString(i) != null) {
				part2Insert.append(rs.getString(i));
			} else {
				part2Insert.append("null");
			}
			break;

		case "BLOB":
		case "CLOB":

			part2Insert.append("null");
			break;

		case "DATE":

			String dataFinale = null;
			String dataDaFormattare = null;
			dataDaFormattare = dateFormat.get(md.getColumnLabel(i));
			dataDaFormattare = dataDaFormattare.replace("mm", "MM").replace("DD", "dd");
			DateFormat formatter = new SimpleDateFormat(dataDaFormattare);
			dataFinale = formatter.format(rs.getDate(i));

			if (rs.getString(i) != null) {
				part2Insert.append("'" + dataFinale + "'");
			} else {
				part2Insert.append("null");
			}
			break;

		case "TIMESTAMP":

			DateFormat formatter0 = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
			DateFormat formatter6 = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.ssssss");
			String tmsFinale = null;
			if (md.getScale(i) == 6) {
				tmsFinale = formatter6.format(rs.getTimestamp(i));
			} else if (md.getScale(i) == 0) {
				tmsFinale = formatter0.format(rs.getTimestamp(i));
			} else {
				System.out.println("ERRORE timestamp non supportato" + md.getScale(i));
				return;
			}

			if (rs.getString(i) != null) {
				part2Insert.append("'" + tmsFinale + "'");
			} else {
				part2Insert.append("null");
			}
			break;

		default: // VARCHAR
			String stringDaFormattare = rs.getString(i);
			if (rs.getString(i) != null) {
				stringDaFormattare = stringDaFormattare.replaceAll("'", "''");
				part2Insert.append("'" + stringDaFormattare + "'");
			} else {
				part2Insert.append("null");
			}
		}
	}
	
	private static String creaPart1Insert(ResultSetMetaData md) throws SQLException {

		String nameTable = md.getTableName(1);
		String schemaTable = md.getSchemaName(1);

		StringBuilder part1Insert = new StringBuilder("Insert into " + schemaTable + "." + nameTable + " (");

		for (Integer i = 1; i <= md.getColumnCount(); i++) {
			if (i != md.getColumnCount()) {
				part1Insert.append(md.getColumnName(i) + ",");
			} else {
				part1Insert.append(md.getColumnName(i));
			}
		}
		part1Insert.append(")");

		return part1Insert.toString();
	}

}
