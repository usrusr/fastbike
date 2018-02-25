package de.ulf_schreiber.fastbike.boundingpiecechain;

/**
 * A copy/paste template for new types
 * <p>replace ExampleParent with the actual parent, ExampleChild with the actual type (obviously...)</p>
 */
public abstract class ExampleChild<
        V extends ExampleChild<V,R,W,G,M,B,L,A>,
        R extends ExampleChild.Reading<R> & ExampleParent.Reading<R>,
        W extends ExampleChild.Writing<R,W> & ExampleChild.Reading<R> & ExampleParent.Writing<R,W>,
        G extends ExampleChild.Grouping<R,G> & ExampleParent.Grouping<R,G>,
        M extends ExampleChild.Merging<R,G,M> & ExampleChild.Grouping<R,G> & ExampleParent.Merging<R,G,M>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
        > extends ExampleParent<V,R,W,G,M,B,L,A>{

    public ExampleChild(int blocksize, double precision) {
        super(blocksize, precision);
    }

    protected interface Reading <
            R extends Reading<R>
            > extends ExampleParent.Reading<R> {
    }
    protected interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, ExampleParent.Writing<R,W> {
    }
    protected interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends ExampleParent.Grouping<R,G> {
    }
    protected interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, ExampleParent.Merging<R,G,M> {
    }
}
