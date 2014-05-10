/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.support.versioning.date;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.support.versioning.Range;
import org.neo4j.support.versioning.util.VersioningProperty;

public class VersionContext {

    public static final long DEFAULT_VERSION = -1L;
    public static final RelationshipType PREV_VERSION_REL_TYPE = DynamicRelationshipType.withName("__PREV_VERSION__");
    private long version;

	public static VersionContext vc(long version) {
		return new VersionContext(version);
	}

	public VersionContext(long version) {
		this.version = version;
	}

	public VersionedNode node(Node node) {
		getPropHolderNode(node);
		return new VersionedNode(node, this);
	}

	public boolean hasValidVersion(PropertyContainer propertyContainer) {
		Range range = VersionContext.getVersion(propertyContainer);
		System.out.println("range: " + range);
		return range != null && range.contains(version);
	}

	private Node getPropHolderNode(Node node) {
		return getPropHolderNodeForVersion(node, version);
	}

	public Object getProperty(Node node, String key) {
		return getPropHolderNode(node).getProperty(key);
	}

	public boolean hasProperty(Node node, String key) {
		return getProperty(node, key, null) != null;
	}

	public Object getProperty(Node node, String key, Object defaultValue) {
		try {
			return getProperty(node, key);
		} catch (NotFoundException e) {
			return defaultValue;
		}
	}

	public Iterable<String> getPropertyKeys(Node node) {
		Node propHolderNode = getPropHolderNode(node);
		return rawGetPropertyKeys(propHolderNode);
	}

	public Iterable<Object> getPropertyValues(Node node) {
		final Node propHolderNode = getPropHolderNode(node);
		return new IterableWrapper<Object, String>(rawGetPropertyKeys(propHolderNode)) {
			@Override
			protected Object underlyingObjectToObject(String object) {
				return propHolderNode.getProperty(object);
			}
		};
	}

	private Iterable<String> rawGetPropertyKeys(Node propHolderNode) {
		return new FilteringIterable<>(propHolderNode.getPropertyKeys(), new Predicate<String>() {
			@Override
			public boolean accept(String item) {
				return !item.equals(VersioningProperty.VALID_FROM_PROPERTY.getName()) &&
                        !item.equals(VersioningProperty.VALID_TO_PROPERTY.getName());
			}
		});
	}

	public void deleteRelationship(Relationship relationship) {
		relationship.setProperty(VersioningProperty.DELETED_PROP_KEY.getName(), version);
	}

	public void deleteNode(Node node) {
		node.setProperty(VersioningProperty.DELETED_PROP_KEY.getName(), version);
	}

	private static Node copyPropsToNewNode(Node node) {
		Node newNode = node.getGraphDatabase().createNode();
		for (String propKey : node.getPropertyKeys()) {
			newNode.setProperty(propKey, node.getProperty(propKey, null));
		}
		return newNode;
	}

	private static void rotatePropertiesRelationships(Node node, Node newNode) {
		Relationship olderPropsRel = node.getSingleRelationship(PREV_VERSION_REL_TYPE, Direction.OUTGOING);
		if (olderPropsRel != null) {
			newNode.createRelationshipTo(olderPropsRel.getOtherNode(node), PREV_VERSION_REL_TYPE);
			olderPropsRel.delete();
		}
		node.createRelationshipTo(newNode, PREV_VERSION_REL_TYPE);
	}

	public static void addVersionedProperty(Node node, String key, Object value) {
		Node newNode = copyPropsToNewNode(node);
		newNode.setProperty(key, value);
		rotatePropertiesRelationships(node, newNode);
	}

	public static Object removeVersionedProperty(Node node, String key) {
		Node newNode = copyPropsToNewNode(node);
		Object result = newNode.removeProperty(key);
		rotatePropertiesRelationships(node, newNode);
		return result;
	}

	public static void setVersion(PropertyContainer propertyContainer, Range range) {
		setStartVersion(propertyContainer, range.from());
		setEndVersion(propertyContainer, range.to());
	}

	public static void setStartVersion(PropertyContainer entity, long startVersion) {
		entity.setProperty(VersioningProperty.VALID_FROM_PROPERTY.getName(), startVersion);
	}

	public static void setEndVersion(PropertyContainer entity, long endVersion) {
		entity.setProperty(VersioningProperty.VALID_TO_PROPERTY.getName(), endVersion);
	}

	public static long getStartVersion(PropertyContainer entity) {
		return (Long) entity.getProperty(VersioningProperty.VALID_FROM_PROPERTY.getName(), DEFAULT_VERSION);
	}

	public static long getEndVersion(PropertyContainer entity) {
		return (Long) entity.getProperty(VersioningProperty.VALID_TO_PROPERTY.getName(), DEFAULT_VERSION);
	}

	public static Range getVersion(PropertyContainer propertyContainer) {
		Object from = getStartVersion(propertyContainer);
		Object to = getEndVersion(propertyContainer);
		if (from == DEFAULT_VERSION || to == DEFAULT_VERSION) {
			return null;
		}
		return new Range((Long) from, (Long) to);
	}

	private static Node getPropHolderNodeForVersion(Node node, long version) {
		Range range = getVersion(node);
		System.out.println("Seeking prop holder for: " + node);
		if (!range.contains(version)) {
			Relationship prevVersionRel = node.getSingleRelationship(PREV_VERSION_REL_TYPE, Direction.OUTGOING);
			if (prevVersionRel == null) {
				throw new NotFoundException("Version [" + version + "] not found.");
			}
			return getPropHolderNodeForVersion(prevVersionRel.getOtherNode(node), version);
		}
		return node;
	}
}
