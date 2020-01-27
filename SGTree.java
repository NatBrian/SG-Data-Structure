package cmsc420.meeshquest.part1;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Nathanael Brian
/* This is an extended-tree version of a scapegoat tree. We follow the basic
* structure of the scapegoat tree. The size of a node is defined to be the
* number of external nodes that are descended from it, and the height is
* defined to be the length of the longest path to any external node.
*
* The algorithm maintains two size values, nItems (n) and maxItems (m).
* Whenever an item is inserted, both quantities are increments. Whenever an
* item is deleted, nItems is decremented.
* 
* The insert function descends the tree and adds a new external node at the
* appropriate location. If the tree's height exceeds
* 
* log_{3/2} maxItems
* 
* we traverse the search path starting at the root until finding a node u such
* that
* 
* size(u.child)/size(u) > 2/3.
* 
* (Such a node is guaranteed to exist.) The subtree at u is completely rebuilt
* into a perfectly balanced form. (If there are an odd number of points, the
* additional point is stored in the left subtree.)
*
* The delete function finds the external node that contains the item to be
* deleted. It deletes this item and decrements nItems. If
*
* nItems < 2*maxItems,
*
* the entire tree is rebuilt.
*
* Generic Elements:
* 
* The tree is parameterized by a type P, called a point, which is assumed to
* implement the interface NamedPoint2D. Such an object supports the functions
* getName(), getX(), and getY().
* 
* The constructor is provided a comparator, for comparing objects of type P.
* For Meeshquest, this compares lexicographically by (x,y) coordinates.
*
* Node Structure:
*
* There are two types of nodes, internal and external. Internal nodes store a
* splitting point. Points that are less than or equal to the splitter
* (according to the comparator) go into the left subtree. External nodes just
* store a single point. For the sake of rebalancing, internal nodes also store
* the height and size of the subtree rooted at this node.
*/

public class SGTree {
	private class Node {
		City key;
		Node left, right, parent;
		int nodeSize, nodeHeight; // nodeSize is number of external nodes in subtree
		String tag;

		private Node(City key) {
			this.key = key;
			left = right = parent = null;
			nodeSize = nodeHeight = 0;
			tag = "";
		}
	}

	Node root;
	int n, m, height;

	/* Constructor */
	public SGTree() {
		root = null;
		n = 0; // number of current nodes in the tree
		m = 0; // upper bound of tree size
		height = 0;
	}

	public void insert(City key) {
		n++;
		m++;
		root = insertRecursive(root, null, key);
		fixParents();
		updateAllNodeAttribute();
		height = getHeight(root) - 1;

		if (height > Math.log(m) / (Math.log(3) - Math.log(2))) {
			Node scapeGoatNode = searchCandidateScapeGoat(root, key);
			Node[] arr = rebuild(scapeGoatNode);
			rebuildExternalNode(arr);
			fixParents();
			updateAllNodeAttribute();
			height = getHeight(root) - 1;
		}
	}

	private Node insertRecursive(Node x, Node parent, City key) {
		if (x == null) {
			x = new Node(key);
			x.tag = "external";
			x.parent = parent;
			return x;
		}
		if (x.tag == "internal") {
			if (key.getX() < x.key.getX() || ((key.getX() == x.key.getX()) && (key.getY() <= x.key.getY()))) {
				x.left = insertRecursive(x.left, x, key);
			} else {
				x.right = insertRecursive(x.right, x, key);
			}
		} else {
			if (key.getX() < x.key.getX() || ((key.getX() == x.key.getX()) && (key.getY() <= x.key.getY()))) {

				Node internalNode = new Node(key);
				internalNode.tag = "internal";
				Node externalNode = new Node(key);
				externalNode.tag = "external";

				if (x.parent != null) {
					if (x.parent.left.key.getName().compareTo(x.key.getName()) == 0) {
						x.parent.left = internalNode;
					} else {
						x.parent.right = internalNode;
					}
				}

				internalNode.left = externalNode;
				internalNode.right = x;

				externalNode.parent = internalNode;
				x.parent = internalNode;

				return internalNode;
			} else {
				Node internalNode = new Node(x.key);
				internalNode.tag = "internal";
				Node externalNode = new Node(key);
				externalNode.tag = "external";

				if (x.parent != null) {
					if (x.parent.left.key.getName().compareTo(x.key.getName()) == 0) {
						x.parent.left = internalNode;
					} else {
						x.parent.right = internalNode;
					}
				}

				internalNode.left = x;
				internalNode.right = externalNode;

				externalNode.parent = internalNode;
				x.parent = internalNode;

				return internalNode;
			}
		}
		return x;
	}

	public void delete(City key) {
		n--;
		if (n == 0) {
			n = m = 0;
			root = null;
		}
		else if (n == 1) {
			root = deleteRecursive(root, key);
			fixParents();
			updateAllNodeAttribute();
			height = getHeight(root) - 1;
			m = n;
		}
		else {
			Node a = search(root, key).parent;
			root = deleteRecursive(root, key);
			if (a != null && a.parent != null)
				root = deleteRecursiveInternal(root, a);
			fixParents();
			updateAllNodeAttribute();
			height = getHeight(root) - 1;
			
			if (2 * n < m) {
				m = n;
				Node[] arr = rebuild(root);
				rebuildExternalNode(arr);
				fixParents();
				updateAllNodeAttribute();
				height = getHeight(root) - 1;
			}
		}
	}

	private Node deleteRecursive(Node root, City key) {
		if (root == null)
			return root;

		if (root.tag.compareTo("internal") == 0 && (key.getX() < root.key.getX()
				|| ((key.getX() == root.key.getX()) && (key.getY() <= root.key.getY()))))
			root.left = deleteRecursive(root.left, key);
		else if (root.tag.compareTo("internal") == 0 && (key.getX() > root.key.getX()
				|| ((key.getX() == root.key.getX()) && (key.getY() > root.key.getY()))))
			root.right = deleteRecursive(root.right, key);
		else {
			if (getHeight(root) == 1) {
				root = null;
			}
			else if (getHeight(root) == 2) {
				if (root.parent.left == root) {
					root = null;
				}
				else {
					root = root.parent.left;
					root.tag = "external";
				}
			}
			else {
				if (root.parent.right == root) {
					if (root.parent.parent.left == root.parent) {
						root.parent.parent.left = root.parent.left;
						root = null;
					} else if (root.parent.parent.right == root.parent) {
						root.parent.parent.right = root.parent.left;
						root = null;
					}
				} else if (root.parent.left == root) {
					if (root.parent.parent.left == root.parent) {
						root.parent.parent.left = root.parent.right;
						root = null;
					} else if (root.parent.parent.right == root.parent) {
						root.parent.parent.right = root.parent.right;
						root = null;
					}
				}
			}
		}
		return root;
	}

	private Node deleteRecursiveInternal(Node root, Node node) {
		if (root == null)
			return root;

		if ((node.key.getX() < root.key.getX()
				|| ((node.key.getX() == root.key.getX()) && (node.key.getY() < root.key.getY()))))
			root.left = deleteRecursiveInternal(root.left, node);
		else if ((node.key.getX() > root.key.getX()
				|| ((node.key.getX() == root.key.getX()) && (node.key.getY() > root.key.getY()))))
			root.right = deleteRecursiveInternal(root.right, node);
		else {
			if (root.parent.right == root) {
				if (root.right == null) {
					return root.left;
				}
				return root.right;
			} else if (root.parent.left == root) {
				if (root.right == null) {
					return root.left;
				}
				return root.right;
			}
		}
		return root;
	}

	public void fixParents() {
		ArrayList<Node> arr = new ArrayList<Node>();
		arr = collectAllNodes(root, arr);
		for (int i = 0; i < arr.size(); i++) {
			fixParentsRec(root, arr.get(i), null);
		}
	}

	private void fixParentsRec(Node root, Node target, Node parent) {
		if (root == null || (root.key.getX() == target.key.getX() && root.key.getY() == target.key.getY()
				&& root.tag.compareTo(target.tag) == 0))
			target.parent = parent;

		else if (target.key.getX() < root.key.getX()
				|| ((target.key.getX() == root.key.getX()) && (target.key.getY() <= root.key.getY())))
			fixParentsRec(root.left, target, root);
		else {
			fixParentsRec(root.right, target, root);
		}
	}

	public Node searchCandidateScapeGoat(Node root, City key) {

		if (key.getX() < root.key.getX() || ((key.getX() == root.key.getX()) && (key.getY() <= root.key.getY()))) {
			if (2 * getSize(root) < 3 * getSize(root.left)) {
				return root;
			} else {
				root = searchCandidateScapeGoat(root.left, key);
			}
		} else {
			if (2 * getSize(root) < 3 * getSize(root.right)) {
				return root;
			} else {
				root = searchCandidateScapeGoat(root.right, key);
			}
		}
		return root;
	}

	// rebuild tree for internal nodes
	private Node[] rebuild(Node u) {
		int k = getSize(u);
		Node p = u.parent;

		Node[] arr = new Node[k];
		putExternalNodeToArray(u, arr, 0);

		if (p == null) {
			root = buildSubTree(arr, 0, k);
			root.parent = null;
		} else if (p.right == u) {
			p.right = buildSubTree(arr, 0, k);
			p.right.parent = p;
		} else {
			p.left = buildSubTree(arr, 0, k);
			p.left.parent = p;
		}

		return arr;
	}

	// inorder traversal on putting external nodes into array
	private int putExternalNodeToArray(Node u, Node[] arr, int i) {
		if (u == null) {
			return i;
		}
		i = putExternalNodeToArray(u.left, arr, i);
		if (u.tag.equals("external"))
			arr[i++] = u;
		return putExternalNodeToArray(u.right, arr, i);
	}

	// building subtree
	private Node buildSubTree(Node[] arr, int i, int k) {
		if (k == 0 || k == 1)
			return null;

		int m = (int) Math.ceil((float) k / 2);

		Node internalNode = arr[i + m - 1];
		internalNode.tag = "internal";

		internalNode.left = buildSubTree(arr, i, m);
		if (internalNode.left != null)
			internalNode.left.parent = internalNode;
		internalNode.right = buildSubTree(arr, i + m, k - m);
		if (internalNode.right != null)
			internalNode.right.parent = internalNode;
		return internalNode;
	}

	// rebuild tree for external nodes
	private void rebuildExternalNode(Node[] arr) {
		for (int i = 0; i < arr.length; i++) {
			insertBST(arr[i].key);
		}
	}

	// insert the external node with BinarySearchTree Insertion Function
	private void insertBST(City key) {
		root = insertBSTRecursive(root, key);
	}

	private Node insertBSTRecursive(Node root, City key) {
		if (root == null) {
			root = new Node(key);
			root.tag = "external";
			return root;
		}

		if (key.getX() < root.key.getX() || ((key.getX() == root.key.getX()) && (key.getY() <= root.key.getY())))
			root.left = insertBSTRecursive(root.left, key);
		else
			root.right = insertBSTRecursive(root.right, key);

		return root;
	}

	public int getHeight(Node root) {
		if (root == null)
			return 0;
		else {
			// compute each subtree
			int lDepth = getHeight(root.left);
			int rDepth = getHeight(root.right);

			// get the max depth
			if (lDepth > rDepth)
				return (lDepth + 1);
			else
				return (rDepth + 1);
		}
	}

	// size of internal node (is number of external node)
	public int getSize(Node root) {
		if (root == null) {
			return 0;
		} else {
			if (root.tag.equals("external")) {
				return 1;
			}
			return getSize(root.left) + getSize(root.right);
		}
	}

	// update all nodes size and height
	public void updateAllNodeAttribute() {
		ArrayList<Node> arr = new ArrayList<Node>();
		arr = collectAllNodes(root, arr);
		for (int i = 0; i < arr.size(); i++) {
			arr.get(i).nodeSize = getSize(arr.get(i));
			arr.get(i).nodeHeight = getHeight(arr.get(i));
		}
	}

	public void preOrderXML(Node node, Document doc, Element rootElt) {
		if (node != null) {
			Element nodeElt = doc.createElement(node.tag);
			nodeElt.setAttribute("name", node.key.getName());
			nodeElt.setAttribute("x", Integer.toString(node.key.getX()));
			nodeElt.setAttribute("y", Integer.toString(node.key.getY()));
			preOrderXML(node.left, doc, nodeElt);
			preOrderXML(node.right, doc, nodeElt);
			rootElt.appendChild(nodeElt);
		}
	}

	public void inOrderXML(Node node, Document doc, Element rootElt) {
		if (node != null) {
			inOrderXML(node.left, doc, rootElt);
			if (node.tag.equals("external")) {
				Element nodeElt = doc.createElement("city");
				nodeElt.setAttribute("name", node.key.getName());
				nodeElt.setAttribute("x", Integer.toString(node.key.getX()));
				nodeElt.setAttribute("y", Integer.toString(node.key.getY()));
				nodeElt.setAttribute("color", node.key.getColor());
				nodeElt.setAttribute("radius", Integer.toString(node.key.getR()));
				rootElt.appendChild(nodeElt);
			}
			inOrderXML(node.right, doc, rootElt);
		}
	}

	public ArrayList<Node> collectAllNodes(Node node, ArrayList<Node> arr) {
		if (node != null) {
			arr.add(node);
			collectAllNodes(node.left, arr);
			collectAllNodes(node.right, arr);
		}
		return arr;
	}

	public void traverseInOrder(Node node) {
		if (node != null) {
			traverseInOrder(node.left);
			System.out.print(" " + node.key.getName());
			traverseInOrder(node.right);
		}
	}

	public void traversePreOrder(Node node) {
		if (node != null) {
			System.out.print(" " + node.key.getName() + "-" + node.tag + "-" + Integer.toString(node.nodeSize) + "-"
					+ Integer.toString(node.nodeHeight));
			traversePreOrder(node.left);
			traversePreOrder(node.right);
		}
	}

	public void traverseInOrder() {
		traverseInOrder(root);
	}

	public void traversePreOrder() {
		traversePreOrder(root);
	}

	public Node search(Node root, City key) {
		if (root == null
				|| (root.tag.equals("external") && root.key.getX() == key.getX() && root.key.getY() == key.getY()))
			return root;
		if (key.getX() < root.key.getX() || ((key.getX() == root.key.getX()) && (key.getY() <= root.key.getY())))
			return search(root.left, key);
		return search(root.right, key);
	}

	// size of internal nodes + external nodes
	public int totalNodes(Node node) {
		if (node == null)
			return 0;
		else
			return (totalNodes(node.left) + 1 + totalNodes(node.right));
	}

	// https://rosettacode.org/wiki/Visualize_a_tree
	// display visualization of tree
	public void display() {
		final int height = 5, width = 200;

		int len = width * height * 2 + 2;
		StringBuilder sb = new StringBuilder(len);
		for (int i = 1; i <= len; i++)
			sb.append(i < len - 2 && i % width == 0 ? "\n" : ' ');

		displayR(sb, width / 2, 1, width / 4, width, root, " ");
		System.out.println(sb);
	}

	private void displayR(StringBuilder sb, int c, int r, int d, int w, Node n, String edge) {
		if (n != null) {
			displayR(sb, c - d, r + 2, d / 2, w, n.left, " /");

			String s = String.valueOf(n.key.getName() + n.tag);
			int idx1 = r * w + c - (s.length() + 1) / 2;
			int idx2 = idx1 + s.length();
			int idx3 = idx1 - w;
			if (idx2 < sb.length())
				sb.replace(idx1, idx2, s).replace(idx3, idx3 + 2, edge);

			displayR(sb, c + d, r + 2, d / 2, w, n.right, "\\ ");
		}
	}

//	public static void main(String argv[]) {
////      SGTree tree = new SGTree(); 
////      tree.insert(new City("Edinburgh",100,500,0,""));
////      tree.insert(new City("Lisbon",100,100,0,"")); 
////      tree.insert(new City("Varna",100,800,0,"")); 
////      tree.insert(new City("London",150,250,0,""));  
////      tree.insert(new City("Prague",150,700,0,""));
////      tree.insert(new City("Madrid",200,100,0,""));
////      tree.insert(new City("Paris",250,300,0,""));
////      tree.insert(new City("Copenhagen",250,500,0,""));
////      tree.insert(new City("Marseilles",350,100,0,""));
////      tree.insert(new City("Vienna",350,700,0,"orange"));
////      tree.insert(new City("Amsterdam" ,400,300,0,"yellow"));
////	  	tree.insert(new City("Munich" ,400,200,0,"green"));
////	  	tree.insert(new City("Milan" ,500,50,0,"blue"));
////	  	tree.insert(new City("Rome" ,500,450,0,"purple"));
////	  	tree.insert(new City("Naples" ,450,650,0,"black"));
////	  	tree.insert(new City("Geneva" ,300,200,0,"orange"));
////	  	tree.insert(new City("Geneva_Duplicate",300 ,200,1,"black"));
//
////      tree.insert(new City("x13",13,0,0,"black"));
////      System.out.println(tree.search(tree.root, new City("x15",15,0,0,"black")));
////      tree.insert(new City("x15",15,0,0,"black"));
////      tree.insert(new City("x17",17,0,0,"black"));
////      tree.insert(new City("x12",12,0,0,"black"));
////      tree.insert(new City("x20",20,0,0,"black"));
////      tree.insert(new City("x09",9,0,0,"black"));
////      tree.insert(new City("x02",2,0,0,"black"));
////      tree.insert(new City("x07",7,0,0,"black"));
////      tree.insert(new City("x01",1,0,0,"black"));
////      tree.insert(new City("x14",14,0,0,"black"));
////      tree.insert(new City("x16",16,0,0,"black"));
////      tree.insert(new City("x00",0,0,0,"black"));
////      tree.insert(new City("x04",4,0,0,"black"));
////      tree.insert(new City("x05",5,0,0,"black"));
//
////      tree.insert(new City("13",13,0,0,"black"));
////      tree.insert(new City("15",15,0,0,"black"));
////      tree.insert(new City("17",17,0,0,"black"));
////      tree.insert(new City("12",12,0,0,"black"));
////      tree.insert(new City("20",20,0,0,"black"));
////      tree.insert(new City("9",9,0,0,"black"));
////      tree.insert(new City("2",2,0,0,"black"));
////      tree.insert(new City("7",7,0,0,"black"));
////      tree.insert(new City("1",1,0,0,"black"));
////      tree.insert(new City("14",14,0,0,"black"));
////      tree.insert(new City("16",16,0,0,"black"));
////      tree.insert(new City("0",0,0,0,"black"));
////      tree.insert(new City("4",4,0,0,"black"));
////      tree.insert(new City("5",5,0,0,"black"));
//
////      tree.display();
////      System.out.printf("total nodes %d\n", tree.totalNodes(tree.root));
////      System.out.println("Preorder traversal of binary tree is "); 
////      tree.traversePreOrder(); 
////      System.out.println("");
////      
////      
////      tree.delete(new City("London",150,250,0,""));
////      System.out.printf("total nodes %d\n", tree.totalNodes(tree.root));
////      System.out.println("Preorder traversal of binary tree is "); 
////      tree.traversePreOrder(); 
//
////      ArrayList<Node> arr = new ArrayList<Node>();
////      arr = tree.collectAllNodes(tree.root, arr);
////      System.out.println("");
////      System.out.println(tree.root.tag + tree.root.key.getName());
////      System.out.println("");
////      System.out.printf("%d \n", tree.totalNodes(tree.root));
////      ArrayList<String> arrString = new ArrayList<String>();
////      for (int i = 0; i < arr.size(); i++) {
////    	  arrString.add(arr.get(i).key.getName() + "-" + arr.get(i).tag);
////      }
////      System.out.println("");
////      ArrayList<String> arrString = new ArrayList<String>();
////      for (int i = 0; i < tree.totalNodes(tree.root); i++) {
////    	  System.out.println(arr.get(i).key.getName() + arr.get(i).tag);
////    	  if (arr.get(i).parent != null) {
////    		  System.out.println("--->" + arr.get(i).parent.key.getName() + arr.get(i).parent.tag);
////    	  }
////    	  else {
////    		  System.out.println("---> no parent"); 
////    	  }
////      }
////      
////      System.out.println(Arrays.toString(arrString.toArray()));
//	     
//	    FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter("D:/Documents/University of Maryland - CP/Maryland Fall 2019/CMSC420/MeeshQuest-Skeleton/textinput.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    PrintWriter printWriter = new PrintWriter(fileWriter);
//	    printWriter.println("<commands\r\n" + "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
//				+ "	xsi:noNamespaceSchemaLocation=\"part1in.xsd\"\r\n"
//				+ "	spatialWidth=\"1024\" spatialHeight=\"1024\">");
//		for (int i = 0; i < 10000; i++) {
//			String x = Integer.toString((int) ((Math.random() * ((100 - 0) + 1)) + 0));
//			String y = Integer.toString((int) ((Math.random() * ((100 - 0) + 1)) + 0));
//			String s = "s" + (x) + (y);
//			printWriter.println(
//					"<createCity name=\"" + s + "\" x=\"" + x + "\" y=\"" + y + "\" radius=\"0\" color=\"black\"/>");
//			printWriter.println("<deleteCity name=\"" + s + "\"/>");
//			printWriter.println("<deleteCity name=\"" + s + "\"/>");
//			printWriter.println(
//					"<createCity name=\"" + s + "\" x=\"" + x + "\" y=\"" + y + "\" radius=\"0\" color=\"black\"/>");
//			printWriter.println("<printSGTree/>");
//		}
//		printWriter.println("</commands>");
//	    printWriter.close();
//	}
}
