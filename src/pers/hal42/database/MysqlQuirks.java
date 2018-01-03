package pers.hal42.database;

import pers.hal42.database.ColumnAttributes.Ordinator;
import pers.hal42.lang.Bool;
import pers.hal42.lang.Index;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.CsvIterator;
import pers.hal42.text.ListWrapper;
import pers.hal42.text.StringIterator;

import java.sql.*;
import java.util.Vector;

import static java.text.MessageFormat.format;
import static pers.hal42.database.DBMacros.getStringFromRS;
import static pers.hal42.lang.StringX.removeParens;

public class MysqlQuirks {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(MysqlQuirks.class);

  /** @returns whether @param s is wrapped with back ticks. */
  public static boolean isTicked(String s) {
    if (s == null) {
      return false;
    }
    if (s.length() < 2) {
      return false;
    }
    char tick = s.charAt(0);
    if (tick == '`') { //sometimes the tick is backwards
      return true;//todo: test matching end tick
    }
    //noinspection RedundantIfStatement
    if (tick == '\'') {//sometimes its forward
      return true;//todo: test matching end tick
    }
    return false;
  }

  /** @returns @param word with backtick quoting removed if present */
  public static String unTick(String word) {
    if (isTicked(word)) {
      return StringX.removeParens(word);
    } else {
      return word;
    }
  }


  /** mysql specific routine, and apparently the only fully reliable version thereof. */
  public static boolean getColumnsViaDDL(DBMacros jdbc, TableModel existing) throws SQLException {

    try (Statement s = jdbc.makeStatement()) {
      if (s == null) {
        return false;
      }
      ResultSet rs = s.executeQuery(format("SHOW CREATE TABLE {0}", existing.ti.fullName()));
      if (rs.next()) {
        String paragraph = rs.getString(2);//aka "Create Table", col 1 is table name
        StringIterator it = new CsvIterator(false, '\n', paragraph);
        if (it.hasNext()) {
          dbg.VERBOSE("Ignoring first line:{0}", it.next());
        }
        Ordinator ord = new Ordinator();
        while (it.hasNext()) {
          String line = it.next();
          StringIterator words = new CsvIterator(false, ' ', line);
          String word = words.next();
          switch (word) {
          case ")"://table attributes, not yet stored in model.
            break;
          case "PRIMARY": // PRIMARY KEY (`job_run_id`),
            word = words.next();
            if (word.equals("KEY")) {
              word = words.next();
              if (word.startsWith("(")) {
                //then is followed by parenthesized list of column names, each of which is a member of the pk
                int pkordinator = 0;
                final StringIterator cutter = new CsvIterator(false, ',', removeParens(word));
                while (cutter.hasNext()) {
                  final String name = unTick(cutter.next());
                  ColumnAttributes col = existing.getColByName(name);
                  if (col != null) {
                    col.pkordinal = ++pkordinator;
                    existing.pkIndex.cols.addElement(col);
                  } else {
                    dbg.WARNING("PK refers to unknown column: {0}", name);
                  }
                }
              }//end column list
            }    //end parsing Primary Key
            break;
          case "FULLTEXT"://for now we ignore this and just record that there is an index of some type
            if (!words.next().equals("KEY")) {
              break;
            }
            //join
          case "KEY":          //KEY `job_id_idx` (`job_id`)
            word = words.next();
            if (isTicked(word)) {
              String idxname = removeParens(word);
              pers.hal42.database.Index noob = new pers.hal42.database.Index();
              noob.my.name = idxname;
              existing.indexes.add(noob);
              word = words.next();
              if (word.charAt(0) == '(') {
                final StringIterator cutter = new CsvIterator(false, ',', removeParens(word));
                while (cutter.hasNext()) {
                  final String name = unTick(cutter.next());
                  ColumnAttributes col = existing.getColByName(name);
                  if (col != null) {
                    noob.cols.addElement(col);
                  } else {
                    dbg.WARNING("index {1} refers to unknown column: {0}", name, idxname);
                  }
                }
              }
            }
            break;
          default:
            if (word.startsWith("`")) {
              final ColumnAttributes col = ColumnAttributes.fromDDL(line);
              if (col != null) {
                ord.ordinate(col);
                existing.columns.add(col);
              } else {
                dbg.WARNING("DDL column not parsed: {0}", line);
              }
            } else {
              dbg.WARNING("DDL line not parsed: {0}", line);
            }
            break;
          }
        }
        return true;
      } else {
        return false;
      }
    }
  }

  /** use 'select *' resultset metadata to get table structure information */
  protected static void getColumnsViaSelect(DBMacros jdbc, TableInfo ti, Vector<ColumnAttributes> columns) throws SQLException {

    try (Statement s = jdbc.makeStatement()) {
      ResultSet rs = s.executeQuery(format("SELECT * FROM {0} WHERE 1 = 0", ti.fullName()));
      ResultSetMetaData meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();
      Ordinator ord = new Ordinator();

      for (int i = 1; i <= columnCount; i++) {
        String column_name = meta.getColumnName(i);
        int data_code = meta.getColumnType(i);
        ColumnType data_type = ColumnType.map(data_code);
        final ColumnAttributes col = new ColumnAttributes(column_name, data_type);
        col.nullable = (ResultSetMetaData.columnNullable == meta.isNullable(i));
        col.autoIncrement = meta.isAutoIncrement(i);
        if (data_type == ColumnType.TIMESTAMP && column_name.equalsIgnoreCase("changed")) { //in-house rule for determining automatic timestamp.
          col.autoIncrement = true;//until we get better metadata access we use magic name for this.
        }
        col.size = meta.getColumnDisplaySize(i);//the closest I could find.
        ord.ordinate(col);
        columns.add(col);
      }
    }
  }

  /** caching in this class, did not work with mysql, despite being sql92 compliant (this code is compliant) */
  public static void getColumnsViaDmbd(DBMacros jdbc, TableInfo ti, Vector<ColumnAttributes> columns) {
    Ordinator ord = new Ordinator();

    try (ResultSet cpmd = jdbc.getColumns(ti)) {
      if (cpmd != null) {
        while (cpmd.next()) {
          ColumnAttributes col = null;
          try {
            String tablename = StringX.TrivialDefault(getStringFromRS("TABLE_NAME", cpmd), "");
            String columnname = StringX.TrivialDefault(getStringFromRS("COLUMN_NAME", cpmd), "");
            String type = StringX.TrivialDefault(getStringFromRS("TYPE_NAME", cpmd), "").toUpperCase();
            String size = StringX.TrivialDefault(getStringFromRS("COLUMN_SIZE", cpmd), "");
            String nullable = StringX.TrivialDefault(getStringFromRS("IS_NULLABLE", cpmd), "");
            dbg.VERBOSE("About to create ColumnProfile for " + tablename + "." + columnname + " with type of " + type + "!");
            col = new ColumnAttributes(columnname, type);//, size, nullable);
            ord.ordinate(col);
//              cp.tableCat = StringX.TrivialDefault(getStringFromRS("TABLE_CAT", cpmd), "");
//              cp.tableSchem = StringX.TrivialDefault(getStringFromRS("TABLE_SCHEM", cpmd), "");
            col.size = StringX.parseInt(getStringFromRS("DECIMAL_DIGITS", cpmd));
//              cp.numPrecRadix = StringX.TrivialDefault(getStringFromRS("NUM_PREC_RADIX", cpmd), "");
            col.nullable = Bool.For(getStringFromRS("NULLABLE", cpmd));
//              cp.remarks = StringX.TrivialDefault(getStringFromRS("REMARKS", cpmd), "");
            col.defawlt = getStringFromRS("COLUMN_DEF", cpmd);
//              cp.charOctetLength = StringX.TrivialDefault(getStringFromRS("CHAR_OCTET_LENGTH", cpmd), "");
//              cp.ordinalPosition = StringX.TrivialDefault(getStringFromRS("ORDINAL_POSITION", cpmd), "");
          } catch (Exception e2) {
            dbg.Caught(e2);
          }
          if (col != null) {
            columns.add(col);
            dbg.VERBOSE("Adding column {0}", col.name);
          }
        }
      } else {
        dbg.ERROR("cpmd is NULL!");
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  /**
   * parse mysql infoschema columns info.
   * presumes rs has been tested with next()
   * The @param ordinator is used to recordType column positions, for use with PreparedStatement updates.
   */
  public static ColumnAttributes parseColumn(ResultSet rs, Ordinator ordinator) throws SQLException {

    String column_name = rs.getString("COLUMN_NAME");
    String data_type = rs.getString("DATA_TYPE");
    final ColumnAttributes col = new ColumnAttributes(column_name, data_type);
    col.nullable = rs.getBoolean("IS_NULLABLE");//String YES NO or empty
    col.defawlt = rs.getString("COLUMN_DEFAULT");
    if (data_type.equalsIgnoreCase("timestamp")) {
      col.autoIncrement = rs.getString("EXTRA").equalsIgnoreCase("on update CURRENT_TIMESTAMP");
    }
    ordinator.ordinate(col);
    String column_type = rs.getString("COLUMN_TYPE");
    col.size = rs.getInt("CHARACTER_MAXIMUM_LENGTH");//varchar
    return col;
  }

  public static void showInfoSchemaColumns(ResultSet rs) {
    ResultSetMetaData meta;
    try {
      meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();
      ListWrapper headers = new ListWrapper(new StringBuilder(200));
      for (int i = 0; ++i <= columnCount; ) {
        headers.append(meta.getColumnName(i));
      }
      headers.close();
      dbg.ERROR(headers.toString());
      Index ordinator = new Index(0);
      while (rs.next()) {

        ListWrapper bodies = new ListWrapper(new StringBuilder(200));
        for (int i = 0; ++i <= columnCount; ) {
          bodies.append(format("^{0}^", rs.getString(i)));//some fields seem to have commas in them, can mysql be any more vicious?
        }
        bodies.close();
        dbg.ERROR(bodies.toString());
      }
    } catch (SQLException e) {
      dbg.Caught(e);
    }
  }

  public static void parseInfoSchemaColumns(ResultSet rs, Vector<ColumnAttributes> columns) throws SQLException {
    Ordinator ord = new Ordinator();
    while (rs.next()) {
      columns.add(parseColumn(rs, ord));
    }
  }

  /** get table description. */
  public static void getInfoSchemaColumns(DBMacros jdbc, TableInfo ti, Vector<ColumnAttributes> columns) throws SQLException {
    try (PreparedStatement pst = jdbc.makePreparedStatement("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = ? AND table_schema = ?;")) {
      pst.setString(1, ti.name());
      pst.setString(2, ti.schema());
      ResultSet rs = pst.executeQuery();
      parseInfoSchemaColumns(rs, columns);
    }
  }
}
