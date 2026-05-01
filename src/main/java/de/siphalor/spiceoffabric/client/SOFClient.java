package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.config.SOFExpression;
import de.siphalor.spiceoffabric.config.SOFTweedAttributes;
import de.siphalor.spiceoffabric.container.FoodContainerTooltip;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.networking.SOFClientNetworking;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.tweed5.attributesextension.api.serde.filter.AttributesReadWriteFilterExtension;
import de.siphalor.tweed5.coat.bridge.api.ConfigScreenCreateParams;
import de.siphalor.tweed5.coat.bridge.api.TweedCoatBridgeExtension;
import de.siphalor.tweed5.coat.bridge.api.TweedCoatMappers;
import de.siphalor.tweed5.defaultextensions.presets.api.PresetsExtension;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
//- import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
//- import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
//- import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
//- import net.minecraft.client.renderer.item.ItemProperties;
//- import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
//- import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
//- import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import static de.siphalor.tweed5.defaultextensions.presets.api.PresetsExtension.presetValue;

public class SOFClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		SOFCommonNetworking.init();
		SOFClientNetworking.init();

		initTooltips();
		initRendering();
	}

	private static void initTooltips() {
		ItemTooltipCallback.EVENT.register(SOFClient::itemTooltipCallback);
		//# if MC_VERSION_NUMBER >= 12106
		//# if MC_VERSION_NUMBER >= 260100
		ClientTooltipComponentCallback.EVENT
		//# else
		//- TooltipComponentCallback.EVENT
		//# end
				.register(data -> {
					if (!(data instanceof FoodContainerTooltip(int maxSlots, int filledSlots, int itemCount))) {
						return null;
					}
					if (filledSlots == 0) {
						return new ClientTextTooltip(FoodContainerItem.LORE_EMPTY.getVisualOrderText());
					} else {
						return new ClientTextTooltip(Component.translatable(
								FoodContainerItem.LORE_GENERAL_KEY,
								filledSlots,
								maxSlots,
								itemCount
						).getVisualOrderText());
					}
				});
		//# end
	}

	//# if MC_VERSION_NUMBER >= 12005
	private static void itemTooltipCallback(ItemStack stack, Item.TooltipContext ctx, TooltipFlag flag, List<Component> lines) {
	//# else
	//- private static void itemTooltipCallback(ItemStack stack, TooltipFlag flag, List<Component> lines) {
	//# end
		lines.addAll(1, FoodUtils.getClientTooltipAdditions(Minecraft.getInstance().player, stack));
	}

	private static void initRendering() {
		//# if MC_VERSION_NUMBER >= 12106
		//# elif MC_VERSION_NUMBER >= 12104
		//- ConditionalItemModelProperties.ID_MAPPER.put(
				//- SpiceOfFabric.createId("filled"),
				//- FilledConditionalItemModelProperty.CODEC
		//- );
		//# else
		//- if (!SpiceOfFabric.config.items.usePolymer && SpiceOfFabric.foodContainerItems != null) {
		//- 	registerModelPredicateProviders();
		//- }
		//# end
	}

	//# if MC_VERSION_NUMBER < 12104
	//- private static void registerModelPredicateProviders() {
	//- 	ClampedItemPropertyFunction predicateProvider = (stack, world, entity, seed) ->
	//- 			//# if MC_VERSION_NUMBER >= 12006
	//- 			{
	//- 				HolderLookup.Provider registryAccess;
	//- 				if (world != null) {
	//- 					registryAccess = world.registryAccess();
	//- 				} else if (entity != null) {
	//- 					registryAccess = entity.level().registryAccess();
	//- 				} else {
	//- 					return 0;
	//- 				}
	//- 				return ((FoodContainerItem) stack.getItem()).isInventoryEmpty(stack, registryAccess) ? 0 : 1;
	//- 			};
	//- 			//# else
	//- 			((FoodContainerItem) stack.getItem()).isInventoryEmpty(stack) ? 0 : 1;
	//- 			//# end
	//- 	ResourceLocation predicateId = SpiceOfFabric.createId("filled");
	//- 	for (Item item : SpiceOfFabric.foodContainerItems) {
	//- 		ItemProperties.register(item, predicateId, predicateProvider);
	//- 	}
	//- }
	//# end

	public static Screen createConfigScreen() {
		TweedCoatBridgeExtension coatBridge = SpiceOfFabric.configContainerHelper.configContainer()
				.extension(TweedCoatBridgeExtension.class)
				.orElseThrow(() -> new IllegalStateException("Failed to get TweedCoatBridgeExtension"));

		Stream.of(
				TweedCoatMappers.booleanCheckboxMapper(),
				TweedCoatMappers.integerTextMapper(),
				TweedCoatMappers.enumCycleButtonMapper(),
				TweedCoatMappers.compoundCategoryMapper(),
				TweedCoatMappers.serdeTextMapper(SOFExpression.class)
		).forEach(coatBridge::addMapper);

		SOFConfig defaultValue = SpiceOfFabric.configContainerHelper.configContainer().rootEntry()
				.call(presetValue(PresetsExtension.DEFAULT_PRESET_NAME));

		return coatBridge.createConfigScreen(ConfigScreenCreateParams.<SOFConfig>builder()
				.rootEntry(SpiceOfFabric.configContainerHelper.configContainer().rootEntry())
				.currentValue(SpiceOfFabric.globalConfig)
				.defaultValue(defaultValue)
				.title(Component.translatable(SpiceOfFabric.MOD_ID + ".config"))
				.translationKeyPrefix(SpiceOfFabric.MOD_ID + ".config")
				.saveHandler(value -> {
					SpiceOfFabric.configContainerHelper.writeConfigInConfigDirectory(value);

					AttributesReadWriteFilterExtension filterExtension = SpiceOfFabric.configContainerHelper
							.configContainer()
							.extension(AttributesReadWriteFilterExtension.class)
							.orElseThrow(IllegalStateException::new);
					SpiceOfFabric.configContainerHelper.readPartialConfigInConfigDirectory(SpiceOfFabric.config, context ->
							filterExtension.addFilter(context, SOFTweedAttributes.SCOPE, SOFTweedAttributes.SCOPE_ANY)
					);
				})
				.build());
	}
}
