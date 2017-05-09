package pers.hal42.database;

/* used for heuristics in managing tables.
A configuration table's rows get updates, but the table itself doesn't get many new rows.
A log table's rows rarely get updates (usually only in the same transaction) and new rows are frequently created, and deleted (if ever) by time.
* */

public enum TableType {
  cfg,log;


}

