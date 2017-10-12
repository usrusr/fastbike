package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * <nl>
 *     <li>somewhat memory-efficient</li>
 *     <li>piece table</li>
 *     <li>append-friendly</li>
 *     <li>somewhat fast "point -&gt; closest points along track within radius" function</li>
 *     <li>distance is independent from geometric distance</li>
 *     <li>nodes can have extra data like height, cost,...</li>
 * </nl>
 *
 * @param <P>
 * @param <T>
 */
public class BoundingTree<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P>> {
    int blocksize = 16;
    double resolution = 0.0000001;

    T type;

    void stringifyPoint(Appendable sw, P point){
        try {
         if(point==null) {
                 sw.append("null");
         }else{
             sw.append('[').append(""+point.getLat()).append(':').append(""+point.getLng()).append(']');
         }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean samePoint(P a, P b){
        if(a==null&&b==null) return true;
        if(a==null||b==null) return false;
        return a.sameAs(b, resolution);
    }

    static abstract class Node<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P>, N extends Node<P,T,N>> extends MutableBoundingBox{
        GroupNode parent;
        N previous;
        N next;

        double distance;

        void growBoundingBox(P include){
            double lng = include.getLng();
            double lat = include.getLng();
            west = Math.min(west, lng);
            east = Math.max(east, lng);
            south = Math.min(south, lat);
            north = Math.max(north, lat);

            if(parent!=null) growBoundingBox(include);
        }
        void build(BoundingBuilder<P, T> builder) {
            if(builder.toSkip<=0 && builder.toVisit>= distance){
                builder.toVisit-= distance;
                builder.extendBoundsFor(this);
            }
        }
    }
    static class LeafNode<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P> > extends Node<P,T,LeafNode<P,T>> {
        Object array;
        /** in elements */
        int size;
        double distance;


        LeafNode(BoundingTree<P,T> tree, Iterator<Step> it, P looker) {
            array = tree.type.createArray(tree.blocksize*tree.type.size());
            int i=0;
            looker.at(array,0,0);
            while(it.hasNext() && i<tree.blocksize){
                Step next = it.next();

                looker.copyFrom(next);
                extendBoundsFor(next.getLat(), next.getLng());
                distance+=next.getDist();

                looker.move(1);
            }
        }

        @Override
        public void build(BoundingBuilder<P, T> builder) {
            super.build(builder);

            P looker = builder.looker;
            looker.at(array, 0, 0);
            final double resolution = builder.tree.resolution;

            for(int i=0;i<size;i++){
                double dist = looker.getDist();

                double restToSkip = builder.toSkip-dist;
                if(restToSkip> resolution){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    builder.extend(looker.getLat(), looker.getLng());
                    double restToVisit = builder.toVisit-dist;
                    if(restToSkip< resolution){
                        builder.toVisit=0;
                        return;
                    }else{
                        builder.toVisit = restToVisit;
                    }
                }
                looker.move(1);
            }
        }
    }
    static class GroupNode<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P> > extends Node<P,T,GroupNode<P,T>> {
        Node<P,T,?>[] children;
        int len;
        boolean containsLeaves = false;

        GroupNode(BoundingTree<P,T> tree) {
            children = new Node[tree.blocksize*tree.type.size()];
        }

        @Override
        public void build(BoundingBuilder<P, T> builder) {
            super.build(builder);
            if(builder.toVisit<builder.tree.resolution) return;

            for(Node<P,T,?> n : children) {
                if(n.distance < builder.toSkip){
                    builder.toSkip-=n.distance;
                }else{
                    n.build(builder);
                    if(builder.toVisit<builder.tree.resolution) return;
                }
            }
        }

        /** @return newChild if already full */
        public Node<P, T, ?> add(Node<P, T, ?> newChild) {
            if(len>=children.length){
                return newChild;
            }
            distance+=newChild.distance;
            extendBoundsFor(newChild);
            children[len]=newChild;
            len++;
            return null;
        }
    }

    static class Piece<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P> >{
        final Node<P,T,?> root;
        final double offset; // into node, e.g. 0 when the node was freshly created
        final double distance;
        final double west;
        final double north;
        final double east;
        final double south;
        Piece(Node<P,T,?> root, double offset, double distance, BoundingTree<P,T> tree) {
            this.root = root;
            this.offset = offset;
            this.distance = distance;

            BoundingBuilder<P, T> boundingBuilder = new BoundingBuilder<P,T>(offset, distance, tree);
            root.build(boundingBuilder);
            west = boundingBuilder.west;
            north = boundingBuilder.north;
            east = boundingBuilder.east;
            south = boundingBuilder.south;
        }
    }
    static class Undo<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P>> {
        int at;
        int removed; // first n pieces are removed, rest added
        Piece<P, T>[] pieces;

        final void apply(BoundingTree<P, T> tree) {
            ListIterator<Piece> iterator = tree.pieces.listIterator(at);
            int i = 0;
            for (; i < removed; i++) iterator.remove();
            int length = pieces.length;
            for (; i < length; i++) iterator.add(pieces[i]);
        }

        final void unapply(BoundingTree<P, T> tree) {
            ListIterator<Piece> iterator = tree.pieces.listIterator(at);
            int i = removed;
            int length = pieces.length;
            for (; i < length; i++) iterator.remove();

            for (i = 0; i < removed; i++) iterator.add(pieces[i]);
        }
    }

    public static class BoundingBuilder<P extends Step.Implementation<P> & BufferLooking<T>, T extends ContentType<T, P>> extends MutableBoundingBox {
        private final BoundingTree<P, T> tree;

        P looker;
        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance, BoundingTree<P, T> tree) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            this.tree = tree;
            looker = tree.type.createPointLooker();
        }

        public void extend(double lat, double lng) {

        }
    }
    private abstract static class MutableBoundingBox {
        double west = 1;
        double north = -1;
        double east = -1;
        double south = 1;

        void extendBoundsFor(double lat, double lng){
            west = Math.min(west, lng);
            east = Math.max(east, lng);
            south = Math.min(south, lat);
            north = Math.max(north, lat);
        }
        void extendBoundsFor(MutableBoundingBox other){
            west = Math.min(west, other.west);
            east = Math.max(east, other.east);
            south = Math.min(south, other.south);
            north = Math.max(north, other.north);
        }
    }

    private final LinkedList<Piece> pieces = new LinkedList<>();
    private final LinkedList<Undo> undos = new LinkedList<>();
    public void append(Iterable<P> points){
        replace(Double.MAX_VALUE, 0, points);
    }
    public void insert(double at, Iterable<P> points){
        replace(at, 0, points);
    }
    public void delete(double at, double distance){
        replace(at, distance, Collections.<P>emptyList());
    }
    public void replace(double at, double distance, Iterable<P> points){

    }


    Node<P,T,?> makeTree(Iterable<Step> iterable){
        P looker = type.createPointLooker();
        Iterator<Step> it = iterable.iterator();
        LeafNode<P, T> first = new LeafNode<P,T>(this, it, looker);

        while(it.hasNext()){


        }



        Node<P, T, ?> ret = first;
        return ret;
    }

    /** @return new root or self (will always return self if called with parent) */
    Node<P,T,?> extendTree(GroupNode<P,T> group, GroupNode<P,T> parent, Iterator<Step> iterator, P looker){
        GroupNode<P, T> cur = group;
        while(iterator.hasNext()) {
            if (cur.len == this.blocksize) {
                if(parent==null){
                    GroupNode<P, T> newParent = new GroupNode<>(this);
                    newParent.add(cur);
                    return extendTree(newParent, null, iterator, looker);
                }else {
                    // continue in parent call
                    return group;
                }
            }
            if(cur.containsLeaves){
                LeafNode<P, T> newChild = new LeafNode<>(this, iterator, looker);

                cur.add(newChild);
            }else{
                GroupNode<P, T> newChild = new GroupNode<>(this);
                extendTree(newChild, cur, iterator, looker);
                cur.add(newChild);
            }
        }
        return group;
    }

}
