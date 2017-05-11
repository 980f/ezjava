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
 *  A class used to update records from SQL tables. The constructor is not
 *  public. To obtain a DbUpdater call DbTable.updater(); Example: To update all
 *  the people who are (younger than 18 or older than 80) and whose name is
 *  "Fred"... to have a favourite_team of "Raiders"; <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbUpdater updater = people.deleter();
 * updater.addColumn(people.getColumn("FAVOURITE_TEAM"), "Raiders");
 * updater.setWhere(people.getColumn("AGE").lessThan(new Integer(18)).or(
 *    people.getColumn("AGE").greaterThan(new Integer(80))).and(
 *    people.getColumn("NAME").equal("FRED"));
 * int numberOfPeopleUpdated = updater.execute();
 * </PRE> This is equivilent to... <PRE>
 * UPDATE PEOPLE SET FAVOURITE_TEAM='Raiders' WHERE (AGE < 18 OR AGE > 80 ) AND NAME='Fred'
 * </PRE> Note the use of equal(), NOT equals(). To get more fancy, to update
 *  the same group of people to have a favourite team the same as Bill's team,
 *  we use a sub-select... <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbSelector bills_team = db.selector();
 * bills_team.addColumn(people.getColumn("FAVOURITE_TEAM"));
 * bills_team.setWhere(people.getColumn("NAME").equal("BILL"));
 * DbUpdater updater = people.deleter();
 * updater.addColumn(people.getColumn("FAVOURITE_TEAM"), bills_team);
 * updater.setWhere(people.getColumn("AGE").lessThan(new Integer(18)).or(
 *    people.getColumn("AGE").greaterThan(new Integer(80))).and(
 *    people.getColumn("NAME").equal("FRED"));
 * int numberOfPeopleUpdated = updater.execute();
 * </PRE> This is equivilent to... <PRE>
 * UPDATE PEOPLE SET FAVOURITE_TEAM=(SELECT FAVOURITE_TEAM FROM PEOPLE WHERE NAME='Bill')
 *    WHERE (AGE < 18 OR AGE > 80 ) AND NAME='Fred'
 * </PRE>
 *
 * @author     Chris Bitmead
 * @created    December 13, 2001
 */

public class DbUpdater {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbUpdater.class);

  TableProfile table;
  List<ColumnProfile> intoList = new ArrayList<>();
  /** until we inject some interface we can stick anything into a column */
  List<Object> fromList = new ArrayList<>();
  DbExpr where;

  DbUpdater(TableProfile table) {
    this.table = table;
  }

  /**
   *  Set the where condition on which records to update.
   *
   * @param  where  The new where value
   */
  public void setWhere(DbExpr where) {
    this.where = where;
  }

  /**
   *  Add a column specification to update. The new value can either be a raw
   *  value - Integer, String, java.sql.Date etc. Or it can be a DbSelector in
   *  the case of a sub-select.
   *
   * @param  into  The column to update.
   * @param  from  The new value.
   */
  public void addColumn(ColumnProfile into, Object from) {
    intoList.add(into);
    fromList.add(from);
  }

  /**
   *  Execute this delete command on a specific connection.
   *
   * @return                  The number of record affected.
   * @exception  Exception  Description of Exception
   */
  public int execute() {
//      PreparedStatement stmt = dbcon.con.prepareStatement(getQueryString());
//      setSqlValues(stmt, 1);
//      return stmt.executeUpdate();
    return -1;//
  }

  int setSqlValues(PreparedStatement stmt, int i) throws SQLException {
    Iterator intoi = intoList.iterator();
    Iterator fromi = fromList.iterator();
    while (intoi.hasNext()) {
      ColumnProfile intocol = (ColumnProfile) intoi.next();
      Object fromcol = fromi.next();
      if (fromcol instanceof DbSelector) {
        i = ((DbSelector) fromcol).setSqlValues(stmt, i, null);
      } else {
        i = DbSelector.setSqlValue(stmt, i, fromcol, intocol);
      }
    }
    i = where.setSqlValues(stmt, i);
    return i;
  }

  String getQueryString() {
    String rtn = "UPDATE " + table.fullname() + " ";
    int i = 0;
    Iterator intoi = intoList.iterator();
    Iterator fromi = fromList.iterator();
    while (intoi.hasNext()) {
      ColumnProfile intocol = (ColumnProfile) intoi.next();
      Object fromcol = fromi.next();
      if (i != 0) {
        rtn += ", ";
      } else {
        rtn += " SET ";
      }
      rtn += intocol.name() + " = ";
      if (fromcol instanceof DbSelector) {
        rtn += "(" + ((DbSelector) fromcol).getQueryString() + ")";
      } else {
        rtn += "?";
      }
      i++;
    }
    if (where != null) {
      rtn += " WHERE ";
      rtn += where.getQueryString();
    }
    dbg.WARNING("getQueryString(): "+ rtn);
    return rtn;
  }

}
