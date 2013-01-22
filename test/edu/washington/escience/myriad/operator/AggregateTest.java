package edu.washington.escience.myriad.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.column.Column;
import edu.washington.escience.myriad.operator.agg.Aggregate;
import edu.washington.escience.myriad.operator.agg.Aggregator;
import edu.washington.escience.myriad.operator.agg.SingleGroupByAggregate;
import edu.washington.escience.myriad.systemtest.SystemTestBase;
import edu.washington.escience.myriad.util.TestUtils;

public class AggregateTest {

  public static HashMap<SystemTestBase.Tuple, Integer> groupByAvgLongColumn(final TupleBatchBuffer source,
      final int groupByColumn, final int aggColumn) {
    final List<List<Column<?>>> tbs = source.getAllAsRawColumn();
    final HashMap<Object, Long> sum = new HashMap<Object, Long>();
    final HashMap<Object, Integer> count = new HashMap<Object, Integer>();
    for (final List<Column<?>> rawData : tbs) {
      final int numTuples = rawData.get(0).size();
      for (int i = 0; i < numTuples; i++) {
        final Object groupByValue = rawData.get(groupByColumn).get(i);
        final Long aggValue = (Long) rawData.get(aggColumn).get(i);
        Long currentSum = sum.get(groupByValue);
        if (currentSum == null) {
          currentSum = 0l;
          count.put(groupByValue, 1);
        } else {
          count.put(groupByValue, count.get(groupByValue) + 1);
        }
        sum.put(groupByValue, currentSum + aggValue);
      }
    }
    final HashMap<SystemTestBase.Tuple, Integer> result = new HashMap<SystemTestBase.Tuple, Integer>();

    for (final Map.Entry<Object, Long> e : sum.entrySet()) {
      final Object gValue = e.getKey();
      final Long sumV = e.getValue();
      final SystemTestBase.Tuple t = new SystemTestBase.Tuple(2);
      t.set(0, (Comparable<?>) gValue);
      t.set(1, sumV * 1.0 / count.get(gValue));
      result.put(t, 1);
    }
    return result;
  }

  public static HashMap<SystemTestBase.Tuple, Long> groupByCount(final TupleBatchBuffer source, final int groupByColumn) {
    final List<List<Column<?>>> tbs = source.getAllAsRawColumn();
    final HashMap<Object, Long> count = new HashMap<Object, Long>();
    for (final List<Column<?>> rawData : tbs) {
      final int numTuples = rawData.get(0).size();
      for (int i = 0; i < numTuples; i++) {
        final Object groupByValue = rawData.get(groupByColumn).get(i);
        Long currentCount = count.get(groupByValue);
        if (currentCount == null) {
          currentCount = 0l;
        }
        count.put(groupByValue, currentCount++);
      }
    }
    final HashMap<SystemTestBase.Tuple, Long> result = new HashMap<SystemTestBase.Tuple, Long>();

    for (final Map.Entry<Object, Long> e : count.entrySet()) {
      final Object gValue = e.getKey();
      final Long countV = e.getValue();
      final SystemTestBase.Tuple t = new SystemTestBase.Tuple(2);
      t.set(0, (Comparable<?>) gValue);
      t.set(1, countV);
      result.put(t, 1l);
    }
    return result;
  }

  public static <T extends Comparable<T>> HashMap<SystemTestBase.Tuple, Integer> groupByMax(
      final TupleBatchBuffer source, final int groupByColumn, final int aggColumn) {
    final List<List<Column<?>>> tbs = source.getAllAsRawColumn();
    final HashMap<Object, T> max = new HashMap<Object, T>();
    for (final List<Column<?>> rawData : tbs) {
      final int numTuples = rawData.get(0).size();
      for (int i = 0; i < numTuples; i++) {
        final Object groupByValue = rawData.get(groupByColumn).get(i);
        @SuppressWarnings("unchecked")
        final T aggValue = ((Column<T>) rawData.get(aggColumn)).get(i);
        final T currentMax = max.get(groupByValue);
        if (currentMax == null) {
          max.put(groupByValue, aggValue);
        } else if (aggValue.compareTo(currentMax) > 0) {
          max.put(groupByValue, aggValue);
        }
      }
    }
    final HashMap<SystemTestBase.Tuple, Integer> result = new HashMap<SystemTestBase.Tuple, Integer>();

    for (final Map.Entry<Object, T> e : max.entrySet()) {
      final Object gValue = e.getKey();
      final T maxV = e.getValue();
      final SystemTestBase.Tuple t = new SystemTestBase.Tuple(2);
      t.set(0, (Comparable<?>) gValue);
      t.set(1, maxV);
      result.put(t, 1);
    }
    return result;
  }

  public static <T extends Comparable<T>> HashMap<SystemTestBase.Tuple, Integer> groupByMin(
      final TupleBatchBuffer source, final int groupByColumn, final int aggColumn) {
    final List<List<Column<?>>> tbs = source.getAllAsRawColumn();
    final HashMap<Object, T> min = new HashMap<Object, T>();
    for (final List<Column<?>> rawData : tbs) {
      final int numTuples = rawData.get(0).size();
      for (int i = 0; i < numTuples; i++) {
        final Object groupByValue = rawData.get(groupByColumn).get(i);
        @SuppressWarnings("unchecked")
        final T aggValue = ((Column<T>) rawData.get(aggColumn)).get(i);
        final T currentMin = min.get(groupByValue);
        if (currentMin == null) {
          min.put(groupByValue, aggValue);
        } else if (aggValue.compareTo(currentMin) < 0) {
          min.put(groupByValue, aggValue);
        }
      }
    }
    final HashMap<SystemTestBase.Tuple, Integer> result = new HashMap<SystemTestBase.Tuple, Integer>();

    for (final Map.Entry<Object, T> e : min.entrySet()) {
      final Object gValue = e.getKey();
      final T minV = e.getValue();
      final SystemTestBase.Tuple t = new SystemTestBase.Tuple(2);
      t.set(0, (Comparable<?>) gValue);
      t.set(1, minV);
      result.put(t, 1);
    }
    return result;
  }

  public static HashMap<SystemTestBase.Tuple, Integer> groupBySumLongColumn(final TupleBatchBuffer source,
      final int groupByColumn, final int aggColumn) {
    final List<List<Column<?>>> tbs = source.getAllAsRawColumn();
    final HashMap<Object, Long> sum = new HashMap<Object, Long>();
    for (final List<Column<?>> rawData : tbs) {
      final int numTuples = rawData.get(0).size();
      for (int i = 0; i < numTuples; i++) {
        final Object groupByValue = rawData.get(groupByColumn).get(i);
        final Long aggValue = (Long) rawData.get(aggColumn).get(i);
        Long currentSum = sum.get(groupByValue);
        if (currentSum == null) {
          currentSum = 0l;
        }
        sum.put(groupByValue, currentSum + aggValue);
      }
    }
    final HashMap<SystemTestBase.Tuple, Integer> result = new HashMap<SystemTestBase.Tuple, Integer>();

    for (final Map.Entry<Object, Long> e : sum.entrySet()) {
      final Object gValue = e.getKey();
      final Long sumV = e.getValue();
      final SystemTestBase.Tuple t = new SystemTestBase.Tuple(2);
      t.set(0, (Comparable<?>) gValue);
      t.set(1, sumV);
      result.put(t, 1);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> T max(final TupleBatchBuffer tbb, final int column) {
    final List<List<Column<?>>> tbs = tbb.getAllAsRawColumn();
    T max = ((Column<T>) tbs.get(0).get(column)).get(0);
    for (final List<Column<?>> tb : tbs) {
      final int numTuples = tb.get(0).size();
      final Column<T> c = (Column<T>) tb.get(column);
      for (int i = 0; i < numTuples; i++) {
        final T current = c.get(i);
        if (max.compareTo(current) < 0) {
          max = current;
        }
      }
    }
    return max;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> T min(final TupleBatchBuffer tbb, final int column) {
    final List<List<Column<?>>> tbs = tbb.getAllAsRawColumn();
    T min = ((Column<T>) tbs.get(0).get(column)).get(0);
    for (final List<Column<?>> tb : tbs) {
      final int numTuples = tb.get(0).size();
      final Column<T> c = (Column<T>) tb.get(column);
      for (int i = 0; i < numTuples; i++) {
        final T current = c.get(i);
        if (min.compareTo(current) > 0) {
          min = current;
        }
      }
    }
    return min;
  }

  public static long sumLong(final TupleBatchBuffer tbb, final int column) {
    final List<List<Column<?>>> tbs = tbb.getAllAsRawColumn();
    long sum = 0;
    for (final List<Column<?>> tb : tbs) {
      final int numTuples = tb.get(0).size();
      @SuppressWarnings("unchecked")
      final Column<Long> c = (Column<Long>) tb.get(column);
      for (int i = 0; i < numTuples; i++) {
        final Long current = c.get(i);
        sum += current;
      }
    }
    return sum;
  }

  public TupleBatchBuffer generateRandomTuples(final int numTuples) {
    final String[] names = TestUtils.randomFixedLengthNumericString(1000, 1005, numTuples, 20);
    final long[] ids = TestUtils.randomLong(1000, 1005, names.length);

    final Schema schema =
        new Schema(ImmutableList.of(Type.LONG_TYPE, Type.STRING_TYPE), ImmutableList.of("id", "name"));

    final TupleBatchBuffer tbb = new TupleBatchBuffer(schema);
    for (int i = 0; i < names.length; i++) {
      tbb.put(0, ids[i]);
      tbb.put(1, names[i]);
    }
    return tbb;
  }

  @Test
  public void testNoGroupAvg() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    final Long sumID = sumLong(testBase, 0);
    final Aggregate agg =
        new Aggregate(new TupleSource(testBase), new int[] { 0 }, new int[] { Aggregator.AGG_OP_AVG });
    agg.open();
    TupleBatch tb = null;
    while ((tb = agg.next()) != null) {
      assertTrue(Double.compare(sumID * 1.0 / numTuples, tb.getDouble(0, 0)) == 0);
    }
    agg.close();
  }

  @Test
  public void testNoGroupCount() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    final Aggregate agg =
        new Aggregate(new TupleSource(testBase), new int[] { 0 }, new int[] { Aggregator.AGG_OP_COUNT });
    agg.open();
    TupleBatch tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(numTuples, tb.getLong(0, 0));
    }
  }

  @Test
  public void testNoGroupMax() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    final Long maxID = max(testBase, 0);
    final String maxName = max(testBase, 1);
    Aggregate agg = new Aggregate(new TupleSource(testBase), new int[] { 0 }, new int[] { Aggregator.AGG_OP_MAX });
    agg.open();
    TupleBatch tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(maxID, tb.getObject(0, 0));
    }
    agg.close();

    agg = new Aggregate(new TupleSource(testBase), new int[] { 1 }, new int[] { Aggregator.AGG_OP_MAX });
    agg.open();
    tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(maxName, tb.getString(0, 0));
    }
  }

  @Test
  public void testNoGroupMin() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    final Long minID = min(testBase, 0);
    final String minName = min(testBase, 1);

    Aggregate agg = new Aggregate(new TupleSource(testBase), new int[] { 0 }, new int[] { Aggregator.AGG_OP_MIN });
    agg.open();
    TupleBatch tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(minID, tb.getObject(0, 0));
    }
    agg.close();

    agg = new Aggregate(new TupleSource(testBase), new int[] { 1 }, new int[] { Aggregator.AGG_OP_MIN });
    agg.open();
    tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(minName, tb.getString(0, 0));
    }
  }

  @Test
  public void testNoGroupSum() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    final Long sumID = sumLong(testBase, 0);
    final Aggregate agg =
        new Aggregate(new TupleSource(testBase), new int[] { 0 }, new int[] { Aggregator.AGG_OP_SUM });
    agg.open();
    TupleBatch tb = null;
    while ((tb = agg.next()) != null) {
      assertEquals(sumID, tb.getObject(0, 0));
    }
    agg.close();
  }

  @Test
  public void testSingleGroupAvg() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    // group by name, aggregate on id
    final SingleGroupByAggregate agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 0 }, 1, new int[] { Aggregator.AGG_OP_AVG });
    agg.open();
    TupleBatch tb = null;
    final TupleBatchBuffer result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    final HashMap<SystemTestBase.Tuple, Integer> actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupByAvgLongColumn(testBase, 1, 0), actualResult);
  }

  @Test
  public void testSingleGroupMax() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    // group by name, aggregate on id
    SingleGroupByAggregate agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 0 }, 1, new int[] { Aggregator.AGG_OP_MAX });
    agg.open();
    TupleBatch tb = null;
    TupleBatchBuffer result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    HashMap<SystemTestBase.Tuple, Integer> actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupByMax(testBase, 1, 0), actualResult);

    agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 1 }, 0, new int[] { Aggregator.AGG_OP_MAX });
    agg.open();
    tb = null;
    result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupByMax(testBase, 0, 1), actualResult);
  }

  @Test
  public void testSingleGroupMin() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    // group by name, aggregate on id
    SingleGroupByAggregate agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 0 }, 1, new int[] { Aggregator.AGG_OP_MIN });
    agg.open();
    TupleBatch tb = null;
    TupleBatchBuffer result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    HashMap<SystemTestBase.Tuple, Integer> actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupByMin(testBase, 1, 0), actualResult);

    agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 1 }, 0, new int[] { Aggregator.AGG_OP_MIN });
    agg.open();
    tb = null;
    result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupByMin(testBase, 0, 1), actualResult);
  }

  @Test
  public void testSingleGroupSum() throws DbException {
    final int maxValue = 200000;
    final int numTuples = (int) (Math.random() * maxValue);

    final TupleBatchBuffer testBase = generateRandomTuples(numTuples);
    // group by name, aggregate on id
    final SingleGroupByAggregate agg =
        new SingleGroupByAggregate(new TupleSource(testBase), new int[] { 0 }, 1, new int[] { Aggregator.AGG_OP_SUM });
    agg.open();
    TupleBatch tb = null;
    final TupleBatchBuffer result = new TupleBatchBuffer(agg.getSchema());
    while ((tb = agg.next()) != null) {
      tb.compactInto(result);
    }
    agg.close();
    final HashMap<SystemTestBase.Tuple, Integer> actualResult = TestUtils.tupleBatchToTupleBag(result);
    TestUtils.assertTupleBagEqual(groupBySumLongColumn(testBase, 1, 0), actualResult);
  }
}