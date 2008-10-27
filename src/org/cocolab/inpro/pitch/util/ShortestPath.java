package org.cocolab.inpro.pitch.util;

import java.util.*;

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
	
	public LinkedList<NodeType> calculate () {
					
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			if (node == targetNode) {
				LinkedList<NodeType> result = new LinkedList<NodeType>();
				while (node != null) {
					result.addFirst(node.payload);
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
	
	private class PriorityQueue<T> extends LinkedList<T> {
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
	
	public static void main(String[] args) {
		Integer[] nodes = new Integer[5];
		for (int i = 0; i < 5; i++) {
			nodes[i] = new Integer(i);
		}
		ShortestPath<Integer> sp = new ShortestPath<Integer>();
		sp.connect(nodes[0], nodes[1], 2);
		sp.connect(nodes[0], nodes[2], 4);
		sp.connect(nodes[0], nodes[3], 7);
		sp.connect(nodes[1], nodes[3], 3);
		sp.connect(nodes[2], nodes[3], 2);
		sp.connect(nodes[2], nodes[4], 3);
		sp.connect(nodes[3], nodes[4], 1);
		sp.setStart(nodes[0]);
		sp.setTarget(nodes[4]);
		List<Integer> result = sp.calculate();
		
		for (Integer i : result) {
			System.out.println(i);
		}
		
	}
	
}

