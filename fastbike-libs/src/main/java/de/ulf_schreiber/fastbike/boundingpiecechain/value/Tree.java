package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import java.util.Iterator;

public abstract class Tree<
        V extends CoordinateDistance<R,W,G,M>,
        R extends CoordinateDistance.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
        G extends CoordinateDistance.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>
    > {
    final V meta;

    public Tree(V meta) {
        this.meta = meta;
    }

    int blocksize = 16;

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
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>,
            N extends Node<V,R,W,G,M,B,L,A,N>
        > {
//        void build(BoundingBuilder<R, M> builder) {
//            if(builder.toSkip<=0 && builder.toVisit>= distance){
//                builder.toVisit-= distance;
//                builder.extendBoundsFor(this);
//            }
//        }
        B array;
        int elements = 0;
        abstract void build(Tree<V,R,W,G,M,B,L,A>.BoundingBuilder builder);
//        public void build(Tree<V,R,W,G,M,B,L,A>.BoundingBuilder builder) {
//            V meta = builder.tree.meta;
//            if(builder.toSkip < meta.precision) {
//                double rest = builder.toVisit - getDistance();
//                if(rest > meta.precision){
//                    builder.toVisit = rest;
//                    meta.extendBy(builder.result, this.group());
//                }
//            }
//        }

        abstract int depth();
    }
    static abstract class LeafNode<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>
    > extends Node<V,R,W,G,M,B,L,A,LeafNode<V,R,W,G,M,B,L,A>> {


        LeafNode(Tree<V,R,W,G,M,B,L,A> tree, Iterator<R> it, L elemLooker, M mutableBoundsReturn) {
            final int blocksize = tree.blocksize;
            final V meta = tree.meta;
            array = elemLooker.createBuffer(blocksize);
            elemLooker.wrap(array, blocksize,0);
            while(it.hasNext() && elements < blocksize){
                R next = it.next();
                meta.copy(next, elemLooker.write());
                meta.extendBy(mutableBoundsReturn, next);
                elemLooker.moveRelative(1);
                elements++;
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(Tree<V,R,W,G,M,B,L,A>.BoundingBuilder builder) {

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
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>
        > extends Node<V,R,W,G,M,B,L,A,GroupNode<V,R,W,G,M,B,L,A>> {

        Node<V,R,W,G,M,B,L,A,?>[] children;

        int depth;

        @Override
        int depth() {
            return depth;
        }

        GroupNode(Tree<V,R,W,G,M,B,L,A> tree, Iterator<R> it, Value.Editor<L,R,W,B> elemLooker, Value.Editor<A,G,M,B> groupLooker, M mutableBoundsReturn, Node<V,R,W,G,M,B,L,A,?> firstChild, int depth){
            children = new Node[tree.blocksize];
            array = groupLooker.createBuffer(tree.blocksize);
            final V meta = tree.meta;
            this.depth = depth;

            groupLooker.wrap(array, 0, 0);

            M groupLookerWrite = groupLooker.write();
            G mutableBoundsReturnRead = mutableBoundsReturn.group();

            if(firstChild!=null) { // creating a new root
                children[0]=firstChild;
                meta.clearMerge(groupLookerWrite);
                meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
            }

            while(it.hasNext() && elements<tree.blocksize){
                Node<V, R, W, G, M, B, L, A, ?> newNode;

                if(depth==1){
                    groupLooker.moveAbsolute(elements);
                    newNode = tree.createLeafNode(tree, it, elemLooker, groupLookerWrite);
                } else {
                    meta.clearMerge(mutableBoundsReturn);
                    newNode = tree.createGroupNodeNode(tree, it, elemLooker, groupLooker, mutableBoundsReturn, null, depth-1);
                    groupLooker.wrap(array, elements, 0);
                    meta.clearMerge(groupLookerWrite);
                    meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
                }
                children[elements] = newNode;
                elements++;
            }

            meta.clearMerge(mutableBoundsReturn);
            groupLooker.wrap(array, 0, 0);
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                meta.extendBy(mutableBoundsReturn, groupLooker.read());
            }
        }

        @Override
        public void build(Tree<V,R,W,G,M,B,L,A>.BoundingBuilder builder) {
            V meta = builder.tree.meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            A groupLooker = builder.groupLooker;
            meta.clearMerge(groupLooker.write());
            groupLooker.wrap(array, 0, 0);
            G read = groupLooker.read();
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                double childDist = read.getDistance();

                double restToSkip = builder.toSkip-childDist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    double restToVisit = builder.toVisit-childDist;
                    if(restToVisit > -precision){
                        meta.extendBy(builder.result, read);
                        builder.toVisit=restToVisit;
                    }else{
                        children[i].build(builder);
                        return;
                    }
                }
            }
        }
    }
    class Segment {
        final Node<V,R,W,G,M,B,L,A,?> children;
        final G bounds;

        Segment(Node<V, R, W, G, M, B, L, A, ?> children, G bounds) {
            this.children = children;
            this.bounds = bounds;
        }
    }
    abstract LeafNode<V,R,W,G,M,B,L,A> createLeafNode(Tree<V,R,W,G,M,B,L,A> tree, Iterator<R> it, Value.Editor<L,R,W,B> looker, M bounds);
    abstract GroupNode<V,R,W,G,M,B,L,A> createGroupNodeNode(Tree<V,R,W,G,M,B,L,A> tree, Iterator<R> it, Value.Editor<L,R,W,B> looker, Value.Editor<A,G,M,B> groupLooker, M bounds, Node<V,R,W,G,M,B,L,A,?> firstChild, int depth);

    Segment makeTree(Iterable<R> iterable){
        Value.Editor<L,R,W,B> elemLooker = meta.<L,B>createElementWriter();
        Value.Editor<A,G,M,B> groupLooker = meta.<A,B>createAggregateWriter();
        Iterator<R> it = iterable.iterator();
        M mutableBounds = meta.createMutableBounds();
        LeafNode<V,R,W,G,M,B,L,A> first = createLeafNode(this, it, elemLooker, mutableBounds);
        Node<V,R,W,G,M,B,L,A,?> root = first;
        int depth = 0;
        while(it.hasNext()){
            depth++;
            root = createGroupNodeNode(this, it, elemLooker, groupLooker, mutableBounds, root, depth);
        }

        return new Segment(root, mutableBounds.group());


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
        private final Tree<V,R,W,G,M,B,L,A> tree;

        final M result;
        final L valueLooker;
        final A groupLooker;

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance, Tree<V,R,W,G,M,B,L,A> tree) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            this.tree = tree;
            result = tree.meta.createMutableBounds();
            valueLooker = tree.meta.<L,B>createElementWriter();
            groupLooker = tree.meta.<A,B>createAggregateWriter();
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
