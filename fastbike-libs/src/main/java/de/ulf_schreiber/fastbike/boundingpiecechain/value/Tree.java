package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import java.util.Iterator;

public class Tree<
        V extends CoordinateDistance<R,W,G,M>,
        R extends CoordinateDistance.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
        G extends CoordinateDistance.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
        B,
        L extends Value.Looking<L,R,W,B>
    > {
    final V meta;

    public Tree(V meta) {
        this.meta = meta;
    }






    int blocksize = 16;
//    double resolution = 0.0000001;
    void stringifyPoint(Appendable sw, R point){
        meta.stringifyPoint(sw,point);
    }

    boolean samePoint(R a, R b){
        return meta.sameAs(a,b);
    }

    static abstract class Node<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Looking<L,R,W,B>,
            N extends Node<V,R,W,G,M,B,L,N>
        > implements CoordinateDistance.Merging<R,G,M>{
//        void build(BoundingBuilder<R, M> builder) {
//            if(builder.toSkip<=0 && builder.toVisit>= distance){
//                builder.toVisit-= distance;
//                builder.extendBoundsFor(this);
//            }
//        }

        public void build(Tree<V,R,W,G,M,B,L>.BoundingBuilder builder) {
            V meta = builder.tree.meta;
            if(builder.toSkip < meta.precision) {
                double rest = builder.toVisit - getDistance();
                if(rest > meta.precision){
                    builder.toVisit = rest;
                    meta.extendBy(builder.result, this.group());
                }
            }
        }

        abstract int depth();
    }
    static abstract class LeafNode<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Looking<L,R,W,B>
    > extends Node<V,R,W,G,M,B,L,LeafNode<V,R,W,G,M,B,L>> {
        B array;
        int elements = 0;

        LeafNode(Tree<V,R,W,G,M,B,L> tree, Iterator<R> it, L looker, M bounds) {
            array = looker.createBuffer(tree.blocksize);
            looker.wrap(array,tree.blocksize,0);
            while(it.hasNext() && elements <tree.blocksize){
                R next = it.next();
                tree.meta.copy(next, looker.write());
                tree.meta.extendBy(bounds, next);
                looker.moveRelative(1);
                elements++;
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(Tree<V,R,W,G,M,B,L>.BoundingBuilder builder) {
            super.build(builder);

            V meta = builder.tree.meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            L looker = builder.valueLooker;
            looker.wrap(array, builder.tree.blocksize, 0);

            for(int i=0;i<elements;i++){
                double dist = looker.read().getDistance();

                double restToSkip = builder.toSkip-dist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    meta.extendBy(builder.result, looker.read());
                    double restToVisit = builder.toVisit-dist;
                    if(restToSkip< precision){
                        builder.toVisit=0;
                        return;
                    }else{
                        builder.toVisit = restToVisit;
                    }
                }
                looker.moveRelative(1);
            }
        }
    }
    abstract static class GroupNode<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Looking<L,R,W,B> & CoordinateDistance.Writing<R,W>
        > extends Node<V,R,W,G,M,B,L,GroupNode<V,R,W,G,M,B,L>> {

        Node<V,R,W,G,M,B,L,?>[] children;
        int len;
        int depth;

        GroupNode(Tree<V,R,W,G,M,B,L> tree) {
            children = new Node[tree.blocksize];
        }

        @Override
        public void build(Tree<V,R,W,G,M,B,L>.BoundingBuilder builder) {
            super.build(builder);

            V meta = builder.tree.meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            for(Node<V, R, W, G, M, B, L, ?> child : children) {
                double childDist = child.getDistance();
                if(childDist < builder.toSkip){
                    builder.toSkip-=childDist;
                }else{
                    child.build(builder);
                    if(builder.toVisit < precision) return;
                }
            }
        }

        /** @return newChild if already full */
        public Node<V,R,W,G,M,B,L,?> add(Node<V,R,W,G,M,B,L,?> newChild, Tree<V,R,W,G,M,B,L> tree) {
            if(len>=children.length){
                return newChild;
            }

            tree.meta.extendBy(this.merge(), newChild.group());
            children[len]=newChild;
            len++;
            return null;
        }
    }
//
//    static class Piece<R extends Step.R<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?> >{
//        final Node<R,T,?> root;
//        final double offset; // into node, e.g. 0 when the node was freshly created
//        final double distance;
//        final double getWest;
//        final double getNorth;
//        final double getEast;
//        final double getSouth;
//        Piece(Node<R,T,?> root, double offset, double distance, BoundingTree<R,T> tree) {
//            this.root = root;
//            this.offset = offset;
//            this.distance = distance;
//
//            BoundingBuilder<R, T> boundingBuilder = new BoundingBuilder<R,T>(offset, distance, tree);
//            root.build(boundingBuilder);
//            getWest = boundingBuilder.getWest;
//            getNorth = boundingBuilder.getNorth;
//            getEast = boundingBuilder.getEast;
//            getSouth = boundingBuilder.getSouth;
//        }
//    }
//    static class Undo<R extends Step.R<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?>> {
//        int wrap;
//        int removed; // first n pieces are removed, rest added
//        Piece<R, T>[] pieces;
//
//        final void apply(BoundingTree<R, T> tree) {
//            ListIterator<Piece> iterator = tree.pieces.listIterator(wrap);
//            int i = 0;
//            for (; i < removed; i++) iterator.remove();
//            int length = pieces.length;
//            for (; i < length; i++) iterator.add(pieces[i]);
//        }
//
//        final void unapply(BoundingTree<R, T> tree) {
//            ListIterator<Piece> iterator = tree.pieces.listIterator(wrap);
//            int i = removed;
//            int length = pieces.length;
//            for (; i < length; i++) iterator.remove();
//
//            for (i = 0; i < removed; i++) iterator.add(pieces[i]);
//        }
//    }
//
    public class BoundingBuilder {
        private final Tree<V,R,W,G,M,B,L> tree;

        M result;
        L valueLooker;

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance, Tree<V,R,W,G,M,B,L> tree) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            this.tree = tree;
            result = tree.meta.createMutableBounds();
            valueLooker = tree.meta.<L,B>createElementWriter();
        }
    }
//    private abstract static class MutableBoundingBox {
//        double getWest = 1;
//        double getNorth = -1;
//        double getEast = -1;
//        double getSouth = 1;
//
//        void extendBoundsFor(double lat, double lng){
//            getWest = Math.min(getWest, lng);
//            getEast = Math.max(getEast, lng);
//            getSouth = Math.min(getSouth, lat);
//            getNorth = Math.max(getNorth, lat);
//        }
//        void extendBoundsFor(MutableBoundingBox other){
//            getWest = Math.min(getWest, other.getWest);
//            getEast = Math.max(getEast, other.getEast);
//            getSouth = Math.min(getSouth, other.getSouth);
//            getNorth = Math.max(getNorth, other.getNorth);
//        }
//    }
//
//    private final LinkedList<Piece> pieces = new LinkedList<>();
//    private final LinkedList<Undo> undos = new LinkedList<>();
//    public void append(Iterable<R> points){
//        replace(Double.MAX_VALUE, 0, points);
//    }
//    public void insert(double wrap, Iterable<R> points){
//        replace(wrap, 0, points);
//    }
//    public void delete(double wrap, double distance){
//        replace(wrap, distance, Collections.<R>emptyList());
//    }
//    public void replace(double wrap, double distance, Iterable<R> points){
//
//    }
//
//
//    Node<R,T,?> makeTree(Iterable<Step> iterable){
//        R looker = type.createPointLooker();
//        Iterator<Step> it = iterable.iterator();
//        LeafNode<R, T> first = new LeafNode<R,T>(this, it, looker);
//
//        while(it.hasNext()){
//
//
//        }
//
//
//
//        Node<R, T, ?> ret = first;
//        return ret;
//    }
//
//    /** @return new root or self (will always return self if called with parent) */
//    Node<R,T,?> extendTree(GroupNode<R,T> group, GroupNode<R,T> parent, Iterator<Step> iterator, R looker){
//        GroupNode<R, T> cur = group;
//        while(iterator.hasNext()) {
//            if (cur.len == this.blocksize) {
//                if(parent==null){
//                    GroupNode<R, T> newParent = new GroupNode<>(this);
//                    newParent.add(cur);
//                    return extendTree(newParent, null, iterator, looker);
//                }else {
//                    // continue in parent call
//                    return group;
//                }
//            }
//            if(cur.containsLeaves){
//                LeafNode<R, T> newChild = new LeafNode<>(this, iterator, looker);
//
//                cur.add(newChild);
//            }else{
//                GroupNode<R, T> newChild = new GroupNode<>(this);
//                extendTree(newChild, cur, iterator, looker);
//                cur.add(newChild);
//            }
//        }
//        return group;
//    }
//



}
