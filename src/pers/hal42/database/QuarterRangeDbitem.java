package pers.hal42.database;

import com.fedfis.data.QuarterRange;
import com.fedfis.db.ColumnAttributes;
import pers.hal42.lang.StringX;

import java.sql.ResultSet;
import java.sql.SQLException;

import static java.text.MessageFormat.format;

public class QuarterRangeDbitem {
  /** time range */  //todo: make a class for this pair and its transformation into QuarterRange
  public final ColumnAttributes start = ColumnAttributes.Stringy(6, true);
  public final ColumnAttributes end = ColumnAttributes.Stringy(6, true);

  public QuarterRangeDbitem(String prefix) {//todo:1 eponomizer needs path builder
    if (StringX.NonTrivial(prefix)) {
      prefix = "quarter";
    }
    start.name = format("{0}_{1}", prefix, "start");
    end.name = format("{0}_{1}", prefix, "end");
  }

  /** parse by column names */
  public void parseInto(QuarterRange target, ResultSet rs) throws SQLException {
    String start = rs.getString(this.start.name);
    target.one().parse(start);
    String end = rs.getString(this.end.name);
    target.two().parse(end);
  }

  /** for incrementally scanned row */
  public static class QuarterRangeResultParser {
    QuarterRange target;

    private QuarterRangeResultParser(QuarterRange target) {
      this.target = target;
    }

    QuarterRange apply(ResultSetFieldIterator fi) {
      String start = fi.get("");
      target.one().parse(start);
      String end = fi.get("");
      target.two().parse(end);
      return target;
    }
  }
}
