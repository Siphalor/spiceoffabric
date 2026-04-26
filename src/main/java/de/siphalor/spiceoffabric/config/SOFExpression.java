package de.siphalor.spiceoffabric.config;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.tweed5.core.api.entry.ConfigEntry;
import de.siphalor.tweed5.core.api.entry.SimpleConfigEntry;
import de.siphalor.tweed5.defaultextensions.validation.api.result.ValidationIssue;
import de.siphalor.tweed5.defaultextensions.validation.api.result.ValidationIssueLevel;
import de.siphalor.tweed5.defaultextensions.validation.api.result.ValidationResult;
import de.siphalor.tweed5.serde.extension.api.TweedReadContext;
import de.siphalor.tweed5.serde.extension.api.TweedWriteContext;
import de.siphalor.tweed5.serde.extension.api.read.result.TweedReadIssue;
import de.siphalor.tweed5.serde.extension.api.read.result.TweedReadResult;
import de.siphalor.tweed5.serde.extension.api.readwrite.TweedEntryReaderWriter;
import de.siphalor.tweed5.serde_api.api.*;
import de.siphalor.tweed5.weaver.pojoext.validation.api.validators.WeavableConfigEntryValidator;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NullMarked
@Value
public class SOFExpression {
	String raw;
	Expression compiled;

	public void setVariable(String name, double value) {
		compiled.setVariable(name, value);
	}

	public double evaluate() {
		return compiled.evaluate();
	}

	public static SOFExpression parse(String expression, Config config) {
		Expression compiledExpression = new ExpressionBuilder(expression)
				.functions(config.getFunctions())
				.variables(config.getVariables())
				.build();
		return new SOFExpression(expression, compiledExpression);
	}

	public String toString() {
		return raw;
	}

	public static class Validator implements WeavableConfigEntryValidator {
		@Override
		public <T extends @Nullable Object> ValidationResult<T> validate(ConfigEntry<T> configEntry, T value) {
			if (!(value instanceof SOFExpression)) {
				return ValidationResult.withIssues(null, List.of(
						new ValidationIssue("Must be an expression", ValidationIssueLevel.ERROR)
				));
			}
			SOFExpression expression = (SOFExpression) value;
			net.objecthunter.exp4j.ValidationResult result = expression.getCompiled().validate(false);

			List<ValidationIssue> issues = new ArrayList<>();
			if (result.getErrors() != null) {
				for (String error : result.getErrors()) {
					issues.add(new ValidationIssue(
							error,
							result.isValid() ? ValidationIssueLevel.WARN : ValidationIssueLevel.ERROR
					));
				}
			}

			if (expression.getRaw().contains("^")) {
				issues.add(new ValidationIssue(
						"The ^ operator is deprecated in Spice of Fabric expressions, use the power() function instead",
						ValidationIssueLevel.WARN
				));
			}

			return ValidationResult.withIssues(value, issues);
		}

		@Override
		public <T> String description(ConfigEntry<T> configEntry) {
			return "";
		}
	}

	@RequiredArgsConstructor
	public static class ReaderWriter implements TweedEntryReaderWriter<SOFExpression, SimpleConfigEntry<SOFExpression>> {
		public static final ReaderWriter ITEM = new ReaderWriter(Config.ITEM);
		public static final String ITEM_NAME = SpiceOfFabric.MOD_ID + "_expression_item";
		public static final ReaderWriter AFTER_DEATH = new ReaderWriter(Config.AFTER_DEATH);
		public static final String AFTER_DEATH_NAME = SpiceOfFabric.MOD_ID + "_expression_after_death";
		public static final ReaderWriter HEALTH_FORMULA = new ReaderWriter(Config.HEALTH_FORMULA);
		public static final String HEALTH_FORMULA_NAME = SpiceOfFabric.MOD_ID + "_expression_health_formula";

		private final Config config;

		@Override
		public TweedReadResult<SOFExpression> read(TweedDataReader reader, SimpleConfigEntry<SOFExpression> entry, TweedReadContext context) {
			String rawExpression;
			try {
				TweedDataToken token = reader.readToken();
				if (!token.canReadAsString()) {
					return TweedReadResult.error(TweedReadIssue.error("Expected string token for expression", context));
				}
				rawExpression = token.readAsString();
			} catch (TweedDataReadException e) {
				return TweedReadResult.error(TweedReadIssue.error("Failed to read expression as string", context));
			}

			try {
				return TweedReadResult.ok(parse(rawExpression, config));
			} catch (Exception e) {
				return TweedReadResult.error(TweedReadIssue.error("Failed to parse expression: " + e.getMessage(), context));
			}
		}

		@Override
		public void write(
				TweedDataVisitor writer,
				@Nullable SOFExpression value,
				SimpleConfigEntry<SOFExpression> entry,
				TweedWriteContext context
		) throws TweedDataWriteException {
			if (value == null) {
				writer.visitNull();
			} else {
				writer.visitString(value.getRaw());
			}
		}
	}

	@Value
	public static class Config {
		private static final Function[] FUNCTIONS = new Function[]{
				new Function("max", 2) {
					@Override
					public double apply(double... args) {
						return Math.max(args[0], args[1]);
					}
				},
				new Function("min", 2) {
					@Override
					public double apply(double... args) {
						return Math.min(args[0], args[1]);
					}
				},
				new Function("power", 2) {
					@Override
					public double apply(double... args) {
						return Math.pow(args[0], args[1]);
					}
				}
		};
		public static final Config ITEM = new Config(
				FUNCTIONS,
				Set.of("timesEaten", "hungerValue", "saturationValue", "consumeDuration")
		);
		public static final Config AFTER_DEATH = new Config(
				FUNCTIONS,
				Set.of("hunger", "saturation")
		);
		public static final Config HEALTH_FORMULA = new Config(
				FUNCTIONS,
				Set.of("uniqueFoodsEaten", "baseHealth")
		);

		Function[] functions;
		Set<String> variables;
	}
}
