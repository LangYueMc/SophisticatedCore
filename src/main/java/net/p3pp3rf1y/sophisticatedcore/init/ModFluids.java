package net.p3pp3rf1y.sophisticatedcore.init;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import io.github.fabricators_of_create.porting_lib.util.SimpleFlowableFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;


public class ModFluids {
	private ModFluids() {}

	private static SimpleFlowableFluid.Properties fluidProperties() {
		return new SimpleFlowableFluid.Properties(XP_STILL, XP_FLOWING);
	}

	public static final ResourceLocation EXPERIENCE_TAG_NAME = new ResourceLocation("forge:experience");

	public static final TagKey<Fluid> EXPERIENCE_TAG = TagKey.create(Registries.FLUID, EXPERIENCE_TAG_NAME);
	public static final LazyRegistrar<Fluid> FLUIDS = LazyRegistrar.create(BuiltInRegistries.FLUID, SophisticatedCore.ID);

	public static final RegistryObject<FlowingFluid> XP_STILL = FLUIDS.register("xp_still", () -> new SimpleFlowableFluid.Still(fluidProperties()));

	public static final RegistryObject<FlowingFluid> XP_FLOWING = FLUIDS.register("xp_flowing", () -> new SimpleFlowableFluid.Flowing(fluidProperties()));

	
	public static void registerHandlers() {
		FLUIDS.register();
	}
}
