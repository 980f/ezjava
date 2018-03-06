package pers.hal42.ext;

import pers.hal42.lang.LinearMap;
import pers.hal42.lang.StringX;
import pers.hal42.text.StringIterator;
import pers.hal42.text.cmd;

public class ArgMap extends LinearMap<String, String> {

  public ArgMap(StringIterator args) {
    parse(args);
  }

  public void parse(StringIterator args) {
    cmd opt = new cmd();
    while (args.hasNext()) {
      String arg = args.next();
      if (!StringX.NonTrivial(arg)) {
        continue;
      }
      if (arg.startsWith("#")) { //commented out item
        continue;
      }
      opt.split(arg, '=');
//          if (!opt.hasValue()) {
//            dbg.ERROR("argument has no operand, perhaps a gratuitous space or missing '='?: {0}", arg);
//            continue;
//          }
      put(opt.key, opt.value);
    }
  }

  public String getValue(String key) {
    return get(key);
  }
}
