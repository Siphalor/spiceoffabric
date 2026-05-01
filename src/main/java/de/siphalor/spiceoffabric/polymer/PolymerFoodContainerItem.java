package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
//- import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
//- import lombok.RequiredArgsConstructor;
//# if MC_VERSION_NUMBER >= 260100
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
//# else
//- import xyz.nucleoid.packettweaker.PacketContext;
//# end
//- import org.jetbrains.annotations.Nullable;

//- import net.minecraft.core.component.DataComponents;
//- import net.minecraft.resources.ResourceLocation;
//- import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//- import net.minecraft.world.item.TooltipFlag;
//- import net.minecraft.world.item.component.CustomModelData;

public class PolymerFoodContainerItem extends FoodContainerItem implements PolymerItem {
	private final Item emptyPolymerItem;
	private final Item filledPolymerItem;
	//# if MC_VERSION_NUMBER >= 12111
	//# elif MC_VERSION_NUMBER >= 12106
	//- private final ResourceLocation polymerModel;
	//# elif MC_VERSION_NUMBER >= 12104
	//- private final ResourceLocation polymerModel;
	//- private final CustomModelData emptyCmd;
	//- private final CustomModelData filledCmd;
	//# elif MC_VERSION_NUMBER >= 12102
	//- private final ResourceLocation emptyPolymerModel;
	//- private final ResourceLocation filledPolymerModel;
	//# else
	//- private final int emptyCmd;
	//- private final int filledCmd;
	//# end

	public PolymerFoodContainerItem(
			Properties settings,
			int size,
			MenuType<?> screenHandlerType,
			Item emptyPolymerItem,
			Item filledPolymerItem
			//# if MC_VERSION_NUMBER >= 12111
			//# elif MC_VERSION_NUMBER >= 12106
			//- , ResourceLocation polymerModel
			//# elif MC_VERSION_NUMBER >= 12104
			//- , ResourceLocation polymerModel
			//- , CustomModelData emptyCmd
			//- , CustomModelData filledCmd
			//# elif MC_VERSION_NUMBER >= 12102
			//- , ResourceLocation emptyPolymerModel
			//- , ResourceLocation filledPolymerModel
			//# else
			//- , int emptyCmd
			//- , int filledCmd
			//# end
	) {
		super(settings, size, screenHandlerType);
		this.emptyPolymerItem = emptyPolymerItem;
		this.filledPolymerItem = filledPolymerItem;
		//# if MC_VERSION_NUMBER >= 12106
		//# elif MC_VERSION_NUMBER >= 12104
		//- this.polymerModel = polymerModel;
		//- this.emptyCmd = emptyCmd;
		//- this.filledCmd = filledCmd;
		//# elif MC_VERSION_NUMBER >= 12102
		//- this.emptyPolymerModel = emptyPolymerModel;
		//- this.filledPolymerModel = filledPolymerModel;
		//# else
		//- this.emptyCmd = emptyCmd;
		//- this.filledCmd = filledCmd;
		//# end
	}

	@Override
	//# if MC_VERSION_NUMBER >= 12102
	public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
		//# if MC_VERSION_NUMBER >= 260100
		var registryAccess = context.get(PacketContext.REGISTRY_ACCESS);
		//# else
		//- ServerPlayer player = context.getPlayer();
		//- var registryAccess = player == null ? null : player.registryAccess();
		//# end
	//# else
	//- public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayer player) {
		//- var registryAccess = player == null ? null : player.registryAccess();
	//# end
		//# if MC_VERSION_NUMBER >= 12006
		return registryAccess != null && isInventoryEmpty(itemStack, registryAccess)
				? emptyPolymerItem
				: filledPolymerItem;
		//# else
		//- return getInventory(itemStack).isEmpty() ? emptyPolymerItem : filledPolymerItem;
		//# end
	}

	//# if MC_VERSION_NUMBER >= 12106
	//# elif MC_VERSION_NUMBER >= 12102
	//- @Override
	//- public @Nullable ResourceLocation getPolymerItemModel(ItemStack stack, PacketContext context) {
		//- //# if MC_VERSION_NUMBER >= 12104
		//- return polymerModel;
		//- //# else
		//- if (isInventoryEmpty(stack, context.getPlayer().registryAccess())) {
		//- 	return emptyPolymerModel;
		//- } else {
		//- 	return filledPolymerModel;
		//- }
		//- //# end
	//- }
	//- //# if MC_VERSION_NUMBER >= 12104
	//- @Override
	//- public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
		//- ItemStack polymerStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
		//- if (isInventoryEmpty(polymerStack, context.getPlayer().registryAccess())) {
		//- 	polymerStack.set(DataComponents.CUSTOM_MODEL_DATA, emptyCmd);
		//- } else {
		//- 	polymerStack.set(DataComponents.CUSTOM_MODEL_DATA, filledCmd);
		//- }
		//- return polymerStack;
	//- }
	//- //# end
	//# else
	//- @Override
	//- public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayer player) {
		//- //# if MC_VERSION_NUMBER >= 12006
		//- return player != null && isInventoryEmpty(itemStack, player.registryAccess()) ? emptyCmd : filledCmd;
		//- //# else
		//- return getInventory(itemStack).isEmpty() ? emptyCmd : filledCmd;
		//- //# end
	//- }
	//# end
}
