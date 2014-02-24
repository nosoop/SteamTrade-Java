package com.poonso.scrapbanktf.util;

/**
 * A mutable integer class.
 * 
 * @author Top-Cat (?)
 */
public class MutableInt implements Comparable<MutableInt> {

    int value = 1;

    public MutableInt() {
    }

    public MutableInt(int init) {
        value = init;
    }

    public void increment() {
        ++value;
    }

    public void add(int ammount) {
        value = value + ammount;
    }

    public int get() {
        return value;
    }

    @Override
    public int compareTo(MutableInt arg0) {
        return get() == arg0.get() ? 0 : get() > arg0.get() ? -1 : 1;
    }

    public void decrement() {
        --value;
    }

    public void set(int size) {
        value = size;
    }
}
