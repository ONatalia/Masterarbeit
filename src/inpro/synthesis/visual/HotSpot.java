package inpro.synthesis.visual;

import java.awt.Point;

/**
 * A hotspot associates an item with a point on the screen.
 * it can be checked whether a given Point matches this hotspot
 * (that is, the point is within the hotspot; the hotspot size
 * is given in the constructor)
 * and can then return the item that is associated with the hotspot. 
 * 
 * Hotspots are used in SegmentSelector and also in SegmentPanel to
 * allow easy access to PitchMarks
 * @author timo
 */
@SuppressWarnings("serial")
class HotSpot<T> extends Point {
	final T item;
	final int squaredHotspotSize;
	
	HotSpot(Point p, T item, float hotspotSize) {
		super(p);
		this.squaredHotspotSize = (int) (hotspotSize * hotspotSize);
		this.item = item;
	}
	
	HotSpot(int x, int y, T item, float hotspotSize) {
		this(new Point(x, y), item, hotspotSize);
	}
	
	HotSpot(T item, int x, int y, float hotspotSize) {
		this(new Point(x, y), item, hotspotSize);
	}
	
	boolean matches(Point p) {
		return (squaredDistance(this, p) < squaredHotspotSize);
	}
	
	T getItem() {
		return item;
	}

	public static int squaredDistance(Point p1, Point p2) {
		int xDelta = p1.x - p2.x;
		int yDelta = p1.y - p2.y;
		return xDelta * xDelta + yDelta * yDelta;
	}
}