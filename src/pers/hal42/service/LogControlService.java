package pers.hal42.service;

import pers.hal42.logging.LogLevelEnum;
import pers.hal42.logging.LogSwitch;
import pers.hal42.thread.Counter;
import pers.hal42.transport.EasyCursor;

public class LogControlService extends Service {

  // Singleton
  private static LogControlService logControl = null;
  public static final LogControlService THE() {
    return logControl;
  }
  public static final synchronized boolean setLogControl(ServiceConfigurator configger) {
    if(logControl == null) {
      logControl = new LogControlService(configger); // causes it to load and set the levels
      return true;
    }
    return false;
  }

  public static final String NAME = "LogControl";
  private Counter writes = new Counter();
  public LogControlService(ServiceConfigurator cfg) {
    super(NAME, cfg, true);
    up();
  }
  public String svcCnxns() {
    return ""+ LogSwitch.listLevels().size();
  }
  public String svcWrites() {
    return ""+writes.value();
  }
// make this show the last applied time & how many were applied?
//  public String svcNotes() {
//    return diskSpaceFree(LogFile.getPath())+" ["+LogFile.getPath()+"]";
//  }
  public boolean isUp() {
    return true;
  }
  public void down() {
    // stub
  }

  public void up() {
    applyLevels(loadLevels()); // load them from storage & apply them
    saveUnsavedLevels();
    markStateChange();
  }

  private EasyCursor loadLevels() {
    return configger.getAllServiceParams(serviceName());
  }

  private void applyLevels(EasyCursor ezc) {
    LogSwitch.apply(ezc);
  }

  // This saves any new levels that haven't existed in the database thus far into it with defaults
  public void saveUnsavedLevels() {
    configger.getServiceParams(serviceName(), LogSwitch.asProperties()); // this get() just causes any not saved to be saved; does not change ones that already exist!
  }

  private void saveLevels() {
    configger.setServiceParams(serviceName(), LogSwitch.asProperties());
  }

  private void saveLevel(LogSwitch ls) {
    configger.setServiceParam(serviceName(), ls.Name(), ls.toString());//todo:0 proper 3rd argument for logging level.
  }

  public void setAll(String to) {
    LogSwitch.SetAll(to);
    saveLevels();
  }

  public int set(LogSwitch ls, String to) {
    ls.setLevel(LogLevelEnum.valueOf(to)).Level();
    saveLevel(ls);
    return ls.Level();
  }

  public boolean selfConfiguring() {
    return true; // this prevents the system from generating interfaces for users to alter configuration on disk
  }
}

