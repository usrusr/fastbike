package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import de.ulf_schreiber.fastbike.boundingpiecechain.Read;

/**
 * A copy/paste template for new types
 * <p>replace ExampleParent with the actual parent, ExampleChild with the actual type (obviously...)</p>
 */
public abstract class ExampleChild<
        R extends ExampleChild.Reading<R> & ExampleParent.Reading<R> & Value.CT,
        W extends ExampleChild.Writing<R,W> & ExampleChild.Reading<R> & ExampleParent.Writing<R,W> & Value.CT,
        G extends ExampleChild.Grouping<R,G> & ExampleParent.Grouping<R,G> & Value.CT,
        M extends ExampleChild.Merging<R,G,M> & ExampleChild.Grouping<R,G> & ExampleParent.Merging<R,G,M> & Value.CT
        > extends ExampleParent<R,W,G,M>{

    public ExampleChild(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R> & CT
            > extends ExampleParent.Reading<R> {
    }
    interface Writing<
            R extends Reading<R> & CT,
            W extends Writing<R,W> & CT
            > extends Reading<R>, ExampleParent.Writing<R,W> {
    }
    interface Grouping<
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT
            > extends ExampleParent.Grouping<R,G> {
    }
    interface Merging <
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT,
            M extends Merging<R,G,M>  & CT
            > extends Grouping<R,G>, ExampleParent.Merging<R,G,M> {
    }
    public interface PublicRead extends Reading<PublicRead>, CT {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup>, CT {

    }
}
