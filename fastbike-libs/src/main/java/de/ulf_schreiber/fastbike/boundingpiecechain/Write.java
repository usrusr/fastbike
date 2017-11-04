package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface Write<
        R extends Read<R>,
        W extends Write<R,W>
    > extends Read<R>{
}
