package pers.hal42.thread;

public interface QReceiver {
  /**
   * @return false if object is not accepted. true if successfully received
   */
  public boolean Post(Object arf);
}

