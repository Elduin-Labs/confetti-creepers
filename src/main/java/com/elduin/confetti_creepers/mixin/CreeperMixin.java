package com.elduin.confetti_creepers.mixin;

import com.elduin.confetti_creepers.Confetti;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
@MixinEnvironment(type = MixinEnvironment.Env.MAIN)
public abstract class CreeperMixin {

	@Unique
	private boolean confetti_creepers$calledYet;

	/** When you walk up to the creeper, it calls you on the phone. */
	@Inject(method = "tick", at = @At("HEAD"))
	private void confetti_creepers$phoneCall(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		if (!(self.level() instanceof ServerLevel level)) {
			return;
		}
		Player near = level.getNearestPlayer(self, 5.0);
		if (near instanceof ServerPlayer player && !confetti_creepers$calledYet) {
			confetti_creepers$calledYet = true;
			Confetti.creeperCalls(self, level, player);
		} else if (level.getNearestPlayer(self, 8.0) == null) {
			confetti_creepers$calledYet = false;
		}
	}

	/** Instead of exploding, the creeper pops into confetti. */
	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
	private void confetti_creepers$confettiInstead(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		if (self.level() instanceof ServerLevel level) {
			Confetti.creeperPop(self, level);
			ci.cancel();
		}
	}
}
