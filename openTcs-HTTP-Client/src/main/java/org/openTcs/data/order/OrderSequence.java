/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.openTcs.data.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import org.openTcs.data.ObjectHistory;
import org.openTcs.data.TCSObject;
import org.openTcs.data.TCSObjectReference;
import org.openTcs.data.model.Vehicle;
import static org.openTcs.util.Assertions.checkArgument;
import static org.openTcs.util.Assertions.checkInRange;

/**
 * Describes a process spanning multiple {@link TransportOrder}s which are to be executed
 * subsequently by the same {@link Vehicle}.
 * <p>
 * The most important rules for order sequence processing are:
 * </p>
 * <ul>
 * <li>Only transport orders that have not yet been activated may be added to an order sequence.
 * Allowing them to be added at a later point of time would imply that, due to concurrency in the
 * kernel, a transport order might happen to be dispatched at the same time or shortly after it is
 * added to a sequence, regardless of if its predecessors in the sequence have already been finished
 * or not.</li>
 * <li>The <em>intendedVehicle</em> of a transport order being added to an order sequence must be
 * the same as that of the sequence itself.
 * If it is <code>null</code> in the sequence, a vehicle that will process all orders in the
 * sequence will be chosen automatically once the first order in the sequence is dispatched.</li>
 * <li>If an order sequence is marked as <em>complete</em> and all transport orders belonging to it
 * have arrived in state <code>FINISHED</code> or <code>FAILED</code>, it will be marked as
 * <em>finished</em> implicitly.</li>
 * <li>If a transport order belonging to an order sequence fails and the sequence's
 * <em>failureFatal</em> flag is set, all subsequent orders in the sequence will automatically be
 * considered (and marked as) failed, too, and the order sequence will implicitly be marked as
 * <em>complete</em> (and <em>finished</em>).</li>
 * </ul>
 *
 * @author Stefan Walter (Fraunhofer IML)
 */
public class OrderSequence
    extends TCSObject<OrderSequence>
    implements Serializable {

  /**
   * The type of this order sequence.
   * An order sequence and all transport orders it contains share the same type.
   */
  @Nonnull
  private final String type;
  /**
   * Transport orders belonging to this sequence that still need to be processed.
   */
  private final List<TCSObjectReference<TransportOrder>> orders;
  /**
   * The index of the order that was last finished in the sequence.
   * -1 if none was finished, yet.
   */
  private final int finishedIndex;
  /**
   * Indicates whether this order sequence is complete and will not be extended by more orders.
   */
  private final boolean complete;
  /**
   * Indicates whether this order sequence has been processed completely.
   */
  private final boolean finished;
  /**
   * Indicates whether the failure of one order in this sequence is fatal to all subsequent orders.
   */
  private final boolean failureFatal;
  /**
   * The vehicle that is intended to process this order sequence.
   * If this sequence is free to be processed by any vehicle, this is <code>null</code>.
   */
  private final TCSObjectReference<Vehicle> intendedVehicle;
  /**
   * The vehicle processing this order sequence, or <code>null</code>, if no vehicle has been
   * assigned to it, yet.
   */
  private final TCSObjectReference<Vehicle> processingVehicle;

  /**
   * Creates a new OrderSequence.
   *
   * @param name This sequence's name.
   */
  public OrderSequence(String name) {
    super(name);
    this.type = OrderConstants.TYPE_NONE;
    this.orders = new ArrayList<>();
    this.finishedIndex = -1;
    this.complete = false;
    this.finished = false;
    this.failureFatal = false;
    this.intendedVehicle = null;
    this.processingVehicle = null;
  }

  private OrderSequence(String name,
                        Map<String, String> properties,
                        ObjectHistory history,
                        String type,
                        TCSObjectReference<Vehicle> intendedVehicle,
                        List<TCSObjectReference<TransportOrder>> orders,
                        int finishedIndex,
                        boolean complete,
                        boolean failureFatal,
                        boolean finished,
                        TCSObjectReference<Vehicle> processingVehicle) {
    super(name, properties, history);
    this.type = requireNonNull(type, "type");
    this.intendedVehicle = intendedVehicle;
    this.orders = new ArrayList<>(requireNonNull(orders, "orders"));
    this.finishedIndex = finishedIndex;
    this.complete = complete;
    this.failureFatal = failureFatal;
    this.finished = finished;
    this.processingVehicle = processingVehicle;
  }

  /**
   * Returns this order sequence's type.
   *
   * @return This order sequence's type.
   */
  @Nonnull
  public String getType() {
    return type;
  }

  /**
   * Returns the list of orders making up this sequence.
   *
   * @return The list of orders making up this sequence.
   */
  public List<TCSObjectReference<TransportOrder>> getOrders() {
    return Collections.unmodifiableList(orders);
  }

  /**
   * Returns the next order in the sequence that hasn't been finished, yet.
   *
   * @return <code>null</code> if this sequence has been finished already or
   * currently doesn't have any unfinished orders, else the order after the one
   * that was last finished.
   */
  public TCSObjectReference<TransportOrder> getNextUnfinishedOrder() {
    // If the whole sequence has been finished already, return null.
    if (finished) {
      return null;
    }
    // If the sequence has not been marked as finished but the last order in the
    // list has been, return null, too.
    else if (finishedIndex + 1 >= orders.size()) {
      return null;
    }
    // Otherwise just get the order after the one that was last finished.
    else {
      return orders.get(finishedIndex + 1);
    }
  }

  /**
   * Returns the index of the order that was last finished in the sequence, or
   * -1, if none was finished, yet.
   *
   * @return the index of the order that was last finished in the sequence.
   */
  public int getFinishedIndex() {
    return finishedIndex;
  }



  /**
   * Indicates whether this order sequence is complete and will not be extended
   * by more orders.
   *
   * @return <code>true</code> if, and only if, this order sequence is complete
   * and will not be extended by more orders.
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Indicates whether this order sequence has been processed completely.
   * (Note that <em>processed completely</em> does not necessarily mean
   * <em>finished successfully</em>; it is possible that one or more transport
   * orders belonging to this sequence have failed.)
   *
   * @return <code>true</code> if, and only if, this order sequence has been
   * processed completely.
   */
  public boolean isFinished() {
    return finished;
  }

  /**
   * Indicates whether the failure of a single order in this sequence implies
   * that all subsequent orders in this sequence are to be considered failed,
   * too.
   *
   * @return <code>true</code> if, and only if, the failure of an order in this
   * sequence implies the failure of all subsequent orders.
   */
  public boolean isFailureFatal() {
    return failureFatal;
  }

  /**
   * Returns a reference to the vehicle that is intended to process this
   * order sequence.
   *
   * @return A reference to the vehicle that is intended to process this
   * order sequence. If this sequence is free to be processed by any vehicle,
   * <code>null</code> is returned.
   */
  public TCSObjectReference<Vehicle> getIntendedVehicle() {
    return intendedVehicle;
  }

  /**
   * Returns a reference to the vehicle currently processing this sequence.
   *
   * @return A reference to the vehicle currently processing this sequence. If
   * this sequence has not been processed, yet, <code>null</code> is
   * returned.
   */
  public TCSObjectReference<Vehicle> getProcessingVehicle() {
    return processingVehicle;
  }

  private List<TCSObjectReference<TransportOrder>> ordersWithAppended(
      @Nonnull TCSObjectReference<TransportOrder> order) {
    List<TCSObjectReference<TransportOrder>> result = new ArrayList<>(orders.size() + 1);
    result.addAll(orders);
    result.add(order);
    return result;
  }
}
