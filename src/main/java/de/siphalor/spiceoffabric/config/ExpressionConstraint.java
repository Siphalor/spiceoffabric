package de.siphalor.spiceoffabric.config;

import com.mojang.datafixers.util.Pair;
import de.siphalor.tweed4.config.constraints.AnnotationConstraint;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class ExpressionConstraint implements AnnotationConstraint<String> {
	private String[] variables;

	@Override
	public void fromAnnotationParam(String param, Class<?> valueType) {
		variables = StringUtils.split(param, ',');
	}

	@Override
	public Result<String> apply(String value) {
		try {
			Expression expression = new ExpressionBuilder(value).functions(Config.customExpFunctions).variables(variables).build();
			ValidationResult result = expression.validate(false);
			if (!result.isValid()) {
				return new Result<>(false, null, result.getErrors().stream()
						.map(error -> Pair.of(Severity.ERROR, error))
						.toList()
				);
			}
			if (value.contains("^")) {
				return new Result<>(true, value, Collections.singletonList(Pair.of(Severity.WARN, "The ^ operator is deprecated in Spice of Fabric expressions, use the power() function instead")));
			}
			return new Result<>(true, value, Collections.emptyList());
		} catch (IllegalArgumentException e) {
			return new Result<>(false, null, List.of(
					Pair.of(Severity.ERROR, e.getMessage() + "; in expression: " + value)
			));
		} catch (Exception e) {
			return new Result<>(false, null, List.of(
					Pair.of(Severity.ERROR, "Invalid expression, got " + e.getClass().getSimpleName() + ": " + e.getMessage() + "; in expression: " + value)
			));
		}
	}

	@Override
	public String getDescription() {
		return "Must be a valid expression";
	}
}
