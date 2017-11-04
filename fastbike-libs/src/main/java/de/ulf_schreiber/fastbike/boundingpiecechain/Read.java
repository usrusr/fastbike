package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface Read<R extends Read<R>> {
    boolean sameAs(R other, double precision);
}
