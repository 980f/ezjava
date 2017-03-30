package pers.hal42.transport;


public interface EasyHelper {
  /** * save object to cursor, might be null in which case save default values */
  public void helpsave(EasyCursor ezc, Object uneasy);
  /** * load guaranteed existing null constructed object from cursor */
  public Object helpload(EasyCursor ezc,Object uneasy);

}

