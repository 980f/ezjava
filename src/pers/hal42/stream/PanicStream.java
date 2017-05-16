package pers.hal42.stream;

public interface PanicStream {
  void PANIC(String re);

  void PANIC(String re, Object panicky);
}
