package de.ulf_schreiber.fastbike.boundingpiecechain;




public interface ContentType<T extends ContentType<T, P>, P extends Point & BufferLooking<T>> {

    /** in elements, not bytes */
    int size();
    /** of whole field */
    int bytesize();

    /** @return an array containing */
    Object createArray(int i);

     P createPointLooker();
}
