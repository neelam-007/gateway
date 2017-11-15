package com.l7tech.external.assertions.portaldeployer;

import java.util.function.IntSupplier;

/**
 * @author chemi11, 2017-10-26
 *
 * This class will provide increasingly larger integers until the maxInt has been reach,
 * then it will continually return the maxInt
 */
public class ExponentialIntSupplier implements IntSupplier {
  int i = 0;
  int maxInt;
  int multiplier = 1000;

  public ExponentialIntSupplier(int i, int maxInt) {
    this.i = i;
    this.maxInt = maxInt;
  }

  @Override
  public int getAsInt() {
    i++;
    return Math.min(i * i * multiplier, maxInt * multiplier);
  }

  // Reset the integer back to 0
  public void reset() {
    i = 0;
  }
}
