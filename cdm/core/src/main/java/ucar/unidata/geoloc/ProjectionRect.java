/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.common.math.DoubleMath;
import ucar.nc2.util.Misc;
import java.io.*;

/**
 * Bounding box for ProjectionPoint's.
 * Note that getX() getY() really means getMinX(), getMinY(), rather than
 * "upper left point" of the rectangle.
 *
 * LOOK will not implement Serializable in ver6
 * LOOK may be immutable AutoValue in ver6
 */
public class ProjectionRect implements java.io.Serializable {
  // TODO make final in ver6
  private double x, y, width, height;

  /**
   * default constructor, initialized to center (0,0) and width (10000, 10000)
   */
  public ProjectionRect() {
    this(-5000, -5000, 5000, 5000);
  }

  /**
   * Construct a ProjectionRect from any two opposite corner points.
   *
   * @param corner1 a corner.
   * @param corner2 the opposite corner.
   */
  public ProjectionRect(ProjectionPoint corner1, ProjectionPoint corner2) {
    this(corner1.getX(), corner1.getY(), corner2.getX(), corner2.getY());
  }

  /**
   * Construct a ProjectionRect from any two opposite corner points.
   *
   * @param minimum lower left corner, ie the minimum x and y
   * @param width x width.
   * @param height y height
   */
  public ProjectionRect(ProjectionPoint minimum, double width, double height) {
    setRect(minimum.getX(), minimum.getY(), width, height);
  }

  /**
   * Copy Constructor
   *
   * @param r rectangle to copy
   */
  public ProjectionRect(ProjectionRect r) {
    this(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
  }

  /**
   * construct a MapArea from any two opposite corner points
   *
   * @param x1 x coord of any corner of the bounding box
   * @param y1 y coord of the same corner as x1
   * @param x2 x coord of opposite corner from x1,y1
   * @param y2 y coord of same corner as x2
   */
  public ProjectionRect(double x1, double y1, double x2, double y2) {
    double wx0 = 0.5 * (x1 + x2);
    double wy0 = 0.5 * (y1 + y2);
    double width = Math.abs(x1 - x2);
    double height = Math.abs(y1 - y2);
    setRect(wx0 - width / 2, wy0 - height / 2, width, height);
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  ////////////////////////////////////////////////////
  // taken from java.awt.geom.Rectangle2D, removed because awt missing on android

  /**
   * Returns the smallest X coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the smallest X coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMinX() {
    return getX();
  }

  /**
   * Returns the smallest Y coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the smallest Y coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMinY() {
    return getY();
  }

  /**
   * Returns the largest X coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the largest X coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMaxX() {
    return getX() + getWidth();
  }

  /**
   * Returns the largest Y coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the largest Y coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMaxY() {
    return getY() + getHeight();
  }

  /**
   * Returns the X coordinate of the center of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   * 
   * @return the X coordinate of the center of the framing rectangle
   *         of the <code>Shape</code>.
   * @since 1.2
   */
  public double getCenterX() {
    return getX() + getWidth() / 2.0;
  }

  /**
   * Returns the Y coordinate of the center of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   * 
   * @return the Y coordinate of the center of the framing rectangle
   *         of the <code>Shape</code>.
   * @since 1.2
   */
  public double getCenterY() {
    return getY() + getHeight() / 2.0;
  }

  /**
   * Adds a <code>Rectangle2D</code> object to this
   * <code>Rectangle2D</code>. The resulting <code>Rectangle2D</code>
   * is the union of the two <code>Rectangle2D</code> objects.
   * 
   * @param r the <code>Rectangle2D</code> to add to this
   *        <code>Rectangle2D</code>.
   * @since 1.2
   * @deprecated use builder
   */
  @Deprecated
  public void add(ProjectionRect r) {
    double x1 = Math.min(getMinX(), r.getMinX());
    double x2 = Math.max(getMaxX(), r.getMaxX());
    double y1 = Math.min(getMinY(), r.getMinY());
    double y2 = Math.max(getMaxY(), r.getMaxY());
    setRect(x1, y1, x2 - x1, y2 - y1);
  }

  /**
   * Adds a point, specified by the double precision arguments
   * <code>newx</code> and <code>newy</code>, to this
   * <code>Rectangle2D</code>. The resulting <code>Rectangle2D</code>
   * is the smallest <code>Rectangle2D</code> that
   * contains both the original <code>Rectangle2D</code> and the
   * specified point.
   * <p>
   * After adding a point, a call to <code>contains</code> with the
   * added point as an argument does not necessarily return
   * <code>true</code>. The <code>contains</code> method does not
   * return <code>true</code> for points on the right or bottom
   * edges of a rectangle. Therefore, if the added point falls on
   * the left or bottom edge of the enlarged rectangle,
   * <code>contains</code> returns <code>false</code> for that point.
   * 
   * @param newx the X coordinate of the new point
   * @param newy the Y coordinate of the new point
   * @since 1.2
   * @deprecated use builder
   */
  @Deprecated
  public void add(double newx, double newy) {
    double x1 = Math.min(getMinX(), newx);
    double x2 = Math.max(getMaxX(), newx);
    double y1 = Math.min(getMinY(), newy);
    double y2 = Math.max(getMaxY(), newy);
    setRect(x1, y1, x2 - x1, y2 - y1);
  }

  /**
   * Adds the <code>Point2D</code> object <code>pt</code> to this
   * <code>Rectangle2D</code>.
   * The resulting <code>Rectangle2D</code> is the smallest
   * <code>Rectangle2D</code> that contains both the original
   * <code>Rectangle2D</code> and the specified <code>Point2D</code>.
   * <p>
   * After adding a point, a call to <code>contains</code> with the
   * added point as an argument does not necessarily return
   * <code>true</code>. The <code>contains</code>
   * method does not return <code>true</code> for points on the right
   * or bottom edges of a rectangle. Therefore, if the added point falls
   * on the left or bottom edge of the enlarged rectangle,
   * <code>contains</code> returns <code>false</code> for that point.
   * 
   * @param pt the new <code>Point2D</code> to add to this
   *        <code>Rectangle2D</code>.
   * @since 1.2
   * @deprecated use builder
   */
  @Deprecated
  public void add(ProjectionPoint pt) {
    add(pt.getX(), pt.getY());
  }

  public boolean isEmpty() {
    return (width <= 0.0) || (height <= 0.0);
  }

  public boolean intersects(ProjectionRect r) {
    return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
  }

  public boolean intersects(double x, double y, double w, double h) {
    if (isEmpty() || w <= 0 || h <= 0) {
      return false;
    }
    double x0 = getX();
    double y0 = getY();
    return (x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight());
  }

  /**
   * Intersects the pair of specified source <code>Rectangle2D</code>
   * objects and puts the result into the specified destination
   * <code>Rectangle2D</code> object. One of the source rectangles
   * can also be the destination to avoid creating a third Rectangle2D
   * object, but in this case the original points of this source
   * rectangle will be overwritten by this method.
   * 
   * @param src1 the first of a pair of <code>Rectangle2D</code>
   *        objects to be intersected with each other
   * @param src2 the second of a pair of <code>Rectangle2D</code>
   *        objects to be intersected with each other
   * @param dest the <code>Rectangle2D</code> that holds the
   *        results of the intersection of <code>src1</code> and
   *        <code>src2</code>
   * @since 1.2
   */
  public static void intersect(ProjectionRect src1, ProjectionRect src2, ProjectionRect dest) {
    double x1 = Math.max(src1.getMinX(), src2.getMinX());
    double y1 = Math.max(src1.getMinY(), src2.getMinY());
    double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
    double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
    dest.setRect(x1, y1, x2 - x1, y2 - y1);
  }

  /**
   * Returns {@code true} if this bounding box contains {@code point}.
   *
   * @param point a point in projection coordinates.
   * @return {@code true} if this bounding box contains {@code point}.
   */
  public boolean contains(ProjectionPoint point) {
    return DoubleMath.fuzzyCompare(point.getX(), getMinX(), 1e-6) >= 0
        && DoubleMath.fuzzyCompare(point.getX(), getMaxX(), 1e-6) <= 0
        && DoubleMath.fuzzyCompare(point.getY(), getMinY(), 1e-6) >= 0
        && DoubleMath.fuzzyCompare(point.getY(), getMaxY(), 1e-6) <= 0;
  }

  /**
   *
   * Returns {@code true} if this bounding box contains {@code rect}.
   *
   * @param rect a bounding box in projection coordinates
   * @return {@code true} if this bounding box contains {@code rect}.
   */
  public boolean contains(ProjectionRect rect) {
    boolean contained;
    // The old ProjectionRect class was based off of java.awt.Rectangle.
    // If the rectangles were the same, .contains(rect) returned true.
    // This check makes sure the old behavior is preserved.
    if (this.equals(rect)) {
      contained = true;
    } else {
      // Just check to see if corners of rect contained within this
      contained = (this.contains(rect.getMinPoint()) && this.contains(rect.getMaxPoint()));
    }
    return contained;
  }

  /////////////////////////////////////////////////////

  /**
   * Get the Lower Right Point
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerRightPoint() {
    return ProjectionPoint.create(getMaxPoint().getX(), getMinPoint().getY());
  }

  /**
   * Get the Upper Left Point (same as getMaxPoint)
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperRightPoint() {
    return getMaxPoint();
  }

  /**
   * Get the Lower Right Point (same as getMinPoint)
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerLeftPoint() {
    return getMinPoint();
  }

  /**
   * Get the Upper Left Point
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperLeftPoint() {
    return ProjectionPoint.create(getMinPoint().getX(), getMaxPoint().getY());
  }

  /**
   * Get the minimum corner of the bounding box.
   *
   * @return minimum corner of the bounding box
   */
  public ProjectionPoint getMinPoint() {
    return ProjectionPoint.create(getX(), getY());
  }

  /**
   * Get the maximum corner of the bounding box.
   *
   * @return maximum corner of the bounding box
   */
  public ProjectionPoint getMaxPoint() {
    return ProjectionPoint.create(getX() + getWidth(), getY() + getHeight());
  }

  /**
   * Get a String representation of this object.
   *
   * @return a String representation of this object.
   */
  public String toString() {
    return String.format("min: %.3f %.3f size: %.3f %.3f", getX(), getY(), getWidth(), getHeight());
  }

  public String toString2(int ndec) {
    String f = " %." + ndec + "f";
    return String.format("min:" + f + f + " max:" + f + f, getX(), getY(), getMaxX(), getMaxY());
  }

  /**
   * set minimum X
   *
   * @deprecated use builder
   */
  @Deprecated
  public void setX(double x) {
    setRect(x, getY(), getWidth(), getHeight());
  }

  /**
   * set minimum Y
   *
   * @param y minimum y
   * @deprecated use builder
   */
  @Deprecated
  public void setY(double y) {
    setRect(getX(), y, getWidth(), getHeight());
  }

  /**
   * set X width
   *
   * @param w x width
   * @deprecated use builder
   */
  @Deprecated
  public void setWidth(double w) {
    setRect(getX(), getY(), w, getHeight());
  }

  /**
   * set Y height
   *
   * @param h Y height
   * @deprecated use builder
   */
  @Deprecated
  public void setHeight(double h) {
    setRect(getX(), getY(), getWidth(), h);
  }

  /** @deprecated use builder */
  @Deprecated
  public void setRect(ProjectionRect r) {
    setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
  }

  /** @deprecated use builder */
  @Deprecated
  public void setRect(double x, double y, double w, double h) {
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
  }

  // serialization

  /**
   * Read the object from the input stream of the serialized object
   *
   * @param s stream to read
   * @throws IOException Problem reading from stream
   */
  private void readObject(ObjectInputStream s) throws IOException {
    double x = s.readDouble();
    double y = s.readDouble();
    double w = s.readDouble();
    double h = s.readDouble();
    setRect(x, y, w, h);
  }

  /**
   * Wrtie the object to the output stream
   *
   * @param s stream to write
   * @throws IOException Problem writing to stream
   */
  private void writeObject(ObjectOutputStream s) throws IOException {
    s.writeDouble(getX());
    s.writeDouble(getY());
    s.writeDouble(getWidth());
    s.writeDouble(getHeight());
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProjectionRect that = (ProjectionRect) o;

    if (Double.compare(that.height, height) != 0)
      return false;
    if (Double.compare(that.width, width) != 0)
      return false;
    if (Double.compare(that.x, x) != 0)
      return false;
    return Double.compare(that.y, y) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(width);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(height);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns the result of {@link #nearlyEquals(ProjectionRect, double)}, with {@link Misc#defaultMaxRelativeDiffFloat}.
   */
  public boolean nearlyEquals(ProjectionRect other) {
    return nearlyEquals(other, Misc.defaultMaxRelativeDiffFloat);
  }

  /**
   * Returns {@code true} if this rectangle is nearly equal to {@code other}. The "near equality" of corners is
   * determined using {@link ProjectionPoint#nearlyEquals(ProjectionPoint, double)}, with the specified maxRelDiff.
   *
   * @param other the other rectangle to check.
   * @param maxRelDiff the maximum {@link Misc#relativeDifference relative difference} that two corners may have.
   * @return {@code true} if this rectangle is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(ProjectionRect other, double maxRelDiff) {
    return this.getLowerLeftPoint().nearlyEquals(other.getLowerLeftPoint(), maxRelDiff)
        && this.getUpperRightPoint().nearlyEquals(other.getUpperRightPoint(), maxRelDiff);
  }
}