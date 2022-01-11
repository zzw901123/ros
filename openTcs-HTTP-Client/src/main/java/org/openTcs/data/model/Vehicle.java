/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.openTcs.data.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import org.openTcs.data.ObjectHistory;
import org.openTcs.data.TCSObject;
import org.openTcs.data.TCSObjectReference;
import org.openTcs.data.order.OrderConstants;
import org.openTcs.data.order.OrderSequence;
import org.openTcs.data.order.TransportOrder;
import static org.openTcs.util.Assertions.checkArgument;
import static org.openTcs.util.Assertions.checkInRange;

/**
 * Describes a vehicle's current state.
 *
 * @author Stefan Walter (Fraunhofer IML)
 */
public class Vehicle
    extends TCSObject<Vehicle>
    implements Serializable {

  /**
   * A value indicating that no route steps have been travelled for the current drive order, yet.
   */
  public static final int ROUTE_INDEX_DEFAULT = -1;
  /**
   * The key for a property to store the class name of the preferred communication adapter (factory)
   * for this vehicle.
   */
  public static final String PREFERRED_ADAPTER = "tcs:preferredAdapterClass";
  /**
   * This vehicle's length (in mm).
   */
  private final int length;
  /**
   * The value at/above which the vehicle's energy level is considered "good".
   */
  private final int energyLevelGood;
  /**
   * The value at/below which the vehicle's energy level is considered "critical".
   */
  private final int energyLevelCritical;
  /**
   * The value at/above which the vehicle's energy level is considered fully recharged.
   */
  private final int energyLevelFullyRecharged;
  /**
   * The value at/above which the vehicle's energy level is considered sufficiently recharged.
   */
  private final int energyLevelSufficientlyRecharged;
  /**
   * This vehicle's remaining energy (in percent of the maximum).
   */
  private final int energyLevel;
  /**
   * This vehicle's maximum velocity (in mm/s).
   */
  private final int maxVelocity;
  /**
   * This vehicle's maximum reverse velocity (in mm/s).
   */
  private final int maxReverseVelocity;
  /**
   * The operation the vehicle's current communication adapter accepts as a command to recharge the
   * vehicle.
   */
  private final String rechargeOperation;
  /**
   * This vehicle's current state.
   */
  private final State state;
  /**
   * This vehicle's current processing state.
   */
  private final ProcState procState;
  /**
   * This vehicle's integration level.
   */
  private final IntegrationLevel integrationLevel;
  /**
   * A reference to the transport order this vehicle is currently processing.
   */
  private final TCSObjectReference<TransportOrder> transportOrder;
  /**
   * A reference to the order sequence this vehicle is currently processing.
   */
  private final TCSObjectReference<OrderSequence> orderSequence;
  /**
   * The set of transport order types this vehicle is allowed to process.
   */
  private final Set<String> allowedOrderTypes;
  /**
   * The index of the last route step travelled for the current drive order of the current transport
   * order.
   */
  private final int routeProgressIndex;
  /**
   * A reference to the point which this vehicle currently occupies.
   */
  private final TCSObjectReference<Point> currentPosition;
  /**
   * A reference to the point which this vehicle is expected to be seen at next.
   */
  private final TCSObjectReference<Point> nextPosition;
  /**
   * The vehicle's precise position in world coordinates [mm], independent from logical
   * positions/point names.
   * Set to <code>null</code> if the vehicle hasn't provided a precise position.
   */
  private final Triple precisePosition;
  /**
   * The vehicle's current orientation angle (-360..360).
   * Set to <code>Double.NaN</code> if the vehicle hasn't provided an orientation angle.
   */
  private final double orientationAngle;

  /**
   * Creates a new vehicle.
   *
   * @param name The new vehicle's name.
   */
  public Vehicle(String name) {
    super(name);
    this.length = 1000;
    this.energyLevelGood = 90;
    this.energyLevelCritical = 30;
    this.energyLevelFullyRecharged = 90;
    this.energyLevelSufficientlyRecharged = 30;
    this.maxVelocity = 1000;
    this.maxReverseVelocity = 1000;
    this.rechargeOperation = "CHARGE";
    this.procState = ProcState.IDLE;
    this.transportOrder = null;
    this.orderSequence = null;
    this.allowedOrderTypes = new HashSet<>(Arrays.asList(OrderConstants.TYPE_ANY));
    this.routeProgressIndex = ROUTE_INDEX_DEFAULT;
    this.state = State.UNKNOWN;
    this.integrationLevel = IntegrationLevel.TO_BE_RESPECTED;
    this.currentPosition = null;
    this.nextPosition = null;
    this.precisePosition = null;
    this.orientationAngle = Double.NaN;
    this.energyLevel = 100;
  }

  private Vehicle(String name,
                  Map<String, String> properties,
                  ObjectHistory history,
                  int length,
                  int energyLevelGood,
                  int energyLevelCritical,
                  int energyLevelFullyRecharged,
                  int energyLevelSufficientlyRecharged,
                  int maxVelocity,
                  int maxReverseVelocity,
                  String rechargeOperation,
                  ProcState procState,
                  TCSObjectReference<TransportOrder> transportOrder,
                  TCSObjectReference<OrderSequence> orderSequence,
                  Set<String> allowedOrderTypes,
                  int routeProgressIndex,
                  State state,
                  IntegrationLevel integrationLevel,
                  TCSObjectReference<Point> currentPosition,
                  TCSObjectReference<Point> nextPosition,
                  Triple precisePosition,
                  double orientationAngle,
                  int energyLevel) {
    super(name, properties, history);
    this.length = checkInRange(length, 1, Integer.MAX_VALUE, "length");
    this.energyLevelGood = checkInRange(energyLevelGood, 0, 100, "energyLevelGood");
    this.energyLevelCritical = checkInRange(energyLevelCritical, 0, 100, "energyLevelCritical");
    this.energyLevelFullyRecharged = checkInRange(energyLevelFullyRecharged,
                                                  0,
                                                  100,
                                                  "energyLevelFullyRecharged");
    this.energyLevelSufficientlyRecharged = checkInRange(energyLevelSufficientlyRecharged,
                                                         0,
                                                         100,
                                                         "energyLevelSufficientlyRecharged");
    this.maxVelocity = checkInRange(maxVelocity, 0, Integer.MAX_VALUE, "maxVelocity");
    this.maxReverseVelocity = checkInRange(maxReverseVelocity,
                                           0,
                                           Integer.MAX_VALUE,
                                           "maxReverseVelocity");
    this.rechargeOperation = requireNonNull(rechargeOperation, "rechargeOperation");
    this.procState = requireNonNull(procState, "procState");
    this.transportOrder = transportOrder;
    this.orderSequence = orderSequence;
    this.allowedOrderTypes = requireNonNull(allowedOrderTypes, "allowedOrderTypes");
    this.routeProgressIndex = routeProgressIndex;
    this.state = requireNonNull(state, "state");
    this.integrationLevel = requireNonNull(integrationLevel, "integrationLevel");
    this.currentPosition = currentPosition;
    this.nextPosition = nextPosition;
    this.precisePosition = precisePosition;
    checkArgument(Double.isNaN(orientationAngle)
        || (orientationAngle >= -360.0 && orientationAngle <= 360.0),
                  "Illegal orientation angle: %s",
                  orientationAngle);
    this.orientationAngle = orientationAngle;
    this.energyLevel = checkInRange(energyLevel, 0, 100, "energyLevel");
  }

  /**
   * Returns this vehicle's remaining energy (in percent of the maximum).
   *
   * @return This vehicle's remaining energy.
   */
  public int getEnergyLevel() {
    return energyLevel;
  }

  /**
   * Checks whether the vehicle's energy level is critical.
   *
   * @return <code>true</code> if, and only if, the vehicle's energy level is
   * critical.
   */
  public boolean isEnergyLevelCritical() {
    return energyLevel <= energyLevelCritical;
  }

  /**
   * Checks whether the vehicle's energy level is degraded (not <em>good</em>
   * any more).
   *
   * @return <code>true</code> if, and only if, the vehicle's energy level is
   * degraded.
   */
  public boolean isEnergyLevelDegraded() {
    return energyLevel <= energyLevelGood;
  }

  /**
   * Checks whether the vehicle's energy level is good.
   *
   * @return <code>true</code> if, and only if, the vehicle's energy level is
   * good.
   */
  public boolean isEnergyLevelGood() {
    return energyLevel > energyLevelGood;
  }

  /**
   * Checks whether the vehicle's energy level is fully recharged.
   *
   * @return <code>true</code> if, and only if, the vehicle's energy level is
   * fully recharged.
   */
  public boolean isEnergyLevelFullyRecharged() {
    return energyLevel >= energyLevelFullyRecharged;
  }

  /**
   * Checks whether the vehicle's energy level is sufficiently recharged.
   *
   * @return <code>true</code> if, and only if, the vehicle's energy level is
   * sufficiently recharged.
   */
  public boolean isEnergyLevelSufficientlyRecharged() {
    return energyLevel >= energyLevelSufficientlyRecharged;
  }

  /**
   * Returns this vehicle's critical energy level (in percent of the maximum).
   * The critical energy level is the one at/below which the vehicle should be
   * recharged.
   *
   * @return This vehicle's critical energy level.
   */
  public int getEnergyLevelCritical() {
    return energyLevelCritical;
  }

  /**
   * Returns this vehicle's good energy level (in percent of the maximum).
   * The good energy level is the one at/above which the vehicle can be
   * dispatched again when charging.
   *
   * @return This vehicle's good energy level.
   */
  public int getEnergyLevelGood() {
    return energyLevelGood;
  }

  /**
   * Returns this vehicle's energy level for being fully recharged (in percent of the maximum).
   *
   * @return This vehicle's fully recharged treshold.
   */
  public int getEnergyLevelFullyRecharged() {
    return energyLevelFullyRecharged;
  }

  /**
   * Returns this vehicle's energy level for being sufficiently recharged (in percent of the
   * maximum).
   *
   * @return This vehicle's sufficiently recharged energy level.
   */
  public int getEnergyLevelSufficientlyRecharged() {
    return energyLevelSufficientlyRecharged;
  }

  /**
   * Returns the operation that the vehicle's current communication adapter
   * accepts as a command to recharge the vehicle.
   *
   * @return The operation that the vehicle's current communication adapter
   * accepts as a command to recharge the vehicle.
   */
  public String getRechargeOperation() {
    return rechargeOperation;
  }

  /**
   * Returns this vehicle's maximum velocity (in mm/s).
   *
   * @return This vehicle's maximum velocity (in mm/s).
   */
  public int getMaxVelocity() {
    return maxVelocity;
  }

  /**
   * Returns this vehicle's maximum reverse velocity (in mm/s).
   *
   * @return This vehicle's maximum reverse velocity (in mm/s).
   */
  public int getMaxReverseVelocity() {
    return maxReverseVelocity;
  }


  /**
   * Returns this vehicle's current state.
   *
   * @return This vehicle's current state.
   */
  public State getState() {
    return state;
  }

  /**
   * Checks if this vehicle's current state is equal to the given one.
   *
   * @param otherState The state to compare to this vehicle's one.
   * @return <code>true</code> if, and only if, the given state is equal to this
   * vehicle's one.
   */
  public boolean hasState(State otherState) {
    return state.equals(otherState);
  }

  /**
   * Returns this vehicle's current processing state.
   *
   * @return This vehicle's current processing state.
   */
  public ProcState getProcState() {
    return procState;
  }

  /**
   * Returns this vehicle's integration level.
   *
   * @return This vehicle's integration level.
   */
  public IntegrationLevel getIntegrationLevel() {
    return integrationLevel;
  }

  /**
   * Checks if this vehicle's current processing state is equal to the given
   * one.
   *
   * @param otherState The state to compare to this vehicle's one.
   * @return <code>true</code> if, and only if, the given state is equal to this
   * vehicle's one.
   */
  public boolean hasProcState(ProcState otherState) {
    return procState.equals(otherState);
  }

  /**
   * Returns this vehicle's current length.
   *
   * @return this vehicle's current length.
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns a reference to the transport order this vehicle is currently
   * processing.
   *
   * @return A reference to the transport order this vehicle is currently
   * processing, or <code>null</code>, if it is not processing any transport
   * order at the moment.
   */
  public TCSObjectReference<TransportOrder> getTransportOrder() {
    return transportOrder;
  }

  /**
   * Returns a reference to the order sequence this vehicle is currently
   * processing.
   *
   * @return A reference to the order sequence this vehicle is currently
   * processing, or <code>null</code>, if it is not processing any order
   * sequence at the moment.
   */
  public TCSObjectReference<OrderSequence> getOrderSequence() {
    return orderSequence;
  }

  /**
   * Returns the set of order types this vehicle is allowed to process.
   *
   * @return The set of order types this vehicle is allowed to process.
   */
  public Set<String> getAllowedOrderTypes() {
    return allowedOrderTypes;
  }

  /**
   * Returns the index of the last route step travelled for the current drive
   * order of the current transport order.
   *
   * @return The index of the last route step travelled for the current drive
   * order of the current transport order.
   */
  public int getRouteProgressIndex() {
    return routeProgressIndex;
  }

  /**
   * Returns a reference to the point this vehicle currently occupies.
   *
   * @return A reference to the point this vehicle currently occupies, or
   * <code>null</code>, if this vehicle's position is unknown or the vehicle is
   * currently not in the system.
   */
  public TCSObjectReference<Point> getCurrentPosition() {
    return currentPosition;
  }

  /**
   * Returns a reference to the point this vehicle is expected to be seen at
   * next.
   *
   * @return A reference to the point this vehicle is expected to be seen at
   * next, or <code>null</code>, if this vehicle's next position is unknown.
   */
  public TCSObjectReference<Point> getNextPosition() {
    return nextPosition;
  }

  /**
   * Returns the vehicle's position in world coordinates [mm], independent
   * from logical positions/point names. May be <code>null</code> if the vehicle
   * hasn't provided a precise position.
   *
   * @return The vehicle's precise position in mm.
   */
  public Triple getPrecisePosition() {
    return precisePosition;
  }

  /**
   * Returns the vehicle's current orientation angle (-360..360).
   * May be <code>Double.NaN</code> if the vehicle hasn't provided an
   * orientation angle.
   *
   * @return The vehicle's current orientation angle.
   */
  public double getOrientationAngle() {
    return orientationAngle;
  }
  
  /**
   * Checks if this vehicle is currently processing any transport order.
   *
   * @return <code>true</code> if, and only if, this vehicle is currently
   * processing a transport order.
   */
  public boolean isProcessingOrder() {
    return transportOrder != null;
  }

  @Override
  public String toString() {
    return "Vehicle{"
        + "name=" + getName()
        + ", procState=" + procState
        + ", integrationLevel=" + integrationLevel
        + ", state=" + state
        + ", energyLevel=" + energyLevel
        + ", currentPosition=" + currentPosition
        + ", precisePosition=" + precisePosition
        + ", orientationAngle=" + orientationAngle
        + ", nextPosition=" + nextPosition
        + ", length=" + length
        + ", transportOrder=" + transportOrder
        + ", routeProgressIndex=" + routeProgressIndex
        + ", orderSequence=" + orderSequence
        + ", energyLevelGood=" + energyLevelGood
        + ", energyLevelCritical=" + energyLevelCritical
        + ", energyLevelFullyRecharged=" + energyLevelFullyRecharged
        + ", energyLevelSufficientlyRecharged=" + energyLevelSufficientlyRecharged
        + ", maxVelocity=" + maxVelocity
        + ", maxReverseVelocity=" + maxReverseVelocity
        + ", rechargeOperation=" + rechargeOperation
        + ", allowedOrderTypes=" + allowedOrderTypes
        + '}';
  }

  /**
   * The elements of this enumeration describe the various possible states of a
   * vehicle.
   */
  public enum State {

    /**
     * The vehicle's current state is unknown, e.g. because communication with
     * it is currently not possible for some reason.
     */
    UNKNOWN,
    /**
     * The vehicle's state is known and it's not in an error state, but it is
     * not available for receiving orders.
     */
    UNAVAILABLE,
    /**
     * There is a problem with the vehicle.
     */
    ERROR,
    /**
     * The vehicle is currently idle/available for processing movement orders.
     */
    IDLE,
    /**
     * The vehicle is processing a movement order.
     */
    EXECUTING,
    /**
     * The vehicle is currently recharging its battery/refilling fuel.
     */
    CHARGING
  }

  /**
   * A vehicle's state of integration into the system.
   */
  public static enum IntegrationLevel {

    /**
     * The vehicle's reported position is ignored.
     */
    TO_BE_IGNORED,
    /**
     * The vehicle's reported position is noticed, meaning that resources will not be reserved for
     * it.
     */
    TO_BE_NOTICED,
    /**
     * The vehicle's reported position is respected, meaning that resources will be reserved for it.
     */
    TO_BE_RESPECTED,
    /**
     * The vehicle is fully integrated and may be assigned to transport orders.
     */
    TO_BE_UTILIZED
  }

  /**
   * A vehicle's processing state as seen by the dispatcher.
   */
  public enum ProcState {

    /**
     * The vehicle is currently not processing a transport order.
     */
    IDLE,
    /**
     * The vehicle is currently processing a transport order and is waiting for
     * the next drive order to be assigned to it.
     */
    AWAITING_ORDER,
    /**
     * The vehicle is currently processing a drive order.
     */
    PROCESSING_ORDER
  }

  /**
   * The elements of this enumeration represent the possible orientations of a
   * vehicle.
   */
  public enum Orientation {

    /**
     * Indicates that the vehicle is driving/standing oriented towards its
     * front.
     */
    FORWARD,
    /**
     * Indicates that the vehicle is driving/standing oriented towards its
     * back.
     */
    BACKWARD,
    /**
     * Indicates that the vehicle's orientation is undefined/unknown.
     */
    UNDEFINED
  }

}
