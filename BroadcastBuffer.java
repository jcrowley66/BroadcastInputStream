package org.apache.commons.io;

/** LICENSE - this is provided under the terms of the Apache 2.0 license - see https://spdx.org/licenses/Apache-2.0.html */

/** One data buffer used in the BroadcastInputStream process. */
class BroadcastBuffer {
  volatile BroadcastBuffer next = null; // Volatile since multiple consumers will follow this chain and
                                        // check for non-null to see if another buffer available
  int[]              data;
  int                amtPresent = 0;    // Amount of data in 'data' array (may be less than max allowed)
  int                numOpen;           // Counted down, when hits zero buffer may be released & reused
  // Constructor - package visible
  BroadcastBuffer(int dataSize) {
    data = new int[dataSize];
  }
}
