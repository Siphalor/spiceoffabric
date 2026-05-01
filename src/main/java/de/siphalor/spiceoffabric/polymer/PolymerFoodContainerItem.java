package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
//- import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
//- import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
//- import org.jetbrains.annotations.Nullable;
//# if MC_VERSION_NUMBER >= 260100
//# else
//- import xyz.nucleoid.packettweaker.PacketContext;
//# end

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
//- import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PolymerFoodContainerItem extends FoodContainerItem implements PolymerItem {
	private final Item emptyPolymerItem;
	private final Item filledPolymerItem;
	//# if MC_VERSION_NUMBER >= 12111
	private final Identifier emptyPolymerModel;
	private final Identifier filledPolymerModel;
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
			, Identifier emptyPolymerModel
			, Identifier filledPolymerModel
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
		//# if MC_VERSION_NUMBER >= 12102
		this.emptyPolymerModel = emptyPolymerModel;
		this.filledPolymerModel = filledPolymerModel;
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

	//# if MC_VERSION_NUMBER >= 260100
	@Override
	public @org.jspecify.annotations.Nullable Identifier getPolymerItemModel(
			ItemStack stack,
			PacketContext context,
			HolderLookup.Provider registryAccess
	) {
		if (isInventoryEmpty(stack, registryAccess)) {
			return emptyPolymerModel;
		} else {
			return filledPolymerModel;
		}
	}
	//# elif MC_VERSION_NUMBER >= 12102
	//- @Override
	//- public @Nullable
	//- /*# if MC_VERSION_NUMBER >= 12111 */Identifier/*# else */ResourceLocation/*# end */
	//- getPolymerItemModel(ItemStack stack, PacketContext context) {
	//- 	if (isInventoryEmpty(stack, context.getPlayer().registryAccess())) {
	//- 		return emptyPolymerModel;
	//- 	} else {
	//- 		return filledPolymerModel;
	//- 	}
	//- }
	//# else
	//- @Override
	//- public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayer player) {
	//- 	//# if MC_VERSION_NUMBER >= 12006
	//- 	return player != null && isInventoryEmpty(itemStack, player.registryAccess()) ? emptyCmd : filledCmd;
	//- 	//# else
	//- 	return getInventory(itemStack).isEmpty() ? emptyCmd : filledCmd;
	//- 	//# end
	//- }
	//# end
}
