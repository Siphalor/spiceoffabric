package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.networking.SOFClientNetworking;
import de.siphalor.spiceoffabric.util.FoodUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class SOFClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		SOFClientNetworking.init();

		ItemTooltipCallback.EVENT.register(SOFClient::itemTooltipCallback);

		initRendering();
	}

	private static void itemTooltipCallback(ItemStack stack, TooltipContext context, List<Text> lines) {
		lines.addAll(1, FoodUtils.getClientTooltipAdditions(MinecraftClient.getInstance().player, stack));
	}

	private static void initRendering() {
		if (!SOFConfig.items.usePolymer && SpiceOfFabric.foodContainerItems != null) {
			registerModelPredicateProviders();
		}
	}

	private static void registerModelPredicateProviders() {
		UnclampedModelPredicateProvider predicateProvider = (stack, world, entity, seed) ->
				((FoodContainerItem) stack.getItem()).isInventoryEmpty(stack) ? 0 : 1;
		Identifier predicateId = new Identifier(SpiceOfFabric.MOD_ID, "filled");
		for (Item item : SpiceOfFabric.foodContainerItems) {
			ModelPredicateProviderRegistry.register(item, predicateId, predicateProvider);
		}
	}
}
