package edu.washington.escience.myria.sp2bench;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;
import edu.washington.escience.myria.operator.RootOperator;
import edu.washington.escience.myria.operator.StreamingStateWrapper;
import edu.washington.escience.myria.parallel.ExchangePairID;
import edu.washington.escience.myria.parallel.Producer;
import edu.washington.escience.myria.parallel.SingleQueryPlanWithArgs;

public class Erdos4 implements QueryPlanGenerator {

  final static ImmutableList<Type> outputTypes = ImmutableList.of(Type.STRING_TYPE);
  final static ImmutableList<String> outputColumnNames = ImmutableList.of("names");
  final static Schema outputSchema = new Schema(outputTypes, outputColumnNames);

  final ExchangePairID sendToMasterID = ExchangePairID.newID();

  @Override
  public Map<Integer, SingleQueryPlanWithArgs> getWorkerPlan(int[] allWorkers) throws Exception {
    ArrayList<Producer> producers = new ArrayList<Producer>();
    StreamingStateWrapper e4 = Erdos.erdosN(4, allWorkers, producers);
    return Erdos.getWorkerPlan(allWorkers, Erdos.extractName(e4), producers);
  }

  @Override
  public RootOperator getMasterPlan(int[] allWorkers) throws Exception {
    return Erdos.getMasterPlan(allWorkers);
  }

}
