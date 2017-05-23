package pers.hal42.thread;

public interface QActor<Qtype> {
  void runone(Qtype fromq);//called each time an object is pulled from a q for processing.

  void Stop();//called when QAgent has stopped.
}

