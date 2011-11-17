package ru.hh.util;

public class Pair<A, B> {
  public final A fst;
  public final B snd;

  public Pair(A fst, B snd) {
    this.fst = fst;
    this.snd = snd;
  }
  
  public static <A, B> Pair<A, B> of(A fst, B snd) {
    return new Pair<A, B>(fst, snd);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair pair = (Pair) o;

    if (fst != null ? !fst.equals(pair.fst) : pair.fst != null) return false;
    if (snd != null ? !snd.equals(pair.snd) : pair.snd != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = fst != null ? fst.hashCode() : 0;
    result = 31 * result + (snd != null ? snd.hashCode() : 0);
    return result;
  }
}
