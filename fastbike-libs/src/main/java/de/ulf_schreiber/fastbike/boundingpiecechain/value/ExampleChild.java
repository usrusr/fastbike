package de.ulf_schreiber.fastbike.boundingpiecechain.value;

/**
 * A copy/paste template for new types
 * <p>replace ExampleParent with the actual parent, ExampleChild with the actual type (obviously...)</p>
 */
public abstract class ExampleChild<
        R extends ExampleChild.Reading<R> & ExampleParent.Reading<R>,
        W extends ExampleChild.Writing<R,W> & ExampleChild.Reading<R> & ExampleParent.Writing<R,W>,
        G extends ExampleChild.Grouping<R,G> & ExampleParent.Grouping<R,G>,
        M extends ExampleChild.Merging<R,G,M> & ExampleChild.Grouping<R,G> & ExampleParent.Merging<R,G,M>
        > extends ExampleParent<R,W,G,M>{

    public ExampleChild(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends ExampleParent.Reading<R> {
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, ExampleParent.Writing<R,W> {
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends ExampleParent.Grouping<R,G> {
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, ExampleParent.Merging<R,G,M> {
    }
    public interface PublicRead extends Reading<PublicRead> {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup> {

    }
}
