/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.objects.shared;

import java.util.ArrayList;
import java.util.List;

import org.truffleruby.language.RubyContextNode;
import org.truffleruby.language.RubyDynamicObject;
import org.truffleruby.language.objects.ObjectGraph;
import org.truffleruby.language.objects.ShapeCachingGuards;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.ObjectLocation;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

/** Share the object and all that is reachable from it (see {@link ObjectGraph#getAdjacentObjects}) */
@ImportStatic(ShapeCachingGuards.class)
public abstract class ShareObjectNode extends RubyContextNode {

    protected static final int CACHE_LIMIT = 8;

    protected final int depth;

    public ShareObjectNode(int depth) {
        this.depth = depth;
    }

    public abstract void executeShare(RubyDynamicObject object);

    @Specialization(
            guards = "object.getShape() == cachedShape",
            assumptions = { "cachedShape.getValidAssumption()", "sharedShape.getValidAssumption()" },
            limit = "CACHE_LIMIT")
    @ExplodeLoop
    protected void shareCached(RubyDynamicObject object,
            @Cached("object.getShape()") Shape cachedShape,
            @CachedLibrary(limit = "1") DynamicObjectLibrary objectLibrary,
            @Cached("createShareInternalFieldsNode()") ShareInternalFieldsNode shareInternalFieldsNode,
            @Cached("createReadAndShareFieldNodes(getObjectProperties(cachedShape))") ReadAndShareFieldNode[] readAndShareFieldNodes,
            @Cached("createSharedShape(cachedShape)") Shape sharedShape) {
        // Mark the object as shared first to avoid recursion
        assert object.getShape() == cachedShape;
        objectLibrary.markShared(object);
        assert object.getShape() == sharedShape;

        // Share the logical class
        if (!object.getLogicalClass().getShape().isShared()) {
            // The logical class is fixed for a given Shape and only needs to be shared once
            CompilerDirectives.transferToInterpreterAndInvalidate();
            SharedObjects.writeBarrier(getContext(), object.getLogicalClass());
        }

        // Share the metaclass. Note that the metaclass might refer to `object` via `attached`,
        // so it is important to share the object first.
        if (!object.getMetaClass().getShape().isShared()) {
            // The metaclass is fixed for a given Shape and only needs to be shared once
            CompilerDirectives.transferToInterpreterAndInvalidate();
            SharedObjects.writeBarrier(getContext(), object.getMetaClass());
        }

        shareInternalFieldsNode.executeShare(object);

        for (ReadAndShareFieldNode readAndShareFieldNode : readAndShareFieldNodes) {
            readAndShareFieldNode.executeReadFieldAndShare(object);
        }

        assert allFieldsAreShared(object);
    }

    private boolean allFieldsAreShared(RubyDynamicObject object) {
        for (Object value : ObjectGraph.getAdjacentObjects(object)) {
            assert SharedObjects.isShared(value) : "unshared field in shared object: " + value;
        }

        return true;
    }

    @Specialization(guards = "updateShape(object)")
    protected void updateShapeAndShare(RubyDynamicObject object) {
        executeShare(object);
    }

    @Specialization(replaces = { "shareCached", "updateShapeAndShare" })
    protected void shareUncached(RubyDynamicObject object) {
        SharedObjects.writeBarrier(getContext(), object);
    }

    protected static List<Property> getObjectProperties(Shape shape) {
        final List<Property> objectProperties = new ArrayList<>();
        // User properties only, ShareInternalFieldsNode do the rest
        for (Property property : shape.getProperties()) {
            if (property.getLocation() instanceof ObjectLocation) {
                objectProperties.add(property);
            }
        }
        return objectProperties;
    }

    protected ShareInternalFieldsNode createShareInternalFieldsNode() {
        return ShareInternalFieldsNodeGen.create(depth);
    }

    protected ReadAndShareFieldNode[] createReadAndShareFieldNodes(List<Property> properties) {
        ReadAndShareFieldNode[] nodes = properties.size() == 0
                ? ReadAndShareFieldNode.EMPTY_ARRAY
                : new ReadAndShareFieldNode[properties.size()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = ReadAndShareFieldNodeGen.create(properties.get(i), depth);
        }
        return nodes;
    }

    protected Shape createSharedShape(Shape cachedShape) {
        if (cachedShape.isShared()) {
            throw new UnsupportedOperationException(
                    "Thread-safety bug: the object is already shared. This means another thread marked the object as shared concurrently.");
        } else {
            return cachedShape.makeSharedShape();
        }
    }

}
