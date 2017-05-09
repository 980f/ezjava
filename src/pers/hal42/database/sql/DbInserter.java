package pers.hal42.database.sql;
import java.util.*;
import java.sql.*;
import pers.hal42.database.*;
import pers.hal42.logging.ErrorLogStream;

/**
 *  A class used to insert records into SQL tables. The constructor is not
 *  public. To obtain a DbInserter call DbTable.inserter(); Example: To insert a
 *  record into the people table... <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbInserter inserter = people.inserter();
 * inserter.addColumn(people.getColumn("NAME"), "Fred"));
 * inserter.addColumn(people.getColumn("FAVOURITE_TEAM"), "Raiders");
 * inserter.addColumn(people.getColumn("AGE"), new Integer(30));
 * int numberOfPeopleInserted = inserter.execute();
 * </PRE> This is equivilent to... <PRE>
 * INSERT INTO PEOPLE(NAME, FAVOURITE_TEAM, AGE) VALUES('Fred', 'Raiders', 30)
 * </PRE> The same thing as above can be achieved using a SELECT clause, and
 *  this can lead us to creating much more complex expressions... <PRE>
 * DbDatabase db = ...;
 * DbSelector selector = db.selector();
 * DbTable people = db.getTable("PEOPLE");
 * DbInserter inserter = people.inserter(selector);
 * inserter.addColumn(people.getColumn("NAME"), selector.addColumn("Fred")));
 * inserter.addColumn(people.getColumn("FAVOURITE_TEAM"), selector.addColumn("Raiders"));
 * inserter.addColumn(people.getColumn("AGE"), selector.addColumn(new Integer(30)));
 * int numberOfPeopleInserted = inserter.execute();
 * </PRE> This is equivilent to... <PRE>
 * INSERT INTO PEOPLE(NAME, FAVOURITE_TEAM, AGE) SELECT 'Fred', 'Raiders', 30
 * </PRE> To get more fancy we can insert data that has been selected from
 *  another table. To insert all the people from the PLAYERS table into the
 *  PEOPLE table who are older than 20, and we set their favourite team to be
 *  the team they play for... <PRE>
 * DbDatabase db = ...;
 * DbSelector selector = db.selector();
 * DbTable people = db.getTable("PEOPLE");
 * DbTable players = db.getTable("PLAYERS");
 * DbInserter inserter = people.inserter(selector);
 * inserter.addColumn(people.getColumn("NAME"), selector.addColumn(players.getColumn("NAME")));
 * inserter.addColumn(people.getColumn("FAVOURITE_TEAM"), selector.addColumn(players.getColumn("TEAM")));
 * inserter.addColumn(people.getColumn("AGE"), selector.addColumn(players.getColumn("AGE")));
 * selector.setWhere(players.getColumn("AGE").greaterThan(new Integer(20)));
 * int numberOfPeopleInserted = inserter.execute();
 * </PRE> This is equivilent to... <PRE>
 * INSERT INTO PEOPLE(NAME, FAVOURITE_TEAM, AGE) SELECT NAME, TEAM, AGE FROM PLAYERS WHERE AGE > 20
 * </PRE>
 *
 * @author     Chris Bitmead
 * @created    December 13, 2001
 */

public class DbInserter {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbInserter.class);

  TableProfile table;
  DbSelector selector;
  List intoList = new ArrayList();
  List fromList = new ArrayList();

  DbInserter(TableProfile table, DbSelector selector) {
    this.table = table;
    this.selector = selector;
  }

  DbInserter(TableProfile table) {
    this.table = table;
    this.selector = selector;
  }

  public int setSqlValues(PreparedStatement stmt, int i) throws SQLException {
    if (selector == null) {
      Iterator it = fromList.iterator();
      Iterator intoit = intoList.iterator();
      while (it.hasNext()) {
        DbSelector.setSqlValue(stmt, i++, it.next(), (ColumnProfile) intoit.next());
      }
    } else {
      selector.setSqlValues(stmt, 1, intoList);
    }
    return i;
  }

  /**
   *  Specify the value of a column to add.
   *
   * @param  into  The column we are inserting into.
   * @param  from  The column from a selector that we are getting a value from.
   */
  public void addColumn(ColumnProfile into, Object from) {
    intoList.add(into);
    fromList.add(from);
  }

  /**
   *  Execute this command on a specific connection.
   *
   * @param  dbcon            Description of Parameter
   * @return                  The number of record affected.
   * @exception  Exception  Description of Exception
   */
  public int execute() {
//      PreparedStatement stmt = dbcon.con.prepareStatement(getQueryString());
//      setSqlValues(stmt, 1);
//      return stmt.executeUpdate();
      return -1;//
  }

  String getValuesQueryString() {
    String rtn = " VALUES (";
    for (int i = 0; i < fromList.size(); i++) {
      if (i != 0) {
        rtn += ", ";
      }
      rtn += "?";
    }
    rtn += ")";
    return rtn;
  }

  String getQueryString() {
    String rtn = "INSERT INTO " + table.fullname() + " (";
    int i = 0;
    Iterator fieldi = intoList.iterator();
    while (fieldi.hasNext()) {
      ColumnProfile col = (ColumnProfile) fieldi.next();
      if (i != 0) {
        rtn += ", ";
      }
      rtn += col.name();
      i++;
    }
    rtn += ") ";
    if (selector == null) {
      rtn += getValuesQueryString();
    } else {
      rtn += selector.getQueryString();
    }
    dbg.WARNING("getQueryString(): " + rtn);
    return rtn;
  }

}
