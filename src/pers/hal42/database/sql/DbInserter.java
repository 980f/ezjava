package pers.hal42.database.sql;

import pers.hal42.database.ColumnProfile;
import pers.hal42.database.TableProfile;
import pers.hal42.logging.ErrorLogStream;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class used to insert records into SQL tables. The constructor is not
 * public. To obtain a DbInserter call DbTable.inserter(); Example: To insert a
 * record into the people table... <PRE>
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
 * this can lead us to creating much more complex expressions... <PRE>
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
 * another table. To insert all the people from the PLAYERS table into the
 * PEOPLE table who are older than 20, and we set their favourite team to be
 * the team they play for... <PRE>
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
 * @author Chris Bitmead
 * @created December 13, 2001
 */

public class DbInserter {
  TableProfile table;
  DbSelector selector;
  List<ColumnProfile> intoList = new ArrayList<>();
  List<Object> fromList = new ArrayList<>();
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbInserter.class);

  DbInserter(TableProfile table, DbSelector selector) {
    this.table = table;
    this.selector = selector;
  }

  DbInserter(TableProfile table) {
    this.table = table;
    this.selector = null;
  }

  public int setSqlValues(PreparedStatement stmt, int i) throws SQLException {
    if (selector == null) {
      Iterator<Object> it = fromList.iterator();
      Iterator<ColumnProfile> intoit = intoList.iterator();
      while (it.hasNext()) {
        DbSelector.setSqlValue(stmt, i++, it.next(), intoit.next());
      }
    } else {
      selector.setSqlValues(stmt, 1, intoList);
    }
    return i;
  }

  /**
   * Specify the value of a column to add.
   *
   * @param into The column we are inserting into.
   * @param from The column from a selector that we are getting a value from.
   */
  public void addColumn(ColumnProfile into, Object from) {
    intoList.add(into);
    fromList.add(from);
  }

  /**
   * Execute this command on a specific connection.

   * @return The number of record affected.
   * @throws Exception Description of Exception
   */
  public int execute() {
//      PreparedStatement stmt = dbcon.con.prepareStatement(getQueryString());
//      setSqlValues(stmt, 1);
//      return stmt.executeUpdate();
    return -1;//
  }

  String getValuesQueryString() {
    StringBuilder rtn = new StringBuilder(" VALUES (");
    for (int i = 0; i < fromList.size(); i++) {
      if (i != 0) {
        rtn.append(", ");
      }
      rtn.append("?");
    }
    rtn.append(")");
    return String.valueOf(rtn);
  }

  String getQueryString() {
    StringBuilder rtn = new StringBuilder(100);
    rtn.append("INSERT INTO ").append(table.fullname()).append(" (");
    int i = 0;
    for (ColumnProfile col : intoList) {
      if (i != 0) {
        rtn.append(", ");
      }
      rtn.append(col.name());
      i++;
    }
    rtn.append(") ");
    if (null != selector) {
      rtn.append(selector.getQueryString());
    } else {
      rtn.append(getValuesQueryString());
    }
    dbg.WARNING("getQueryString(): " + rtn);
    return String.valueOf(rtn);
  }

}
