package de.ulf_schreiber.fastbike.boundingpiecechain;


import de.ulf_schreiber.fastbike.boundingpiecechain.value.Value;

public interface ContentType<
        C extends ContentType<C,R,W>,
        R extends Point.Getters<R> & Value.CT,
        W extends Point.Writers<W, R> & Point.Getters<R> & BufferLooking<W, R>
//        B extends Bounds.Getters<B, R> & ClosedType,
//        A extends Bounds.Aggregate<A,B,R> & Bounds.Getters<B,R>
    >
//        extends Bounds.Aggregate<A,B,R>
{

    /** in elements, not bytes */
    int size();
    /** of whole field */
    int bytesize();

    /** @return an array containing */
    Object createArray(int i);

     W createPointLooker();
}
