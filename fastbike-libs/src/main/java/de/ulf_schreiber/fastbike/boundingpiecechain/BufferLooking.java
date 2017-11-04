package de.ulf_schreiber.fastbike.boundingpiecechain;

import de.ulf_schreiber.fastbike.boundingpiecechain.value.Value;

public interface BufferLooking<W extends Point.Writers<W, P> & Point.Getters<P> , P extends Point.Getters<P> & Value.CT> extends Point.Writers<W, P> {
    /**
     * @param array
     * @param index in groups elements
     * @param offset extra single elements
     */
    void at(Object array, int index, int offset);
    void move(int direction);
    /** @return highest allowed index+1*/
    int size();
}
