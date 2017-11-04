package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

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

        B array;
        int elements = 0;
        abstract void build(Tree<V,R,W,G,M,B,L,A>.BoundingBuilder builder);
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
            V meta = builder.tree().meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            L looker = builder.valueLooker;
            looker.wrap(array, builder.tree().blocksize, 0);

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
            V meta = builder.tree().meta;
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
        final Node<V,R,W,G,M,B,L,A,?> root;
        final G bounds;

        Segment(Node<V, R, W, G, M, B, L, A, ?> root, G bounds) {
            this.root = root;
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


    static class Piece<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>
            > {
        G bounds;
        final Node<V,R,W,G,M,B,L,A,?> root;
        double offset;
        Piece(Node<V,R,W,G,M,B,L,A,?> root, double offset, double distance, Tree<V,R,W,G,M,B,L,A> tree) {
            this.root = root;
            this.offset = offset;

            Tree<V,R,W,G,M,B,L,A>.BoundingBuilder boundingBuilder = tree.new BoundingBuilder(offset, distance);
            root.build(boundingBuilder);
            bounds = boundingBuilder.result.group();
        }
    }
    class Undo<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>
            > {
        int wrap;
        int removed; // first n pieces are removed, rest added
        Piece<V,R,W,G,M,B,L,A>[] pieces;

        final void apply(Tree<V,R,W,G,M,B,L,A> tree) {
            ListIterator<Piece<V,R,W,G,M,B,L,A>> iterator = tree.pieces.listIterator(wrap);
            int i = 0;
            for (; i < removed; i++) iterator.remove();
            int length = pieces.length;
            for (; i < length; i++) iterator.add(pieces[i]);
        }

        final void unapply(Tree<V,R,W,G,M,B,L,A> tree) {
            ListIterator<Piece<V,R,W,G,M,B,L,A>> iterator = tree.pieces.listIterator(wrap);
            int i = removed;
            int length = pieces.length;
            for (; i < length; i++) iterator.remove();

            for (i = 0; i < removed; i++) iterator.add(pieces[i]);
        }
    }

    public class BoundingBuilder {
        final M result;
        final L valueLooker;
        final A groupLooker;

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            result = meta.createMutableBounds();
            valueLooker = meta.<L,B>createElementWriter();
            groupLooker = meta.<A,B>createAggregateWriter();
        }

        public Tree<V,R,W,G,M,B,L,A> tree() {
            return Tree.this;
        }
    }


    private final LinkedList<Piece<V,R,W,G,M,B,L,A>> pieces = new LinkedList<>();
    private final LinkedList<Undo> undos = new LinkedList<>();
    public void append(Iterable<R> points){
        replace(Double.MAX_VALUE, 0, points);
    }
    public void insert(double wrap, Iterable<R> points){
        replace(wrap, 0, points);
    }
    public void delete(double wrap, double distance){
        replace(wrap, distance, Collections.<R>emptyList());
    }
    public void replace(double wrap, double distance, Iterable<R> points){

    }

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
