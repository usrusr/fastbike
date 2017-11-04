package de.ulf_schreiber.fastbike.boundingpiecechain;

import de.ulf_schreiber.fastbike.boundingpiecechain.value.Value;

public class DistanceBounds extends Bounds {
    public interface Getters<A extends Getters<A, P>, P extends Step.Getters<P> & Value.CT> extends Bounds.Getters<A,P>{
        double getDistance();
    }



    public interface Aggregate<
            A extends Aggregate<A,G,P> & Getters<A,P>,
            G extends Getters<G,P> & Value.CT,
            P extends Step.Getters<P> & Value.CT
    > extends Bounds.Aggregate<A,G,P>{
    }


    /** closes the selftype hierarchy */
    public interface Interface extends Getters<Interface, Step.Interface>, Value.CT {}

    public abstract static class Base<A extends Getters<A, P>, P extends Step.Getters<P> & Value.CT> extends Bounds.Base<A,P> implements Getters<A,P> {

    }
    public abstract static class BaseWrite<A extends BaseWrite<A,G,P>, G extends Getters<G,P> & Value.CT, P extends Step.Getters<P> & Value.CT>
            extends Bounds.BaseWrite<A,G,P>
            implements Aggregate<A,G,P>, Getters<A,P>{
        abstract void setDistance(double distance);

        @Override
        public void extendBy(G aggregate) {
            super.extendBy(aggregate);
            setDistance(getDistance() + aggregate.getDistance());
        }
    }
}
