package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface BufferLooking<C extends ContentType>{
    /**
     * @param array
     * @param index in groups elements
     * @param offset extra single elements
     */
    void at(Object array, int index, int offset);
    void move(int direction);
    /** @return highest allowed index+1*/
    int size();
    C type();
}
