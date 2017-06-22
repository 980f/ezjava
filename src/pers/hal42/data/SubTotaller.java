package pers.hal42.data;


import pers.hal42.lang.LinearMap;
import pers.hal42.math.Accumulator;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

import java.util.Iterator;
import java.util.Map;

public class SubTotaller implements isEasy {
  Accumulator grandTotal = new Accumulator();
  Map<String, Accumulator> subTotals;

  public SubTotaller() {
//see initializers
  }

  public SubTotaller(TextList primer) {
    this();
    prime(primer);
  }

  public Accumulator reCalcGrand() {
    grandTotal.zero();
    TextList names = subtotalNames();
    for (String key : names.storage) {
      Accumulator acc = getAccumulator(key);
      grandTotal.add(acc);
    }
    return grandTotal;
  }

  public int add(String key, long amount) {
    grandTotal.add(amount);
    Accumulator acc = getAccumulator(key);
    acc.add(amount); // cents
    return Count();
  }

  // fixed a bug recently.  This wasn't adding the added subtotallers into the grand total.
  public SubTotaller add(SubTotaller rhs) {
    TextList names = rhs.subtotalNames();
    for (int i = names.size(); i-- > 0; ) {
      String key = names.itemAt(i);
      Accumulator acc = rhs.getAccumulator(key);
      getAccumulator(key).add(acc);
      grandTotal.add(acc);
    }
    return this;
  }

  public SubTotaller zero() {
    grandTotal.zero(); // zero the grand, then ...
    TextList names = subtotalNames();
    for (int i = names.size(); i-- > 0; ) {
      getAccumulator(names.itemAt(i)).zero();
    }
    return this;
  }

  // creates 0 valued accumulators at need.
  public final Accumulator getAccumulator(String key) {
    assureTotalersExist();
    return subTotals.computeIfAbsent(key, k -> new Accumulator());
  }

  protected void assureTotalersExist() {
    if (subTotals == null) {
      subTotals = new LinearMap<>(10);
    }
  }

  public long Total() {
    return grandTotal.getTotal();
  }

  public int Count() {
    return (int) grandTotal.getCount();
  }

  public Accumulator grand() {
    return grandTotal;
  }

  public Iterator<String> subbers() {
    assureTotalersExist();
    return subTotals.keySet().iterator();
  }

  public TextList subtotalNames() {
    TextList names = new TextList();
    subbers().forEachRemaining(names::add);
    return names;
  }

  public void prime(TextList tl) {
    for (int i = tl.size(); i-- > 0; ) {
      getAccumulator(tl.itemAt(i));
    }
  }

  public String toString() {
    EasyCursor ezp = new EasyCursor();
    save(ezp);
    return ezp.toString();
  }

  /////////////////
// isEasy()
  public void save(EasyCursor ezp) {
    ezp.setObject("grand", grand());
    ezp.saveMap("subs", subTotals);
  }

  public void load(EasyCursor ezp) {
    grandTotal = ezp.getObject("grand", Accumulator.class);
    subTotals = ezp.getMap("subs", Accumulator.class);
  }
}
