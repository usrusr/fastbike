package de.ulf_schreiber.fastbike.boundingpiecechain;

public class DistanceBounds extends Bounds {
    public interface Getters<A extends Getters<A, P>, P extends Step.Getters<P> & ClosedType> extends Bounds.Getters<A,P>{
        double getDistance();
    }



    public interface Aggregate<
            A extends Aggregate<A,G,P> & Getters<A,P>,
            G extends Getters<G,P> & ClosedType,
            P extends Step.Getters<P> & ClosedType
    > extends Bounds.Aggregate<A,G,P>{
    }


    /** closes the selftype hierarchy */
    public interface Interface extends Getters<Interface, Step.Interface>, ClosedType{}

    public abstract static class Base<A extends Getters<A, P>, P extends Step.Getters<P> & ClosedType> extends Bounds.Base<A,P> implements Getters<A,P> {

    }
    public abstract static class BaseWrite<A extends BaseWrite<A,G,P>, G extends Getters<G,P> & ClosedType, P extends Step.Getters<P> & ClosedType>
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
