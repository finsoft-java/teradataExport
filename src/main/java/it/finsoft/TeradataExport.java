package it.finsoft;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class TeradataExport {

	static DateFormat formatter0 = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	static DateFormat formatter6 = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.ssssss");

	/**
	 * Si aspetta due argomenti: l'url di connessione JDBC al database, e il nome
	 * della tabella
	 * 
	 * @param args
	 * @throws SQLException
	 */
	public static void main(String[] args) throws SQLException {
		if (args.length != 2) {
			printUsage();
			return;
		}
		Connection conn = DriverManager.getConnection(args[0]);
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(args[1]);
		ResultSetMetaData md = rs.getMetaData();
		String part1Insert = creaPart1Insert(md);
		Map<String, String> dateFormat = getFormatoColonneData(conn, md);

		while (rs.next()) {

			StringBuilder part2Insert = creaPart2Insert(rs, md, dateFormat);

			String insertStatement = part1Insert + part2Insert;
			System.out.println(insertStatement);
		}
		conn.close();
	}

	private static void printUsage() {
		System.err.println("Usage:");
		System.err.println("     java -jar TeradataExport.jar <jdbc connection url> <query>");
		System.err.println("     java -jar TeradataExport.jar <jdbc connection url> <query>   >   file.sql");
		System.err.println("Es.");
		System.err.println(
				"     java -jar TeradataExport.jar \"jdbc:teradata://<server>/USER=<user>,PASSWORD=<password>,DATABASE=<database>\"  \"SELECT * FROM MYTABLE\"");
	}

	private static Map<String, String> getFormatoColonneData(Connection conn, ResultSetMetaData md)
			throws SQLException {
		Map<String, String> dateFormat = new HashMap<String, String>();
		Statement stmt = conn.createStatement();
		String tableName = md.getTableName(1);
		String schemaTable = md.getSchemaName(1);
		String queryTabella = "Select ColumnName,ColumnFormat from dbc.columns where databasename='" + schemaTable
				+ "' and TableName = '" + tableName + "' and columntype like '%DA%'";
		ResultSet rsTabella = stmt.executeQuery(queryTabella);
		while (rsTabella.next()) {
			dateFormat.put(rsTabella.getString("ColumnName").trim(), rsTabella.getString("ColumnFormat").trim());
		}
		return dateFormat;
	}

	/**
	 * Formatta in linguaggio SQL il campo i del ResultSet rs, alla riga corrente, e
	 * aggiunge il risultato a part2Insert
	 * 
	 * @param dateFormat e' la mappa restituita da getFormatoColonneData()
	 */
	private static void formattaCampo(StringBuilder part2Insert, ResultSetMetaData md, ResultSet rs, int i,
			Map<String, String> dateFormat) throws SQLException {

		if (rs.getString(i) == null) {
			part2Insert.append("null");
			return;
		}

		switch (md.getColumnTypeName(i)) {

		case "INTEGER": // 1
		case "DECIMAL": // 0.1726537942886353

			part2Insert.append(rs.getString(i));
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
			part2Insert.append("'").append(dataFinale).append("'");
			break;

		case "TIMESTAMP":

			String tmsFinale = null;
			if (md.getScale(i) == 6) {
				tmsFinale = formatter6.format(rs.getTimestamp(i));
			} else if (md.getScale(i) == 0) {
				tmsFinale = formatter0.format(rs.getTimestamp(i));
			} else {
				System.err.println("ERRORE timestamp non supportato, scale=" + md.getScale(i));
				return;
			}
			part2Insert.append("'").append(tmsFinale).append("'");
			break;

		default: // VARCHAR
			String stringDaFormattare = rs.getString(i);
			stringDaFormattare = stringDaFormattare.replaceAll("'", "''");
			part2Insert.append("'").append(stringDaFormattare).append("'");
		}
	}

	/**
	 * Crea la prima parte dello statement, "insert into nometabella (nomecampo1,
	 * nomecampo2, ...)
	 */
	private static String creaPart1Insert(ResultSetMetaData md) throws SQLException {

		String nameTable = md.getTableName(1);
		String schemaTable = md.getSchemaName(1);
		StringBuilder part1Insert = new StringBuilder("Insert into " + schemaTable + "." + nameTable + " (");

		for (Integer i = 1; i <= md.getColumnCount(); i++) {
			if (i != md.getColumnCount()) {
				part1Insert.append(md.getColumnName(i)).append(",");
			} else {
				part1Insert.append(md.getColumnName(i));
			}
		}
		part1Insert.append(")");

		return part1Insert.toString();
	}

	/**
	 * Crea la seconda parte dello statement insert
	 * 
	 * @param dateFormat e' la mappa restituita da getFormatoColonneData()
	 */
	private static StringBuilder creaPart2Insert(ResultSet rs, ResultSetMetaData md, Map<String, String> dateFormat)
			throws SQLException {
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
			}
		}
		return part2Insert;
	}

}
