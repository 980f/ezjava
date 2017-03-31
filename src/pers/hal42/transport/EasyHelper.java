package pers.hal42.transport;


public interface EasyHelper<T> {
  /** * save object to cursor, might be null in which case save default values */
  void helpsave(EasyCursor ezc, T uneasy);
  /** * load guaranteed existing null constructed object from cursor */
  T helpload(EasyCursor ezc, T uneasy);

}

