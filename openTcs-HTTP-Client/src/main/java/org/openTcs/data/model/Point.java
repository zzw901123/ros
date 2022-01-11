/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.openTcs.data.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import org.openTcs.data.ObjectHistory;
import org.openTcs.data.TCSObject;
import org.openTcs.data.TCSObjectReference;
import static org.openTcs.util.Assertions.checkArgument;

/**
 * Describes a position in the driving course at which a {@link Vehicle} may be located.
 *
 * @see Path
 * @author Stefan Walter (Fraunhofer IML)
 */
public class Point
    extends TCSResource<Point>
    implements Serializable {

  /**
   * This point's coordinates in mm.
   */
  private final Triple position;
  /**
   * This point's type.
   */
  private final Type type;
  /**
   * The vehicle's (assumed) orientation angle (-360..360) when it is at this
   * position.
   * May be Double.NaN if an orientation angle is not defined for this point.
   */
  private final double vehicleOrientationAngle;
  /**
   * A set of references to paths ending in this point.
   */
  private final Set<TCSObjectReference<Path>> incomingPaths;
  /**
   * A set of references to paths originating in this point.
   */
  private final Set<TCSObjectReference<Path>> outgoingPaths;
  /**
   * A set of links attached to this point.
   */
  private final Set<Location.Link> attachedLinks;
  /**
   * A reference to the vehicle occupying this point.
   */
  private final TCSObjectReference<Vehicle> occupyingVehicle;

  /**
   * Creates a new point with the given name.
   *
   * @param name This point's name.
   */
  public Point(String name) {
    super(name);
    this.position = new Triple(0, 0, 0);
    this.type = Type.HALT_POSITION;
    this.vehicleOrientationAngle = Double.NaN;
    this.incomingPaths = new HashSet<>();
    this.outgoingPaths = new HashSet<>();
    this.attachedLinks = new HashSet<>();
    this.occupyingVehicle = null;
  }

  private Point(String name,
                Map<String, String> properties,
                ObjectHistory history,
                Triple position,
                Type type,
                double vehicleOrientationAngle,
                Set<TCSObjectReference<Path>> incomingPaths,
                Set<TCSObjectReference<Path>> outgoingPaths,
                Set<Location.Link> attachedLinks,
                TCSObjectReference<Vehicle> occupyingVehicle) {
    super(name, properties, history);
    this.position = requireNonNull(position, "position");
    this.type = requireNonNull(type, "type");
    checkArgument(Double.isNaN(vehicleOrientationAngle)
        || (vehicleOrientationAngle >= -360.0 && vehicleOrientationAngle <= 360.0),
                  "angle not in [-360..360]: %s",
                  vehicleOrientationAngle);
    this.vehicleOrientationAngle = vehicleOrientationAngle;
    this.incomingPaths = setWithoutNullValues(requireNonNull(incomingPaths, "incomingPaths"));
    this.outgoingPaths = setWithoutNullValues(requireNonNull(outgoingPaths, "outgoingPaths"));
    this.attachedLinks = setWithoutNullValues(requireNonNull(attachedLinks, "attachedLinks"));
    this.occupyingVehicle = occupyingVehicle;
  }

  /**
   * Returns the physical coordinates of this point in mm.
   *
   * @return The physical coordinates of this point in mm.
   */
  public Triple getPosition() {
    return position;
  }

  /**
   * Returns a vehicle's orientation angle at this position.
   * (-360..360, or <code>Double.NaN</code>, if an orientation angle is not
   * specified for this point.)
   *
   * @return The vehicle's orientation angle when it's at this position.
   */
  public double getVehicleOrientationAngle() {
    return vehicleOrientationAngle;
  }

  /**
   * Returns this point's type.
   *
   * @return This point's type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Checks whether parking a vehicle on this point is allowed.
   * <p>
   * This method is a convenience method; its return value is equal to
   * <code>getType().equals(Point.Type.PARK_POSITION)</code>.
   * </p>
   *
   * @return <code>true</code> if, and only if, parking is allowed on this
   * point.
   */
  public boolean isParkingPosition() {
    return type.equals(Type.PARK_POSITION);
  }

  /**
   * Checks whether halting on this point is allowed.
   * <p>
   * This method is a convenience method; its return value is equal to
   * <code>getType().equals(Point.Type.PARK_POSITION) ||
   * getType().equals(Point.Type.HALT_POSITION)</code>.
   * </p>
   *
   * @return <code>true</code> if, and only if, halting is allowed on this
   * point.
   */
  public boolean isHaltingPosition() {
    return type.equals(Type.PARK_POSITION) || type.equals(Type.HALT_POSITION);
  }

  /**
   * Returns a reference to the vehicle occupying this point.
   *
   * @return A reference to the vehicle occupying this point, or
   * <code>null</code>, if this point isn't currently occupied by any vehicle.
   */
  public TCSObjectReference<Vehicle> getOccupyingVehicle() {
    return occupyingVehicle;
  }

  /**
   * Returns a set of references to paths ending in this point.
   *
   * @return A set of references to paths ending in this point.
   */
  public Set<TCSObjectReference<Path>> getIncomingPaths() {
    return Collections.unmodifiableSet(incomingPaths);
  }

  /**
   * Returns a set of references to paths originating in this point.
   *
   * @return A set of references to paths originating in this point.
   */
  public Set<TCSObjectReference<Path>> getOutgoingPaths() {
    return Collections.unmodifiableSet(outgoingPaths);
  }

  /**
   * Returns a set of links attached to this point.
   *
   * @return A set of links attached to this point.
   */
  public Set<Location.Link> getAttachedLinks() {
    return Collections.unmodifiableSet(attachedLinks);
  }


  /**
   * Describes the types of positions in a driving course.
   */
  public enum Type {

    /**
     * Indicates a position at which a vehicle is expected to report in.
     * Halting or even parking at such a position is not allowed.
     */
    REPORT_POSITION,
    /**
     * Indicates a position at which a vehicle may halt temporarily, e.g. for executing an
     * operation.
     * The vehicle is also expected to report in when it arrives at such a position.
     * It may not park here for longer than necessary, though.
     */
    HALT_POSITION,
    /**
     * Indicates a position at which a vehicle may halt for longer periods of time when it is not
     * processing orders.
     * The vehicle is also expected to report in when it arrives at such a position.
     */
    PARK_POSITION;
  }
}
