package edu.washington.escience.myria.api.encoding;

import java.util.List;

import edu.washington.escience.myria.api.encoding.QueryConstruct.ConstructArgs;
import edu.washington.escience.myria.operator.JoinMStepPartialNewType;
import edu.washington.escience.myria.operator.RightHashJoin;

/**
 * 
 * Encoding for {@link RightHashJoin}.
 * 
 */
public class JoinMStepPartialNewTypeEncoding extends
		BinaryOperatorEncoding<JoinMStepPartialNewType> {
	public List<String> argColumnNames;
	@Required
	public int[] argColumns1;
	@Required
	public int[] argColumns2;
	@Required
	public int[] argSelect1;
	@Required
	public int[] argSelect2;

	@Override
	public JoinMStepPartialNewType construct(ConstructArgs args) {
		return new JoinMStepPartialNewType(argColumnNames, null, null,
				argColumns1, argColumns2, argSelect1, argSelect2);
	}
}