package pers.hal42.util;

public interface QActor {
  public void runone(Object fromq);//called each time an object is pulled from a q for processing.
  public void Stop();//called when QAgent has stopped.
}

