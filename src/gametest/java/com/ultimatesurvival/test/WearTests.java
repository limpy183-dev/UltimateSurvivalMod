package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;
import static com.ultimatesurvival.test.T.near;

import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.armor.WearerAbilities;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.UsmState;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Armor sets, set bonuses, accessories and the accessory menu. */
final class WearTests {
	private WearTests() {
	}

	@SuppressWarnings("unchecked")
	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) throws Exception {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(2);

		// ---- a locked set is just decoration ----------------------------------------------------------------------------
		T.wear(w, "azurite");
		ctx.waitTicks(4);
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ARMOR)), 0.0, 1e-6, "locked azurite armor gives no armor points");
		check(!T.onServer(w, () -> Wearer.has(T.player(w), "double_jump")), "locked set grants no bonus");

		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(4);
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ARMOR)), 30.0, 1e-6, "unlocked azurite set = 30 armor (the balance table)");
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ARMOR_TOUGHNESS)), 8.0, 1e-6, "and 8 toughness");
		check(T.onServer(w, () -> Wearer.has(T.player(w), "double_jump") && Wearer.has(T.player(w), "feather_fall") && Wearer.has(T.player(w), "glide_sneak")),
				"full azurite set grants Skyborne (double jump, feather fall, air brake)");

		// feather fall: a 10-damage fall hit does nothing
		float hp = T.onServer(w, () -> T.player(w).getHealth());
		T.onServer(w, () -> T.player(w).hurtServer(T.level(w), T.level(w).damageSources().fall(), 10f));
		near(T.onServer(w, () -> T.player(w).getHealth()), hp, 0.01, "feather fall cancels fall damage");

		// double jump: one mid-air boost, then refused until landing
		Vec3 ground = T.onServer(w, () -> T.player(w).position());
		T.stand(w, ground.x, ground.y + 12, ground.z, ground.x + 5, ground.y + 12, ground.z);
		ctx.waitTicks(2);
		T.onServer(w, () -> {
			T.player(w).setDeltaMovement(0, -0.3, 0);
			WearerAbilities.doubleJump(T.player(w));
		});
		double vy = T.onServer(w, () -> T.player(w).getDeltaMovement().y);
		check(vy > 0.5, "double jump boosted the player upward (vy=" + vy + ")");
		T.onServer(w, () -> T.player(w).setDeltaMovement(0, -0.3, 0));
		T.onServer(w, () -> WearerAbilities.doubleJump(T.player(w)));
		check(T.onServer(w, () -> T.player(w).getDeltaMovement().y) < 0, "a second mid-air jump is refused");
		T.stand(w, ground.x, ground.y, ground.z, ground.x + 5, ground.y + 1.6, ground.z);
		ctx.waitTicks(30);

		// third-person screenshot of the set
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		ctx.waitTicks(6);
		T.takeShot(ctx, "phase2-azurite-armor");
		T.stripArmor(w);
		T.wear(w, "verdite");
		ctx.waitTicks(6);
		T.takeShot(ctx, "phase2-verdite-armor");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

		// ---- verdite set: out-of-combat regeneration ------------------------------------------------------------------
		server.runCommand("effect clear @a");
		T.onServer(w, () -> Wearer.forget(T.player(w).getUUID()));       // forget the earlier fall-damage test
		T.onServer(w, () -> T.player(w).setHealth(10f));
		ctx.waitTicks(50);
		check(T.onServer(w, () -> T.player(w).getHealth()) >= 12f, "Verdant Resolve regenerates when unhurt (health "
				+ T.onServer(w, () -> T.player(w).getHealth()) + ", active " + T.onServer(w, () -> Wearer.active(T.player(w)).keySet())
				+ ", head " + T.onServer(w, () -> T.player(w).getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)) + ")");
		T.onServer(w, () -> T.player(w).hurtServer(T.level(w), T.level(w).damageSources().generic(), 1f));
		float afterHit = T.onServer(w, () -> T.player(w).getHealth());
		ctx.waitTicks(60);
		check(T.onServer(w, () -> T.player(w).getHealth()) <= afterHit + 1.2f, "regeneration pauses while recently hurt (only vanilla food regen, never the 3 points a second of Verdant Resolve: " + afterHit + " -> "
				+ T.onServer(w, () -> T.player(w).getHealth()) + ")");
		ctx.waitTicks(70);
		check(T.onServer(w, () -> T.player(w).getHealth()) > afterHit + 1.5f, "...and resumes afterwards");
		T.stripArmor(w);

		// ---- accessories ---------------------------------------------------------------------------------------------------
		server.runCommand("usm reset");
		ctx.waitTicks(2);
		var list = T.onServer(w, () -> UsmState.progress().accessories(T.player(w).getUUID()));
		T.onServer(w, () -> {
			list.set(0, new ItemStack(UsmItems.get("clover_charm")));
			list.set(1, new ItemStack(UsmItems.get("verdite_ring")));
			list.set(2, new ItemStack(UsmItems.get("drake_heart")));       // tier 2 -> locked right now
		});
		ctx.waitTicks(15);
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.LUCK)), 1.5, 1e-6, "Lucky Clover gives +1.5 luck");
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ARMOR)), 2.0, 1e-6, "Verdite Ring gives +2 armor");
		near(T.onServer(w, () -> T.player(w).getMaxHealth()), 20.0, 1e-6, "a locked Drake Heart gives nothing");
		server.runCommand("usm unlock ender_dragon");
		T.onServer(w, () -> list.set(3, new ItemStack(UsmItems.get("verdite_ring"))));
		T.onServer(w, () -> list.set(4, new ItemStack(UsmItems.get("magnet_ring"))));
		ctx.waitTicks(15);
		near(T.onServer(w, () -> T.player(w).getMaxHealth()), 20.0 + T.attr("drake_heart", "max_health"), 1e-6, "unlocked Drake Heart: its hearts");
		near(T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ARMOR)), 4.0, 1e-6, "two Verdite Rings stack to +4 armor");
		check(T.onServer(w, () -> Wearer.has(T.player(w), "magnet")), "Magnet Ring grants the magnet ability");

		T.onServer(w, () -> list.set(2, ItemStack.EMPTY));
		ctx.waitTicks(15);
		near(T.onServer(w, () -> T.player(w).getMaxHealth()), 20.0, 1e-6, "removing the accessory removes its bonus");

		// the menu itself, through the real client
		T.onServer(w, () -> com.ultimatesurvival.item.Accessories.open(T.player(w)));
		ctx.waitForScreen((Class<? extends Screen>) Class.forName("com.ultimatesurvival.client.gui.AccessoryScreen"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "phase2-accessory-menu");
		ctx.runOnClient(mc -> mc.gui.setScreen(null));
		T.onServer(w, () -> list.replaceAll(s -> ItemStack.EMPTY));
		ctx.waitTicks(12);
	}
}
