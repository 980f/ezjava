package pers.hal42.service;

import pers.hal42.transport.EasyCursor;

/**
 * Title:        $Source: /cvs/src/net/paymate/util/ServiceConfigurator.java,v $
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.8 $
 */

public interface ServiceConfigurator {

  public String getServiceParam(String serviceName, String paramname, String defaultValue);
  public double getDoubleServiceParam(String serviceName, String paramname, double defaultValue);
  public long getLongServiceParam(String serviceName, String paramname, long defaultValue);
  public int getIntServiceParam(String serviceName, String paramname, int defaultValue);
  public boolean getBooleanServiceParam(String serviceName, String paramname, boolean defaultValue);
  public EasyCursor getServiceParams(String serviceName, EasyCursor ezc);
  public EasyCursor setServiceParams(String serviceName, EasyCursor ezc); // unconditionally sets them based on the ezc passed in
  public boolean setServiceParam(String serviceName, String paramname, String paramvalue); // unconditionally sets it; returns the value it set
  public EasyCursor getAllServiceParams(String serviceName);
}
