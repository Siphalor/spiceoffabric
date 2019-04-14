package de.siphalor.spiceoffabric.config;

import de.siphalor.tweed.config.constraints.Constraint;
import de.siphalor.tweed.config.constraints.ConstraintException;
import de.siphalor.tweed.config.entry.AbstractValueEntry;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.hjson.JsonValue;

public class ExpressionConstraint implements Constraint<String> {
	private String[] variables;
	private Function[] functions;

	public ExpressionConstraint(String[] variables, Function... functions) {
		this.variables = variables;
		this.functions = functions;
	}

	@Override
	public void apply(JsonValue jsonValue, AbstractValueEntry<String, ?> configEntry) throws ConstraintException {
		try {
			if (!(new ExpressionBuilder(configEntry.value).variables(variables).functions(functions).build().validate(false).isValid())) {
				throw new ConstraintException("The given expression is invalid!", true);
			}
		} catch(Exception e) {
			throw new ConstraintException("The given expression contains invalid variables or functions", true);
		}
	}

	@Override
	public Type getConstraintType() {
		return Type.POST;
	}
}
