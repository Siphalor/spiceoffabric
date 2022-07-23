package de.siphalor.spiceoffabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SoFMixinConfig implements IMixinConfigPlugin {
	private String ItemStack$getMaxUseTime$remapped;
	private String MaxUseTimeCalculator$getMaxUseTime$desc;

	@Override
	public void onLoad(String mixinPackage) {
		MappingResolver mappingResolver = FabricLoader.getInstance().getMappingResolver();
		String ItemStack$remapped = mappingResolver.mapClassName("intermediary", "net.minecraft.class_1799");
		ItemStack$getMaxUseTime$remapped = mappingResolver.mapMethodName("intermediary", "net.minecraft.class_1799", "method_7935", "()I");
		MaxUseTimeCalculator$getMaxUseTime$desc = "(L" + ItemStack$remapped.replace('.', '/') + ";I)I";
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if ("de.siphalor.spiceoffabric.mixin.MixinItemStack".equals(mixinClassName)) {
			for (MethodNode method : targetClass.methods) {
				if (ItemStack$getMaxUseTime$remapped.equals(method.name)) {
					targetClass.methods.remove(method);
					method.accept(new GetMaxUseTimeTransformer(
							targetClass.visitMethod(method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[0])),
							method.access, method.name, method.desc
					));
					break;
				}
			}
		}
	}

	private class GetMaxUseTimeTransformer extends GeneratorAdapter {
		public GetMaxUseTimeTransformer(MethodVisitor methodVisitor, int access, String name, String descriptor) {
			super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.IRETURN) {
				super.loadThis();
				super.swap();
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "de/siphalor/spiceoffabric/util/MaxUseTimeCalculator", "getMaxUseTime", MaxUseTimeCalculator$getMaxUseTime$desc, false);
			}
			super.visitInsn(opcode);
		}
	}
}
