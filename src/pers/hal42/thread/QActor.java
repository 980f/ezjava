package pers.hal42.thread;

public interface QActor<Qtype> {
  void runone(Qtype fromq);//called each time an object is pulled from a q for processing.

  /** called when QAgent has stopped. */
  default void Stop() {

  }

  /** called when QAgent has been idle for at least its idletime. Won't get called if an idleObject has been defined. */
  default void periodically() {

  }
}

