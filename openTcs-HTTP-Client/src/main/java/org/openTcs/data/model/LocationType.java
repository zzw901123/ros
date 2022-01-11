/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.openTcs.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import org.openTcs.data.ObjectHistory;
import org.openTcs.data.TCSObject;

/**
 * Describes the type of a {@link Location}.
 *
 * @author Stefan Walter (Fraunhofer IML)
 */
public class LocationType
    extends TCSObject<LocationType>
    implements Serializable {

  /**
   * The operations allowed at locations of this type.
   */
  private final List<String> allowedOperations;
  /**
   * The peripheral operations allowed at locations of this type.
   */
  private final List<String> allowedPeripheralOperations;

  /**
   * Creates a new LocationType.
   *
   * @param name The new location type's name.
   */
  public LocationType(String name) {
    super(name);
    this.allowedOperations = new ArrayList<>();
    this.allowedPeripheralOperations = new ArrayList<>();
  }

  private LocationType(String name,
                       Map<String, String> properties,
                       ObjectHistory history,
                       List<String> allowedOperations,
                       List<String> allowedPeripheralOperations) {
    super(name, properties, history);
    this.allowedOperations = listWithoutNullValues(requireNonNull(allowedOperations,
                                                                  "allowedOperations"));
    this.allowedPeripheralOperations
        = listWithoutNullValues(requireNonNull(allowedPeripheralOperations,
                                               "allowedPeripheralOperations"));
  }

  /**
   * Returns a set of operations allowed with locations of this type.
   *
   * @return A set of operations allowed with locations of this type.
   */
  public List<String> getAllowedOperations() {
    return Collections.unmodifiableList(allowedOperations);
  }

  /**
   * Checks if a given operation is allowed with locations of this type.
   *
   * @param operation The operation to be checked for.
   * @return <code>true</code> if, and only if, the given operation is allowed
   * with locations of this type.
   */
  public boolean isAllowedOperation(String operation) {
    requireNonNull(operation, "operation");
    return allowedOperations.contains(operation);
  }

  /**
   * Returns a set of peripheral operations allowed with locations of this type.
   *
   * @return A set of peripheral operations allowed with locations of this type.
   */
  public List<String> getAllowedPeripheralOperations() {
    return Collections.unmodifiableList(allowedPeripheralOperations);
  }

}
