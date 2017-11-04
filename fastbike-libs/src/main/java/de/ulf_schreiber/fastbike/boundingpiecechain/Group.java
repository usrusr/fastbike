package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface Group<
        R extends Read<R>,
        G extends Group<R,G>
    > {
}
