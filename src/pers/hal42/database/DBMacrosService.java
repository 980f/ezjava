package pers.hal42.database;

public class DBMacrosService {
}
//extends Service {
//
//  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DBMacrosService.class);
//
//  public static final String NAME = "Database";
//
//  public DBMacrosService(ServiceConfigurator cfg) {
//    super(NAME, cfg);
//  }
//
//  public String svcCnxns() {
//    int cnxns = DBMacros.connectionCount();
//    int used = DBMacros.connectionsUsed();
//    String ret = of(used, cnxns);
//    dbg.ERROR("cnxns returned " + ret);
//    return ret;
//  }
//  public String svcTxns() {
//    return ""+(DBMacros.queryStats.getCount()+DBMacros.updateStats.getCount());
//  }
//  public String svcTimeouts() {
//    return ""+0; // ? +++
//  }
//  public String svcAvgTime() {
//    return DateX.millisToSecsPlus((DBMacros.queryStats.getTotal()+DBMacros.updateStats.getTotal()) / (DBMacros.queryStats.getCount()+DBMacros.updateStats.getCount()));
//  }
//  public String svcWrites() {
//    return printStats(DBMacros.updateStats);
//  }
//  public String svcReads() {
//    return printStats(DBMacros.queryStats);
//  }
//  public String svcNotes() {
//    long min = Math.min(DBMacros.queryStats.getMin(),DBMacros.updateStats.getMin());
//    long max = Math.max(DBMacros.queryStats.getMax(),DBMacros.updateStats.getMax());
//    return "TxnTimes ["+min+"-"+max+"]";
//  }
////  public String svcPend() {
////    return ""+pers.hal42.web.table.TableGen.getUnclosedStatements().size();
////  }
//
//  // +++ do these !!!!
//  public boolean isUp() {
//    return true;
//  }
//  public void down() {
//    // stub
//  }
//  public void up() {
//    // stub
//  }
//
//  public void notifyCfgChanged(String msg) {
//    PANIC(configger.getServiceParam(serviceName(), "cfgChangedNotify", mailList()), "CfgChange", msg);
//  }
//
//  private static final int DEFAULTTXNCOUNTLIMIT = 100;
//  public int txnCountLimit() { // for searches
//    return (configger == null) ? DEFAULTTXNCOUNTLIMIT : configger.getIntServiceParam(NAME, "TXNCOUNTLIMIT", DEFAULTTXNCOUNTLIMIT);
//  }
//  public boolean txnCountUnderLimit(int count) {
//    return (count < txnCountLimit());
//  }
//}
