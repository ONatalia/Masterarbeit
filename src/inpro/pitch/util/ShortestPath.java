/* 
 * Copyright 2007, 2008, Gabriel Skantze and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

package inpro.pitch.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShortestPath<NodeType> {

	private HashMap<NodeType,Node> nodes = new HashMap<NodeType,Node>();
	private HashMap<Node,ArrayList<Connection>> connections = new HashMap<Node,ArrayList<Connection>>();
	private Node startNode = null;
	private Node targetNode = null;
	private PriorityQueue<Node> queue = new PriorityQueue<Node>();
	
	public void clear() {
		nodes.clear();
		connections.clear();
		startNode = null;
		targetNode = null;
		queue.clear();
	}
	
	public List<NodeType> calculate () {
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			if (node == targetNode) {
				List<NodeType> result = new ArrayList<NodeType>();
				while (node != null) {
					result.add(0, node.payload);
					node = node.previous;
				}
				return result;
			}
			for (Connection connection : connections.get(node)) {
				double distance = node.distance + connection.cost;
				if (distance < connection.node.distance) {
					connection.node.distance = distance;
					connection.node.previous = node;
				}
			}
		}
		return null;
	}
	
	public void addNode(NodeType node) {
		if (!nodes.containsKey(node)) nodes.put(node, new Node(node));
	}

	public void setStart(NodeType start) {
		addNode(start);
		startNode = nodes.get(start);
		startNode.distance = 0;
	}

	public void setTarget(NodeType target) {
		addNode(target);
		targetNode = nodes.get(target);	
	}

	public void connect(NodeType from, NodeType to, double cost) {
		addNode(from);
		addNode(to);
		Node fromNode = nodes.get(from);
		Node toNode = nodes.get(to);
		if (!connections.containsKey(fromNode)) connections.put(fromNode, new ArrayList<Connection>());
		connections.get(fromNode).add(new Connection(toNode, cost));
		
		if (!queue.contains(fromNode)) 
			queue.add(fromNode);
		if (!queue.contains(toNode)) 
			queue.add(toNode);
	}
	
	private class Connection {
		public Node node;
		public double cost;
		
		public Connection (Node node, double cost) {
			this.node = node;
			this.cost = cost;
		}
	}
	
	private class Node implements Comparable<Node> {
		public Node previous = null;
		public double distance = Double.MAX_VALUE;
		public NodeType payload = null;
		
		public Node (NodeType payload) {
			this.payload = payload;
		}

		public int compareTo(Node node) {
			if (distance < node.distance) 
				return -1;
			else if (distance > node.distance) 
				return 1;
			else 
				return 0;
		}
	}
	
	private class PriorityQueue<T> extends ArrayList<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5163827055251838392L;

		@SuppressWarnings("unchecked")
		public T poll() {
			if (isEmpty()) return null;
			int min = 0;
			for (int i = 1; i < size(); i++) {
				if (((Comparable<T>)get(i)).compareTo(get(min)) < 0) {
					min = i;
				}
			}
			T result = get(min);
			remove(min);
			return result;
		}
	}
}


