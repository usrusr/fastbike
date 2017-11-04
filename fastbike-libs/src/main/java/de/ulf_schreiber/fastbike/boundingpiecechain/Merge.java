package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface Merge<
        R extends Read<R>,
        G extends Group<R,G>,
        M extends Merge<R,G,M>
    > extends Group<R,G> {
}
