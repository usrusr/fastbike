package de.ulf_schreiber.fastbike.boundingpiecechain.value;


/**
 * parent for {@link ExampleChild}, use {@link ExampleChild} as copy/paste template
 */
public abstract class ExampleParent<
        R extends ExampleParent.Reading<R> & Value.Reading<R> & Value.CT,
        W extends ExampleParent.Writing<R,W> & ExampleParent.Reading<R> & Value.Writing<R,W> & Value.CT,
        G extends ExampleParent.Grouping<R,G> & Value.Grouping<R,G> & Value.CT,
        M extends ExampleParent.Merging<R,G,M> & ExampleParent.Grouping<R,G> & Value.Merging<R,G,M> & Value.CT
        > extends Value<R,W,G,M>{

    public ExampleParent(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R> & CT
            > extends Value.Reading<R> {
    }
    interface Writing<
            R extends Reading<R> & CT,
            W extends Writing<R,W> & CT
            > extends Reading<R>, Value.Writing<R,W> {
    }
    interface Grouping<
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT
            > extends Value.Grouping<R,G> {
    }
    interface Merging <
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT,
            M extends Merging<R,G,M>  & CT
            > extends Grouping<R,G>, Value.Merging<R,G,M> {
    }
    public interface PublicRead extends Reading<PublicRead>, CT {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup>, CT {

    }


}
