package pers.hal42.stream;

import java.util.EventListener;
import java.util.EventObject;

public interface StreamEventListener extends EventListener {

  void notify(EventObject object);

}
