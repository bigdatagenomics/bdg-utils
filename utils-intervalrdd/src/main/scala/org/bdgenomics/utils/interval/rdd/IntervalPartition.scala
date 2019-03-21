/**
 * Licensed to Big Data Genomics (BDG) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The BDG licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bdgenomics.utils.interval.rdd

import org.bdgenomics.utils.interval.array._
import scala.math.max
import scala.reflect.ClassTag

protected class IntervalPartition[K <: Interval[K]: ClassTag, V: ClassTag](protected val array: IntervalArray[K, V])
    extends Serializable {

  /**
   * Fetches IntervalArray
   *
   * @return IntervalArray stored in IntervalPartition
   */
  def getIntervalArray(): IntervalArray[K, V] = {
    array
  }

  /**
   * Generates a new IntervalPartition from an IntervalArray.
   *
   * @param map IntervalArray to store in IntervalPartition
   * @return new IntervalPartition with underlying map structure
   */
  protected def withMap(map: IntervalArray[K, V]): IntervalPartition[K, V] = {
    new IntervalPartition(map)
  }

  /**
   * Gets all (k,v) data from partition within the specificed interval
   *
   * @return Iterator of searched interval and the corresponding (K,V) pairs
   */
  def get(r: K): Iterable[(K, V)] = {
    array.get(r)
  }

  /**
   * Gets all (k,v) data from partition
   *
   * @return Iterator of searched interval and the corresponding (K,V) pairs
   */
  def get(): Iterable[(K, V)] = {
    array.collect.toIterable
  }

  /**
   * Filters underlying IntervalArray by an Interval and returns new IntervalPartition with filtered result.
   *
   * @param r Interval to filter by
   * @return new IntervalPartition with filtered IntervalArray
   */
  private[utils] def filterByInterval(r: K): IntervalPartition[K, V] = {
    IntervalPartition(array.get(r), sorted = true)
  }

  /**
   * Return a new IntervalPartition filtered by some predicate.
   *
   * @param pred: converts (K,V) to boolean
   * @return
   */
  def filter(pred: ((K, V)) => Boolean): IntervalPartition[K, V] = {
    this.withMap(array.filter(pred))
  }

  /**
   * Applies a map function over the interval tree.
   *
   * @param f Function converting type V to V2
   * @return new IntervalPartition of type (k, V2)
   */
  def mapValues[V2: ClassTag](f: V => V2): IntervalPartition[K, V2] = {
    val retTree: IntervalArray[K, V2] = array.mapValues(f)
    new IntervalPartition(retTree)
  }

  /**
   * Puts all (k,v) data from partition within the specificed interval.
   *
   * @param kvs Iterator of (K, V) pairs
   * @return IntervalPartition with new data
   */
  def multiput(kvs: Iterator[(K, V)]): IntervalPartition[K, V] = {
    this.withMap(array.insert(kvs))
  }

  /**
   * Puts all (k,v) data from partition within the specificed interval.
   *
   * @param kv: Single element of (K, V) pair
   * @return IntervalPartition with new data
   */
  def put(kv: (K, V)): IntervalPartition[K, V] = {
    multiput(Iterator(kv))
  }

  /**
   * Merges trees of this partition with a specified partition.
   *
   * @param p IntervalPartition of (K, V)
   * @return Iterator of searched interval and the corresponding (K,V) pairs
   */
  def mergePartitions(p: IntervalPartition[K, V]): IntervalPartition[K, V] = {
    this.withMap(array.insert(p.getIntervalArray().collect.toIterator))
  }

}

private[rdd] object IntervalPartition {

  /**
   * Generates a new IntervalPartition.
   *
   * @param iter Iterable of data, where each record is keyed by an Interval
   * @param sorted Determines whether iter has been sorted
   * @tparam K
   * @tparam K2
   * @tparam V
   * @return Sorted IntervalPartition
   */
  def apply[K <: Interval[K]: ClassTag, K2 <: Interval[K2]: ClassTag, V: ClassTag](iter: Iterable[(K, V)], sorted: Boolean): IntervalPartition[K, V] = {
    val array = iter.toArray
    val map = IntervalArray(array, array.map(_._1.width).fold(0L)(max(_, _)), sorted = sorted)
    new IntervalPartition(map)
  }
}
