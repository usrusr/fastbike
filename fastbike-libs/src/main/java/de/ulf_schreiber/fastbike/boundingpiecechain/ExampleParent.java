package de.ulf_schreiber.fastbike.boundingpiecechain;


/**
 * parent for {@link ExampleChild}, use {@link ExampleChild} as copy/paste template
 */
public abstract class ExampleParent<
        V extends ExampleParent<V,R,W,G,M,B,L,A>,
        R extends ExampleParent.Reading<R> & Value.Reading<R>,
        W extends ExampleParent.Writing<R,W> & ExampleParent.Reading<R> & Value.Writing<R,W>,
        G extends ExampleParent.Grouping<R,G> & Value.Grouping<R,G>,
        M extends ExampleParent.Merging<R,G,M> & ExampleParent.Grouping<R,G> & Value.Merging<R,G,M>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>
        > extends Value<V,R,W,G,M,B,L,A>{

    public ExampleParent(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends Value.Reading<R> {
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, Value.Writing<R,W> {
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends Value.Grouping<R,G> {
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, Value.Merging<R,G,M> {
    }
    public interface PublicRead extends Reading<PublicRead> {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup> {

    }


}
