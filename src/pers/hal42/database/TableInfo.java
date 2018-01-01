package pers.hal42.database;

import org.jetbrains.annotations.NotNull;

import static java.text.MessageFormat.format;

public interface TableInfo extends Comparable<TableInfo> {
  default String catalog() {
    return "def";
  } //mysql default catalog.

  String schema();

  String name();

  /** @returns schema.name, unchecked for nulls */
  default String fullName() {
    return format("{0}.{1}", schema(), name());
  }

  //todo: reproduce tabletype enum (log, config, )
  default String type() {
    return "simple";
  }

  default String remark() {
    return "";
  }

  @Override
  default int compareTo(@NotNull TableInfo o) {
    int diff = catalog().compareTo(o.catalog());
    if (diff == 0) {
      diff = schema().compareTo(o.schema());
      if (diff == 0) {
        diff = name().compareTo(o.name());
      }
    }
    return diff;
  }
}
