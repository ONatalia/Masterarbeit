package org.cocolab.deawu;

@SuppressWarnings("serial")
public class Point extends java.awt.Point {

	public Point (int x, int y) {
		super(x, y);
	}

	public Point (Point p) {
		super(p.x, p.y);
	}

	public String toString() {
		return "("+this.x+", "+this.y+")";
	}


	public void add(Point p) {
		this.x += p.x;
		this.y += p.y;
	}

	public void add(int s) {
		this.x += s;
		this.y += s;
	}


	public void sub(Point p) {
		this.x -= p.x;
		this.y -= p.y;
	}

	public void max(Point p) {
		if (this.x < p.x) this.x = p.x;    // this.x = max(this.x, p.x)
		if (this.y < p.y) this.y = p.y;   
	}

	public void min(Point p) {
		if (this.x > p.x) this.x = p.x;
		if (this.y > p.y) this.y = p.y;
	}

	public void div(int s) {
		// integer divide, drops remainder
		this.x /= s;
		this.y /= s;
	}

	public void mul(int s) {
		this.x *= s;
		this.y *= s;
	}

	public void copy(Point p) {
		this.x = p.x;
		this.y = p.y;
	}

	public void copy(int x0, int y0) {
		this.x = x0;
		this.y = y0;
	}
}
