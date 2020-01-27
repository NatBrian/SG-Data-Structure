package cmsc420.meeshquest.part2;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Nathanael Brian
// SG KD-Tree implementation

/*
 * Motivation:
 * Geometric structures like kd-trees are not as easy to rebalance as 
 * are binary search trees because the the ubiquitous rotation operation cannot
 * be applied to multidimensional partition trees, like the kd-tree. 
 * The alternative is to rebuild subtrees.
 * 
 * In geometric data structures, it is natural to employ an extended tree structure, where points
 * are stored in the external nodes, and internal nodes store splitters, which partition space.
 * 
 * Thus, in order to design a dynamic data structure for geometric point sets, we will combine
 * subtree rebuilding with an extended tree structure. This is exactly what the SG kd-tree is.
 * 
 * Overview:
 * The SG kd-tree has features in common to both the extended version of the scapegoat
 * tree (our SG tree) and kd-trees, Here are the elements that it shares in common with the SG
 * tree:
 * 
 * - It is an extended tree, in which contents are stored only in the external nodes and splitters
 * stored in internal nodes.
 * - It is rebalanced through the process of rebuilding subtrees. The same rules as the SG
 * tree apply for when to trigger rebuilding, determining scapegoat nodes, and deciding
 * how to partition objects among the left and right subtrees.
 * - If a point is equal to the splitter, it is stored in the left subtree. (Note that this differs
 * from the kd-tree convention from class.)
 * 
 * Each splitter is based on the geometric structure of the points within the subtree:
 * - Each internal node stores a splitting dimension (either 0 for x or 1 for y), defined to
 * be the dimension corresponding to the longer side of the smallest axis-parallel rectangle
 * that contains the points of the subtree, with ties broken in favor of x over y.
 * - Points are partitioned among the left and right subtrees based on a lexicographical
 * ordering. If the splitting dimension is x, the points are sorted lexicographically by (x, y),
 * and if the splitting dimension is y, the points are sorted lexicographically by (y, x).
 * http://www.cs.umd.edu/class/fall2019/cmsc420-0201/project.html 
 */

public class SGKDTree<P extends NamedPoint2D> {
	private final boolean DEBUG = false; // produce extra debugging output
	private final int BALANCE_NUM = 2; // numerator in balance ratio
	private final int BALANCE_DENOM = 3; // denominator in balance ratio

	private abstract class Node { // generic node type
		final boolean isExternal; // is node external?

		Node(boolean isExternal) { // constructor
			this.isExternal = isExternal;
		}

		abstract P find(P pt); // find point in subtree

		abstract Node insert(P pt) throws Exception; // insert point into subtree

		abstract Node delete(P pt) throws Exception; // delete point from subtree

		abstract Node rebalance(P pt); // find scapegoat and rebalance tree

		abstract void entryList(List<P> list); // return list of entries in subtree

		abstract void print(Element result); // print subtree to result

		abstract String debugPrint(String prefix); // print for debugging

		abstract boolean check(P low, P high); // integrity check

		abstract public String toString(); // for debugging

		abstract public P nearestNeighbor(P q, Node p, Rectangle cell, float bestDist, P bestPoint) throws Exception; // nearest
																														// neighbor
		// to
		// point q
	}

	// -----------------------------------------------------------------
	// Internal node
	// -----------------------------------------------------------------

	private class InternalNode extends Node {

		final P splitter; // point object used for splitting
		int size; // node size (number of external descendants)
		int height; // node height (max number of edges to external)
		Node left; // children
		Node right;
		int cutDim;

		InternalNode(P splitter, Node left, Node right) {
			super(false);
			this.splitter = splitter;
			this.left = left;
			this.right = right;

			if (high.getX() - low.getX() >= high.getY() - low.getY()) {
				cutDim = 0; // split along x-coordinate (vertically)
			} else {
				cutDim = 1; // split along y-coordinate (horizontally)
			}

			updateSizeAndHeight();
		}

		P find(P pt) {
			if (cutDim == 0) { // x-splitter
				if (compareXY.compare(pt, splitter) <= 0) { // (px, py) <= (sx, sy)
					return left.find(pt);
				} else {
					return right.find(pt);
				}
			} else { // y-splitter
				if (compareYX.compare(pt, splitter) <= 0) { // (py, px) <= (sy, sx)
					return left.find(pt);
				} else {
					return right.find(pt);
				}
			}
		}

		// cutDim is the minimum bounding box that includes pt and splitter
		Node insert(P pt) throws Exception {
			// rect is minimumRectangle listPoint2d pt splitter
			if (cutDim == 0) { // x-splitter
				if (compareXY.compare(pt, splitter) <= 0) { // pt is less or equal
					List<Point2D> listPoint2D = new ArrayList<Point2D>();
					listPoint2D.add(pt.getPoint2D());
					listPoint2D.add(splitter.getPoint2D());
					rect = rect.minimumRectangle(listPoint2D);

					if (DEBUG)
						System.out.println("pt " + pt.toString() + "splitter " + splitter.toString() + "Rectangle low "
								+ rect.getLow().toString() + " high " + rect.getHigh().toString());

					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
						if (DEBUG) {
							System.out.println(
									"splitter " + splitter.toString() + " pt " + pt.toString() + " go left" + " x");
							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
						}
					} else {
						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go left"
								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
					}

					low = rect.getLow();
					high = rect.getHigh();

					if (DEBUG) {
						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
					}

					left = left.insert(pt);
					updateSizeAndHeight(); // update this node's information
				} else { // pt is larger
					List<Point2D> listPoint2D = new ArrayList<Point2D>();
					listPoint2D.add(pt.getPoint2D());
					listPoint2D.add(splitter.getPoint2D());
					rect = rect.minimumRectangle(listPoint2D);

					if (DEBUG)
						System.out.println("pt " + pt.toString() + "splitter " + splitter.toString() + "Rectangle low "
								+ rect.getLow().toString() + " high " + rect.getHigh().toString());

					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
						if (DEBUG) {
							System.out.println(
									"splitter " + splitter.toString() + " pt " + pt.toString() + " go right" + " x");
							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
						}
					} else {
						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go right"
								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
					}

					low = rect.getLow();
					high = rect.getHigh();

					if (DEBUG) {
						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
					}

					right = right.insert(pt);
					updateSizeAndHeight(); // update this node's information
				}
			} else { // y-splitter
				if (compareYX.compare(pt, splitter) <= 0) { // pt is less or equal
					List<Point2D> listPoint2D = new ArrayList<Point2D>();
					listPoint2D.add(pt.getPoint2D());
					listPoint2D.add(splitter.getPoint2D());
					rect = rect.minimumRectangle(listPoint2D);

					if (DEBUG)
						System.out.println("pt " + pt.toString() + "splitter " + splitter.toString() + "Rectangle low "
								+ rect.getLow().toString() + " high " + rect.getHigh().toString());

					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
						if (DEBUG) {
							System.out.println(
									"splitter " + splitter.toString() + " pt " + pt.toString() + " go left" + " x");
							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
						}
					} else {
						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go left"
								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
					}

					low = rect.getLow();
					high = rect.getHigh();

					if (DEBUG) {
						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
					}

					left = left.insert(pt);
					updateSizeAndHeight(); // update this node's information
				} else { // pt is larger
					List<Point2D> listPoint2D = new ArrayList<Point2D>();
					listPoint2D.add(pt.getPoint2D());
					listPoint2D.add(splitter.getPoint2D());
					rect = rect.minimumRectangle(listPoint2D);

					if (DEBUG)
						System.out.println("pt " + pt.toString() + "splitter " + splitter.toString() + "Rectangle low "
								+ rect.getLow().toString() + " high " + rect.getHigh().toString());

					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
						if (DEBUG) {
							System.out.println(
									"splitter " + splitter.toString() + " pt " + pt.toString() + " go right" + " x");
							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
						}
					} else {
						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go right"
								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
					}

					low = rect.getLow();
					high = rect.getHigh();

					if (DEBUG) {
						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
					}

					right = right.insert(pt);
					updateSizeAndHeight(); // update this node's information
				}
			}

			// rect is leftPart rightPart approach
//			if (cutDim == 0) { // x-splitter
//				if (compareXY.compare(pt, splitter) <= 0) { // pt is less or equal
//					rect = rect.leftPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
//
//					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
//							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
//						if (DEBUG) {
//							System.out.println(
//									"splitter " + splitter.toString() + " pt " + pt.toString() + " go left" + " x");
//							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
//									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
//						}
//					} else {
//						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go left"
//								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
//								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
//					}
//
//					low = rect.getLow();
//					high = rect.getHigh();
//
//					if (DEBUG) {
//						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
//								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
//					}
//
//					left = left.insert(pt);
//					updateSizeAndHeight(); // update this node's information
//				} else { // pt is larger
//					rect = rect.rightPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
//
//					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
//							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
//						if (DEBUG) {
//							System.out.println(
//									"splitter " + splitter.toString() + " pt " + pt.toString() + " go right" + " x");
//							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
//									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
//						}
//					} else {
//						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go right"
//								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
//								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
//					}
//
//					low = rect.getLow();
//					high = rect.getHigh();
//
//					if (DEBUG) {
//						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
//								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
//					}
//
//					right = right.insert(pt);
//					updateSizeAndHeight(); // update this node's information
//				}
//			} else { // y-splitter
//				if (compareYX.compare(pt, splitter) <= 0) { // pt is less or equal
//					rect = rect.leftPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
//
//					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
//							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
//						if (DEBUG) {
//							System.out.println(
//									"splitter " + splitter.toString() + " pt " + pt.toString() + " go left" + " x");
//							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
//									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
//						}
//					} else {
//						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go left"
//								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
//								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
//					}
//
//					low = rect.getLow();
//					high = rect.getHigh();
//
//					if (DEBUG) {
//						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
//								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
//					}
//
//					left = left.insert(pt);
//					updateSizeAndHeight(); // update this node's information
//				} else { // pt is larger
//					rect = rect.rightPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
//
//					if (rect.contains(new Point2D(splitter.getX(), splitter.getY()))
//							&& rect.contains(new Point2D(pt.getX(), pt.getY()))) {
//						if (DEBUG) {
//							System.out.println(
//									"splitter " + splitter.toString() + " pt " + pt.toString() + " go right" + " x");
//							System.out.println("contain splitter " + splitter.toString() + " pt " + pt.toString()
//									+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString());
//						}
//					} else {
//						throw new Exception(("splitter " + splitter.toString() + " pt " + pt.toString() + " go right"
//								+ " x\n" + "NOT contain splitter " + splitter.toString() + " pt " + pt.toString()
//								+ " low " + rect.getLow().toString() + " high " + rect.getHigh().toString()));
//					}
//
//					low = rect.getLow();
//					high = rect.getHigh();
//
//					if (DEBUG) {
//						System.out.printf("inserting internal %s low: %d %d high: %d %d \n", pt.toString(),
//								(int) low.getX(), (int) low.getY(), (int) high.getX(), (int) high.getY());
//					}
//
//					right = right.insert(pt);
//					updateSizeAndHeight(); // update this node's information
//				}
//			}
			return this;
		}

		Node rebalance(P pt) {
			if (cutDim == 0) { // x-splitter
				if (compareXY.compare(pt, splitter) <= 0) { // pt is less or equal

					rect = rect.leftPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
					low = rect.getLow();
					high = rect.getHigh();

					if (2 * getSize(this) < 3 * getSize(left)) { // too unbalanced?
						return rebuild(this); // this is the scapegoat
					} else { // balance is okay
						left = left.rebalance(pt); // continue the search
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				} else { // pt is larger

					rect = rect.rightPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
					low = rect.getLow();
					high = rect.getHigh();

					if (2 * getSize(this) < 3 * getSize(right)) { // too unbalanced?
						return rebuild(this); // this is the scapegoat
					} else { // balance is okay
						right = right.rebalance(pt); // continue the search
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				}
			} else { // y-splitter
				if (compareYX.compare(pt, splitter) <= 0) { // pt is less or equal

					rect = rect.leftPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
					low = rect.getLow();
					high = rect.getHigh();

					if (2 * getSize(this) < 3 * getSize(left)) { // too unbalanced?
						return rebuild(this); // this is the scapegoat
					} else { // balance is okay
						left = left.rebalance(pt); // continue the search
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				} else { // pt is larger

					rect = rect.rightPart(cutDim, new Point2D(splitter.getX(), splitter.getY()));
					low = rect.getLow();
					high = rect.getHigh();

					if (2 * getSize(this) < 3 * getSize(right)) { // too unbalanced?
						return rebuild(this); // this is the scapegoat
					} else { // balance is okay
						right = right.rebalance(pt); // continue the search
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				}
			}
		}

		Node delete(P pt) throws Exception {
			if (cutDim == 0) { // x-splitter
				if (compareXY.compare(pt, splitter) <= 0) { // delete from left
					left = left.delete(pt);
					if (left == null) {
						return right; // subtree gone, return sibling
					} else {
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				} else { // delete from right
					right = right.delete(pt); // update this node's information
					if (right == null) {
						return left; // subtree gone, return sibling
					} else {
						updateSizeAndHeight();
						return this;
					}
				}
			} else { // y-splitter
				if (compareYX.compare(pt, splitter) <= 0) { // delete from left
					left = left.delete(pt);
					if (left == null) {
						return right; // subtree gone, return sibling
					} else {
						updateSizeAndHeight(); // update this node's information
						return this;
					}
				} else { // delete from right
					right = right.delete(pt); // update this node's information
					if (right == null) {
						return left; // subtree gone, return sibling
					} else {
						updateSizeAndHeight();
						return this;
					}
				}
			}
		}

		void updateSizeAndHeight() {
			size = getSize(left) + getSize(right);
			height = 1 + Math.max(getHeight(left), getHeight(right));
		}

		// add subtree to list
		void entryList(List<P> list) {
			left.entryList(list);
			right.entryList(list);
		}

		// print XML
		void print(Element element) {
			// print this item
			Element out = resultsDoc.createElement("internal");
			out.setAttribute("splitDim", Integer.toString(cutDim));
			out.setAttribute("x", Integer.toString((int) splitter.getX()));
			out.setAttribute("y", Integer.toString((int) splitter.getY()));
			element.appendChild(out);

			left.print(out); // recurse on children
			right.print(out);
		}

		boolean check(P low, P high) {
			boolean leftCheck = left.check(low, splitter); // check left side
			boolean rightCheck = right.check(splitter, high); // check right side

			if (size != getSize(left) + getSize(right)) {
				System.out.println("Size check fails at internal: " + toString());
				return false;
			} else if (height != 1 + Math.max(getHeight(left), getHeight(right))) {
				System.out.println("Height check fails at internal: " + toString());
				return false;
			} else
				return leftCheck && rightCheck;
		}

		String debugPrint(String prefix) {
			return left.debugPrint(prefix + "| ") + System.lineSeparator() + prefix + toString()
					+ System.lineSeparator() + right.debugPrint(prefix + "| ");
		}

		public String toString() {
			return cutDim + "- " + "(" + splitter.toString() + " ht:" + height + " sz:" + size + ")";
		}

		public float distanceTo(P q, P point) {
			return (float) Math.sqrt((q.getX() - point.getX()) * (q.getX() - point.getX())
					+ (q.getY() - point.getY()) * (q.getY() - point.getY()));
		}

		public P getSplitter() {
			return splitter;
		}

		public Node getLeft() {
			return left;
		}

		public Node getRight() {
			return right;
		}

		public P nearestNeighbor(P q, Node p, Rectangle cell, float bestDist, P bestPoint) throws Exception {
			if (p != null) {
				int cd = cutDim;
				P thisPoint;

				InternalNode pInternal;
				if (!p.isExternal) {
					pInternal = (InternalNode) p;
				} else {
					throw new Exception("nearestNeighbor p " + p.toString() + " not internal");
				}

				Rectangle leftCell = cell.leftPart(cd,
						new Point2D(pInternal.getSplitter().getX(), pInternal.getSplitter().getY())); // left child's
																										// cell
				Rectangle rightCell = cell.rightPart(cd,
						new Point2D(pInternal.getSplitter().getX(), pInternal.getSplitter().getY())); // right child's
																										// cell

				if (q.get(cd) < pInternal.getSplitter().get(cd)) { // q is closer to left
					if (DEBUG)
						System.out.printf("nearest internal go left q:%s splitter:%s\n", q.toString(),
								splitter.toString());

					thisPoint = left.nearestNeighbor(q, pInternal.getLeft(), leftCell, bestDist, bestPoint);
					if (distanceTo(q, thisPoint) < distanceTo(q, bestPoint)) {
						bestPoint = thisPoint;
					}
					thisPoint = right.nearestNeighbor(q, pInternal.getRight(), rightCell, bestDist, bestPoint);
					if (distanceTo(q, thisPoint) < distanceTo(q, bestPoint)) {
						bestPoint = thisPoint;
					}
				} else { // q is closer to right
					if (DEBUG)
						System.out.printf("nearest internal go right q:%s splitter:%s\n", q.toString(),
								splitter.toString());

					thisPoint = right.nearestNeighbor(q, pInternal.getRight(), rightCell, bestDist, bestPoint);
					if (distanceTo(q, thisPoint) < distanceTo(q, bestPoint)) {
						bestPoint = thisPoint;
					}
					thisPoint = left.nearestNeighbor(q, pInternal.getLeft(), leftCell, bestDist, bestPoint);
					if (distanceTo(q, thisPoint) < distanceTo(q, bestPoint)) {
						bestPoint = thisPoint;
					}
				}
			}
			return bestPoint;
		}

		public void setCutDim(int cutDim) {
			this.cutDim = cutDim;
		}
	}

	// -----------------------------------------------------------------
	// External node
	// -----------------------------------------------------------------

	private class ExternalNode extends Node {

		final P point; // the associated point object

		ExternalNode(P point) {
			super(true);
			this.point = point;
		}

		P find(P pt) {
			if (comparator.compare(pt, point) == 0)
				return point;
			else
				return null;
		}

		Node insert(P pt) throws Exception {
			ArrayList<P> list = new ArrayList<P>(); // array list for points
			list.add(pt); // add points to list
			list.add(point);

			List<Point2D> listPoint2D = new ArrayList<Point2D>();
			listPoint2D.add(pt.getPoint2D());
			listPoint2D.add(point.getPoint2D());
			rect = rect.minimumRectangle(listPoint2D);
			low = rect.getLow();
			high = rect.getHigh();

//			rect = new Rectangle(new Point2D(0, 0), new Point2D(getMapWidth(), getMapHeight()));

			// sort list by appropriate splitting dimension
			if (high.getX() - low.getX() >= high.getY() - low.getY()) {
				// split along x-coordinate (vertically)

				// additional comparison to assign the minimum rectangle containing pt and point
//				if (compareXY.compare(pt, point) <= 0) { // pt is less or equal
//					low = new Point2D(pt.getX(), pt.getY());
//					high = new Point2D(point.getX(), point.getY());
//				} else {
//					low = new Point2D(point.getX(), point.getY());
//					high = new Point2D(pt.getX(), pt.getY());
//				}

				Collections.sort(list, compareXY);

			} else {
				// split along y-coordinate (horizontally)

				// additional comparison to assign the minimum rectangle containing pt and point
//				if (compareYX.compare(pt, point) <= 0) { // pt is less or equal
//					low = new Point2D(pt.getX(), pt.getY());
//					high = new Point2D(point.getX(), point.getY());
//				} else {
//					low = new Point2D(point.getX(), point.getY());
//					high = new Point2D(pt.getX(), pt.getY());
//				}

				Collections.sort(list, compareYX);
			}

			if (DEBUG) {
				System.out.printf("Inserting External %s low: %d %d high: %d %d\n", pt.toString(), (int) low.getX(),
						(int) low.getY(), (int) high.getX(), (int) high.getY());
			}

			rect = new Rectangle(new Point2D(0, 0), new Point2D(getMapWidth(), getMapHeight()));

			return buildTree(list); // build a tree and return
		}

		Node rebalance(P pt) {
			assert (false); // should never get here
			return null;
		}

		Node delete(P pt) throws Exception {
			if (comparator.compare(pt, point) == 0) { // found it
				return null;
			} else {
				throw new Exception("cityDoesNotExist");
			}
		}

		void entryList(List<P> list) {
			list.add(point);
		}

		void print(Element element) {
			Element out = resultsDoc.createElement("external");
			out.setAttribute("name", point.getName());
			out.setAttribute("x", Integer.toString((int) point.getX()));
			out.setAttribute("y", Integer.toString((int) point.getY()));
			element.appendChild(out);
		}

		boolean check(P low, P high) {
			if (high.getX() - low.getX() >= high.getY() - low.getY()) {
				// split along x-coordinate (vertically)
				if ((low != null && compareXY.compare(point, low) < 0)
						|| (high != null && compareXY.compare(point, high) > 0)) {
					System.out.println("Membership check fails at external: " + toString());
					return false;
				} else {
					return true;
				}
			} else {
				// split along y-coordinate (horizontally)
				if ((low != null && compareYX.compare(point, low) < 0)
						|| (high != null && compareYX.compare(point, high) > 0)) {
					System.out.println("Membership check fails at external: " + toString());
					return false;
				} else {
					return true;
				}
			}
		}

		String debugPrint(String prefix) {
			return prefix + toString();
		}

		public String toString() {
			return "[" + point.toString() + "]";
		}

		public P getPoint() {
			return point;
		}

		public float distanceToNode(P q, Node p) throws Exception {
			if (p.isExternal) {
				ExternalNode pExternal = (ExternalNode) p;
				P pPoint = pExternal.getPoint();

				return (float) Math.sqrt((q.getX() - pPoint.getX()) * (q.getX() - pPoint.getX())
						+ (q.getY() - pPoint.getY()) * (q.getY() - pPoint.getY()));
			} else {
				throw new Exception("distanceTo, Node p " + p.toString() + " is not external");
			}
		}

		public float distanceTo(P q, P p) {
			return (float) Math.sqrt(
					(q.getX() - p.getX()) * (q.getX() - p.getX()) + (q.getY() - p.getY()) * (q.getY() - p.getY()));
		}

		public P nearestNeighbor(P q, Node p, Rectangle cell, float bestDist, P bestPoint) throws Exception {
			float thisDist = Float.MAX_VALUE;
			try {
				thisDist = distanceToNode(q, p);
				bestDist = distanceTo(q, bestPoint);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // distance to p's point (external node)

			if (DEBUG)
				System.out.printf("nearest external q:%s point:%s thisDist:%d bestDist:%d\n", q.toString(),
						point.toString(), (int) thisDist, (int) bestDist);

			ExternalNode pExternal;
			if (p.isExternal) {
				pExternal = (ExternalNode) p;
			} else {
				throw new Exception("nearestNeighbor p " + p.toString() + " not external");
			}

			P pPoint = pExternal.getPoint();

			if (thisDist < bestDist)
				return pPoint;
			else
				return bestPoint;
		}
	}

	// -----------------------------------------------------------------
	// Tree utilities
	// -----------------------------------------------------------------

	@SuppressWarnings("unchecked")
	int getSize(Node p) {
		if (p.isExternal)
			return 1;
		else
			return ((InternalNode) p).size;
	}

	@SuppressWarnings("unchecked")
	int getHeight(Node p) {
		if (p.isExternal)
			return 0;
		else
			return ((InternalNode) p).height;
	}

	Node rebuild(Node p) {
		if (DEBUG) {
			System.out.println("KD tree: Rebuilding subtree rooted at " + p + ". Subtree before rebuild:"
					+ System.lineSeparator() + p.debugPrint(".."));
		}
		if (p.isExternal) {
			return p; // external - nothing to do
		}

		ArrayList<P> list = new ArrayList<P>(); // place to store points
		p.entryList(list); // generate the list

		if (DEBUG) {
			System.out.println("list before rebuild " + Arrays.toString(list.toArray()));
		}

		// sort list by appropriate splitting dimension
		if (high.getX() - low.getX() >= high.getY() - low.getY()) {
			// split along x-coordinate (vertically)
			Collections.sort(list, compareXY);
		} else {
			// split along y-coordinate (horizontally)
			Collections.sort(list, compareYX);
		}

		if (DEBUG) {
			System.out.println("list in rebuild " + Arrays.toString(list.toArray()));
		}

		Node t = buildTreeRebalance(list); // build new subtree from list

		rect = new Rectangle(new Point2D(0, 0), new Point2D(getMapWidth(), getMapHeight()));

		if (DEBUG) {
			System.out.println("KD tree: Subtree after rebuild:" + System.lineSeparator() + t.debugPrint(".."));
		}

		return t;
	}

	// Balance the tree
	Node buildTreeRebalance(List<P> list) {
		if (DEBUG)
			System.out.println("buildTree list " + Arrays.toString(list.toArray()));

		int k = list.size();
		if (k == 0) { // no points at all
			return null;
		} else if (k == 1) { // a single point
			if (DEBUG)
				System.out.println("External Node " + list.get(0).toString());
			return new ExternalNode(list.get(0));
		} else {
			List<Point2D> listPoint2D = new ArrayList<Point2D>();

			for (int i = 0; i < list.size(); i++) {
				listPoint2D.add(list.get(i).getPoint2D());
			}

			rect = rect.minimumRectangle(listPoint2D);

			int cutDim;
			low = rect.getLow();
			high = rect.getHigh();

			if (DEBUG)
				System.out.println("rebalance rectangle low " + low.toString() + " high " + high.toString());

			if (high.getX() - low.getX() >= high.getY() - low.getY()) {// split along x-coordinate (vertically)
				cutDim = 0;
				if (DEBUG)
					System.out.println("list for left " + Arrays.toString(list.toArray()));
				Collections.sort(list, compareXY);
			} else {// split along y-coordinate (horizontally)
				cutDim = 1;
				if (DEBUG)
					System.out.println("list for left " + Arrays.toString(list.toArray()));
				Collections.sort(list, compareYX);
			}

			int m = (int) Math.ceil((float) k / 2); // size of left subtree
			P splitter = list.get(m - 1); // splitter value

			if (DEBUG)
				System.out.println("splitter " + splitter.toString() + " cutdim " + cutDim);

			// recursively build left and right subtrees
			Node left = buildTreeRebalance(list.subList(0, m));
			Node right = buildTreeRebalance(list.subList(m, k));

			// combine the lists under median (median goes into left subtree)
			InternalNode p = new InternalNode(splitter, left, right);
			p.setCutDim(cutDim);
			if (DEBUG)
				System.out.println("Internal Node " + p.toString() + " cutDim: " + cutDim);

			p.updateSizeAndHeight(); // update p's information
			return p;
		}
	}

	Node buildTree(List<P> list) {
		if (DEBUG)
			System.out.println("buildTree list " + Arrays.toString(list.toArray()));

		int k = list.size();
		if (k == 0) { // no points at all
			return null;
		} else if (k == 1) { // a single point
			return new ExternalNode(list.get(0));
		} else {
			int m = (int) Math.ceil((float) k / 2); // size of left subtree
			P splitter = list.get(m - 1); // splitter value
			// recursively build left and right subtrees
			Node left = buildTree(list.subList(0, m));
			Node right = buildTree(list.subList(m, k));

			// combine the lists under median (median goes into left subtree)
			InternalNode p = new InternalNode(splitter, left, right);
			p.updateSizeAndHeight(); // update p's information
			return p;
		}
	}

	// -----------------------------------------------------------------
	// Private member data
	// -----------------------------------------------------------------

	private Node root = null; // root of the tree
	private final Comparator<P> comparator; // comparator for ordering the tree
	private final CompareXY<P> compareXY; // x-splitter comparator
	private final CompareYX<P> compareYX; // y-splitter comparator
	private final Document resultsDoc; // results document (for printing)
	private int nItems; // number of items (equals getSize(root))
	private int maxItems; // upper bound on the number of items

	private Point2D low; // lower-left corner point
	private Point2D high; // upper-right corner point

	private Rectangle rect; // rectangle

	private int mapWidth; // bounding box
	private int mapHeight; // bounding box

	// -----------------------------------------------------------------
	// Public members
	// -----------------------------------------------------------------

	public SGKDTree(Comparator<P> comparator, Document resultsDoc, CompareXY<P> compareXY, CompareYX<P> compareYX,
			int mapWidth, int mapHeight) {
		root = null;
		this.comparator = comparator;
		this.compareXY = compareXY;
		this.compareYX = compareYX;
		this.resultsDoc = resultsDoc;
		maxItems = nItems = 0;

		low = new Point2D(0, 0);
		high = new Point2D(mapWidth, mapHeight);

		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;

		rect = new Rectangle(low, high);
	}

	public int size() {
		return nItems;
	}

	public P find(P pt) {
		if (root == null) {
			return null;
		} else {
			return root.find(pt);
		}
	}

	public void insert(P pt) throws Exception {
		if (DEBUG) {
			System.out.println("\nKD tree: Inserting " + pt);
		}
		if (root == null) {
			root = new ExternalNode(pt);
		} else {
			root = root.insert(pt);
		}
		nItems++;
		maxItems++;
		assert (nItems == getSize(root));
		int maxAllowedHeight = (int) (Math.log(maxItems) / Math.log((double) BALANCE_DENOM / (double) BALANCE_NUM));
		if (getHeight(root) > maxAllowedHeight) {
			root = root.rebalance(pt);
		}
		if (DEBUG) {
			System.out.println("KD tree: After insertion of " + pt + System.lineSeparator() + debugPrint("  "));
//			root.check(null, null);
		}
	}

	public void delete(P pt) throws Exception {
		if (DEBUG) {
			System.out.println("KD tree: Deleting " + pt);
		}
		if (root == null) {
			throw new Exception("cityDoesNotExist");
		} else {
			root = root.delete(pt);
		}
		nItems--;
		if (2 * nItems < maxItems) {
			root = rebuild(root);
			if (DEBUG) {
				System.out.println("KD tree: Triggered rebuild after deletion. n = " + nItems + " m = " + maxItems);
			}
			maxItems = nItems;
		}
		if (DEBUG) {
			System.out.println("KD tree: After deleting " + pt + System.lineSeparator() + debugPrint("  "));
			root.check(null, null);
		}
	}

	public void clear() {
		root = null;
		maxItems = nItems = 0;
		low = high = null;
	}

	public List<P> entryList() {
		ArrayList<P> list = new ArrayList<P>();
		if (root != null) {
			root.entryList(list);
		}
		return list;
	}

	String debugPrint(String prefix) {
		if (root != null)
			return root.debugPrint(prefix);
		else
			return new String();
	}

	public void print(Element element) {
		Element out = resultsDoc.createElement("KdTree");
		element.appendChild(out);
		if (root != null)
			root.print(out);
	}

	public P nearestNeighbor(P q, Node p, Rectangle cell, float bestDist, P bestPoint) throws Exception {
		if (root != null) {
			return root.nearestNeighbor(q, root, rect, bestDist, bestPoint);
		} else {
			return null;
		}
	}

	public Point2D getLow() {
		return low;
	}

	public Point2D getHigh() {
		return high;
	}

	public int getMapWidth() {
		return mapWidth;
	}

	public int getMapHeight() {
		return mapHeight;
	}

//	public static void main(String[] args) throws Exception {
////		Document resultsDoc = null;
////		SGKDTree<City> tree = new SGKDTree<City>(new OrderByCoordinate<City>(), resultsDoc, new CompareXY<City>(),
////				new CompareYX<City>(), 1250, 1250);
////
////		tree.insert(new City(500, 500, "Edinburgh", "Green", 0));
////		tree.insert(new City(750, 600, "Lisbon", "Green", 5));
////		tree.insert(new City(400, 800, "Varna", "Green", 5));
////		tree.insert(new City(300, 400, "London", "Green", 5));
////		tree.insert(new City(200, 200, "Prague", "Green", 5));
////		tree.insert(new City(900, 450, "Madrid", "Green", 5));
////		tree.insert(new City(700, 300, "Paris", "Green", 5));
////		tree.insert(new City(800, 100, "Copenhagen", "Green", 5));
////		tree.insert(new City(850, 250, "Nice", "Green", 5));
////		
//
////		City a = tree.nearestNeighbor(new City(400, 400, "Shangai", "Green", 5), null,
////				new Rectangle(tree.getLow(), tree.getHigh()), Integer.MAX_VALUE, new City(400, 400, "san", "Green", 5));
////		System.out.println(a.toString());
//
////		 System.out.println(tree.debugPrint(""));
//
//		// random input
//		FileWriter fileWriter = null;
//		try {
//			fileWriter = new FileWriter(
//					"D:/Documents/University of Maryland - CP/Maryland Fall 2019/CMSC420/MeeshQuest-Skeleton-part2/testinput.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		PrintWriter printWriter = new PrintWriter(fileWriter);
//		printWriter.println("<commands\r\n" + "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
//				+ "	xsi:noNamespaceSchemaLocation=\"part2in.xsd\"\r\n"
//				+ "	spatialWidth=\"1024\" spatialHeight=\"1024\">");
//		for (int i = 0; i < 20; i++) {
//			String x = Integer.toString((int) ((Math.random() * ((50 - 0) + 1)) + 0));
//			String y = Integer.toString((int) ((Math.random() * ((50 - 0) + 1)) + 0));
////			String s = "s" + (x) + (y);
//			String s = "s" + i;
//			printWriter.println(
//					"<createCity name=\"" + s + "\" x=\"" + x + "\" y=\"" + y + "\" radius=\"0\" color=\"black\"/>");
////			printWriter.println("<deleteCity name=\"" + s + "\"/>");
////			printWriter.println("<deleteCity name=\"" + s + "\"/>");
//			printWriter.println(
//					"<createCity name=\"" + s + "\" x=\"" + x + "\" y=\"" + y + "\" radius=\"0\" color=\"black\"/>");
////			if (i % 20 == 0) {
////				printWriter.println("<listCities sortBy=\"name\"/>");
////				printWriter.println("<printBinarySearchTree/>");
//			printWriter.println("<printKdTree/>");
////			}
////			printWriter.println("<nearestNeighbor x=\"" + x + "\" y=\"" + y + "\"/>");
//		}
//		printWriter.println("</commands>");
//		printWriter.close();
//	}
}
