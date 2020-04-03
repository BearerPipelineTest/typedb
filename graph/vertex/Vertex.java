/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package hypergraph.graph.vertex;

import hypergraph.graph.Schema;
import hypergraph.graph.edge.Edge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class Vertex<
        VERTEX_SCHEMA extends Schema.Vertex,
        VERTEX extends Vertex,
        EDGE_SCHEMA extends Schema.Edge,
        EDGE extends Edge<EDGE_SCHEMA, VERTEX>> {

    protected final VERTEX_SCHEMA schema;

    protected final DirectedEdges<VERTEX, EDGE_SCHEMA, EDGE> outs;
    protected final DirectedEdges<VERTEX, EDGE_SCHEMA, EDGE> ins;

    protected byte[] iid;

    Vertex(byte[] iid, VERTEX_SCHEMA schema) {
        this.schema = schema;
        this.iid = iid;
        outs = newDirectedEdges(DirectedEdges.Direction.OUT);
        ins = newDirectedEdges(DirectedEdges.Direction.IN);
    }

    protected abstract DirectedEdges<VERTEX, EDGE_SCHEMA, EDGE> newDirectedEdges(DirectedEdges.Direction direction);

    public abstract Schema.Status status();

    public VERTEX_SCHEMA schema() {
        return schema;
    }

    public abstract void commit();

    public DirectedEdges<VERTEX, EDGE_SCHEMA, EDGE> outs() {
        return outs;
    }

    public DirectedEdges<VERTEX, EDGE_SCHEMA, EDGE> ins() {
        return ins;
    }

    public byte[] iid() {
        return iid;
    }

    public void iid(byte[] iid) {
        this.iid = iid;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": [" + schema + "] " + Arrays.toString(iid);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Vertex that = (Vertex) object;
        return Arrays.equals(this.iid, that.iid);
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(iid);
    }

    public abstract static class DirectedEdges<
            DIR_VERTEX extends Vertex,
            DIR_EDGE_SCHEMA extends Schema.Edge,
            DIR_EDGE extends Edge<DIR_EDGE_SCHEMA, DIR_VERTEX>> {

        protected final Map<DIR_EDGE_SCHEMA, Set<DIR_EDGE>> edges;
        protected final Direction direction;

        enum Direction {
            OUT(true),
            IN(false);

            private final boolean isOut;

            Direction(boolean isOut) {
                this.isOut = isOut;
            }

            public boolean isOut() {
                return isOut;
            }

            public boolean isIn() {
                return !isOut;
            }
        }

        DirectedEdges(Direction direction) {
            this.direction = direction;
            edges = new ConcurrentHashMap<>();
        }

        public abstract Iterator<DIR_VERTEX> get(DIR_EDGE_SCHEMA schema);

        public abstract void put(DIR_EDGE_SCHEMA schema, DIR_VERTEX to);

        public abstract void remove(DIR_EDGE_SCHEMA schema, DIR_VERTEX to);

        public abstract void remove(DIR_EDGE_SCHEMA schema);

        public abstract void removeNonRecursive(DIR_EDGE edge);

        public void addNonRecursive(DIR_EDGE edge) {
            edges.computeIfAbsent(edge.schema(), e -> ConcurrentHashMap.newKeySet()).add(edge);
        }

        protected void forEach(Consumer<DIR_EDGE> function) {
            edges.forEach((key, set) -> set.forEach(function));
        }
    }
}
