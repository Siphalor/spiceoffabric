package de.siphalor.spiceoffabric.config;

import com.google.auto.service.AutoService;
import de.siphalor.tweed5.serde.extension.api.TweedReaderWriterProvider;
import org.jspecify.annotations.NullMarked;

@AutoService(TweedReaderWriterProvider.class)
@NullMarked
public class SOFTweedReaderWriterProvider implements TweedReaderWriterProvider {
	@Override
	public void provideReaderWriters(ProviderContext context) {
		context.registerReaderFactory(SOFExpression.ReaderWriter.ITEM_NAME, a -> SOFExpression.ReaderWriter.ITEM);
		context.registerWriterFactory(SOFExpression.ReaderWriter.ITEM_NAME, a -> SOFExpression.ReaderWriter.ITEM);
		context.registerReaderFactory(SOFExpression.ReaderWriter.AFTER_DEATH_NAME, a -> SOFExpression.ReaderWriter.AFTER_DEATH);
		context.registerWriterFactory(SOFExpression.ReaderWriter.AFTER_DEATH_NAME, a -> SOFExpression.ReaderWriter.AFTER_DEATH);
		context.registerReaderFactory(SOFExpression.ReaderWriter.HEALTH_FORMULA_NAME, a -> SOFExpression.ReaderWriter.HEALTH_FORMULA);
		context.registerWriterFactory(SOFExpression.ReaderWriter.HEALTH_FORMULA_NAME, a -> SOFExpression.ReaderWriter.HEALTH_FORMULA);
	}
}
