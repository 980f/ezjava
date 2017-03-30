

import java.util.*; // Vector
import java.io.PrintStream;
import pers.hal42.logging.ErrorLogStream;


public abstract class Service implements Comparable {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Service.class);
  private static final NullServiceConfigurator NULLER = new NullServiceConfigurator(dbg);

  // Don't overload a function if you don't want to output anything.  The base class will handle returning NOCOMMENT.
  protected static final String NOCOMMENT="-";

  private String enumer = "UNINITIALIZED SERVICE!";
  protected void setInstanceName(String s){
    enumer=s;
    if(panicker!=null){
      panicker.setName(hostname+"."+enumer);
    }
  }

  // don't have to worry about removing anytime soon
  /**
   * list of services
   */
  private static final Vector list = new Vector(10,10);

  protected ServiceConfigurator configger = NULLER;

  public Service(String enumer, ServiceConfigurator configger, boolean startLogfile) {
    this.enumer = enumer;
    panicker=SendMailPanicStream.Create(hostname()+"."+enumer,this.dbg);
    panicker.sendTo(mailer);
    if(startLogfile) {
      initLog();
    }
    setConfigger(configger);
    panicker.setMailList(mailList());//prime the email channel
    addToList(this);
  }

  public Service(String enumer, ServiceConfigurator configger) {
    this(enumer, configger, false);
  }

  public void setConfigger(ServiceConfigurator configger) {
    this.configger = configger;
    loadConfigs();
  }

  // prevents duplicates in the list
  private static final Monitor serviceAddMonitor = new Monitor("ServiceAddMonitor");
  private static final void addToList(Service service) {
    try {
      serviceAddMonitor.getMonitor();
      if(!list.contains(service)) {
        list.add(service);
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    } finally {
      serviceAddMonitor.freeMonitor();
    }
  }

  public    abstract boolean  isUp();
  public    abstract void     down();
  public    abstract void     up();

  public void bounce() {
    down();
    up();
  }

  // send an email out stating that the auth module was brought up or down.  DON'T send this to authorizer!
  protected final void markStateChange() {
    markStateChange(null);
  }
  protected final void markStateChange(String otherThings) {
    String line = "service brought " + upText() + "! " + StringX.TrivialDefault(otherThings, "");
    println(line);
    PANIC(line);
  }

  public final String upText() {
    return upText(isUp());
  }

  public static final String upText(boolean isup) {
    return isup ? "UP" : "DOWN";
  }

  public synchronized final void initLog() {
    if(logFileMon == null) {
      logFileMon = new Monitor(serviceName() + ".Logfile");
    }
    try {
      logFileMon.getMonitor();
      if(logFile == null) {
        logFile = new LogFile(serviceName(), false);
        pf = logFile.getPrintFork(serviceName());
        panicker.logTo(pf);
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      logFileMon.freeMonitor();
    }
    println(serviceName() + " log started.");
  }

  public LogFile logFile = null;
  protected PrintFork pf = null;
  protected Monitor logFileMon = null;

  public void println(String s) {
    try {
      pf.println(s);
    } catch (Exception e) {
      LogFile.backupLogException("Service.println()", e);
    }
  }

  /**
   * Overload and only put stuff in here that you want loaded at service config set time only.
   */
  protected void loadConfigs() {
    // nothing
  }

  /**
   * Overload if you have a special one ...
   * @returns DEFAULTMAILLIST if you don't have an alertList defined for this service
   */
  public String mailList() {
    return (configger != null) ? configger.getServiceParam(serviceName(), ALERTLIST, defaultMaillist()) : defaultMaillist();
  }

  public String defaultMaillist() {
    return "";//(mailer == null) ? SendMail.GawdsAndPhones.toString() : mailer.emergencyEmail().toString();
  }

  // PANIC logger ...
  public static final String ALERTLIST = "alertList";
  public static PrintFork problemLog=null;
  protected static LogFile paniclogFile=null;
  private static boolean inited = false;
  private static String hostname = "not inited";
  public static final String hostname() {
    return hostname;
  }

  static {
    startLog();
  }

  private static synchronized void startLog() {
    if(paniclogFile == null) {
      paniclogFile = new LogFile("problems", false);
    }
    if(problemLog == null) {
      problemLog = paniclogFile.getPrintFork();
      problemLog.println("Log Started, OK");//will get one of these each boot.
    }
  }

  private static final synchronized void restartLog() {
    // this closes up and restarts the logs.  This is important in case the log locations moved, or are now known
    if(problemLog != null) {
      problemLog = null;
    }
    if(paniclogFile != null) {
      paniclogFile.AtExit();
      paniclogFile = null;
    }
    startLog();
  }

  public static void init(String hostname){
    synchronized(Service.class) {
      if(!inited) {
        restartLog();
        Service.hostname = hostname;
        inited = true;
        problemLog.println("Service Init, OK");//will get one of these each boot.
      }
    }
  }

  // typical service functions ...

  public final String serviceName() {
    return enumer;
  }
  public String svcCnxns() {
    return NOCOMMENT;
  }
  public String svcTxns() {
    return NOCOMMENT;
  }
  public String svcPend() {
    return NOCOMMENT;
  }
  public String svcTimeouts() {
    return NOCOMMENT;
  }
  public String svcAvgTime() {
    return NOCOMMENT;
  }
  public String svcWrites() {
    return NOCOMMENT;
  }
  public String svcReads() {
    return NOCOMMENT;
  }
  public String svcLogFile() {
    return (logFile != null) ? logFile.status() : NOCOMMENT;
  }
  public String svcNotes() {
    return NOCOMMENT;
  }

  // formatting ...

  protected static final String printStats(Accumulator ua) {
    return "" + ua.getCount() + " @ " + ua.getAverage() + " ms ea";
  }

  protected static final String printByteStats(Accumulator ua) {
    return of(Formatter.sizeLong(ua.getTotal()), ua.getCount())+" = "+Formatter.sizeLong(ua.getAverage()) + " B";
  }

  protected static final String printMoneyStats(Accumulator ua) {
//    RealMoney formatter = new RealMoney(ua.getTotal());
    return "" + ua.getCount() + " [" + /*formatter.Image()*/ ua.getTotal() + "]";
  }

  protected static final String of(long This, long That) { // aka div
    return of(Long.toString(This), That);
  }

  protected static final String of(String This, long That) { // aka div
    return of(This, Long.toString(That));
  }

  protected static final String of(String This, String That) { // aka div
    return ""+This+" / "+That;
  }

  protected static final String diskSpaceFree(String path) {
    TextList msgs = new TextList();
    int c = OS.diskfree(path, msgs);
    String output = msgs.itemAt(1);
    String percentUsed = parseForPercent(output);
    return ""+(100 - StringX.parseInt(percentUsed)) + "% disk free";
  }

  private static final String parseForPercent(String toParse) {
    int i = toParse.indexOf("%");
    String percent = "";
    while(i > -1) {
      char c = toParse.charAt(--i);
      if(c == ' ') {
        break;
      }
      percent = "" + c + percent;
    }
    return percent;
  }

  public boolean is(String servicename) {
    return StringX.equalStrings(serviceName(), servicename);
  }

  public boolean isAuthService() {
    return false; // return (this instanceof Authorizer)?  NO!  Means you have to include that package!  BAD!
  }

  // this means that the service manages its configuration by itself
  // if you overload this function and return true,
  // the webpage generation will not include functionality to set database configuration parameters
  // currently, the only class that does this is the LogControlService.
  public boolean selfConfiguring() {
    return false; // DO NOT ever change this; overload and change in subclass
  }

  public int compareTo(Object o) {
    if(o instanceof Service) {
      Service oservice = (Service)o;
      boolean oauth = oservice.isAuthService();
      boolean thisauth = isAuthService();
      if(thisauth) {
        if(oauth) {
          // need more testing
        } else {
          return 1;
        }
      } else {
        if(oauth) {
          return -1;
        } else {
          // need more testing
        }
      }
      return StringX.compareStrings(this.serviceName(), oservice.serviceName());
    }
    return 0;
  }

  public static final Service [] getList() {
    Object [] owes = list.toArray();
    Arrays.sort(owes);
    Service [] services = new Service[owes.length];
    for(int i = owes.length; i-->0;) {
      services[i] = (Service)owes[i];
    }
    return services;
  }

  public static final Service getServiceByName(String serviceName) {
    Service srv = null;
    Service [] services = getList();
    for(int i = services.length; i-->0;) {
      Service service = services[i];
      if(StringX.equalStrings(service.serviceName(), serviceName)) {
        srv = service;
        break;
      }
    }
    return srv;
  }

  // put this LAST!
  public static final SystemService system = new SystemService(null);
}

class NullServiceConfigurator implements ServiceConfigurator {
  private ErrorLogStream dbg;

  public NullServiceConfigurator(ErrorLogStream dbg) {
    this.dbg = dbg;
  }

  public void complain() {
    dbg.ERROR("USING NULL SERVICE CONFIGURATOR!!!! SHOULDN'T EVER!!!!");
  }

  public String getServiceParam(String serviceName, String paramname, String defaultValue) {
    complain();
    return defaultValue;
  }
  public boolean getBooleanServiceParam(String serviceName, String paramname, boolean defaultValue) {
    complain();
    return defaultValue;
  }
  public double getDoubleServiceParam(String serviceName, String paramname, double defaultValue) {
    complain();
    return defaultValue;
  }
  public long getLongServiceParam(String serviceName, String paramname, long defaultValue) {
    complain();
    return defaultValue;
  }
  public int getIntServiceParam(String serviceName, String paramname, int defaultValue) {
    complain();
    return defaultValue;
  }
  public EasyCursor getServiceParams(String serviceName, EasyCursor ezc) {
    complain();
    return new EasyCursor();
  }
  public EasyCursor setServiceParams(String serviceName, EasyCursor ezc) {
    complain();
    return new EasyCursor();
  }
  public EasyCursor getAllServiceParams(String serviceName) {
    complain();
    return new EasyCursor();
  }
  public boolean setServiceParam(String serviceName, String paramname, String paramvalue) {
    complain();
    return false;
  }

}
//$Id: Service.java,v 1.45 2004/02/24 12:24:21 mattm Exp $
