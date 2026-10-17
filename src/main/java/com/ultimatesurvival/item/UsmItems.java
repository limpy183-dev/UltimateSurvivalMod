package com.ultimatesurvival.item;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.AttrSpec;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Registers every item row of the content manifest. Behaviour is chosen by {@code kind}. */
public final class UsmItems {
	public static final Map<String, Item> BY_ID = new LinkedHashMap<>();
	private static final Map<Item, ItemDef> DEFS = new IdentityHashMap<>();
	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, UltimateSurvival.id("main"));

	private UsmItems() {
	}

	public static Item get(String id) {
		Item item = BY_ID.get(id);
		if (item == null) {
			throw new IllegalArgumentException("unknown ultimatesurvival item: " + id);
		}
		return item;
	}

	/** The content row behind an item, or null for vanilla / foreign items. */
	public static ItemDef defOf(Item item) {
		return DEFS.get(item);
	}

	public static void register() {
		for (ItemDef def : Content.ITEMS) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, UltimateSurvival.id(def.id));
			Item item = Registry.register(BuiltInRegistries.ITEM, key, create(def, key));
			BY_ID.put(def.id, item);
			DEFS.put(item, def);
			Gates.bind(item, Content.gate(def.gate));
		}
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.ultimatesurvival"))
				.icon(() -> new ItemStack(BY_ID.getOrDefault("apex_blade", BY_ID.values().iterator().next())))
				.displayItems((params, output) -> {
					UsmBlocks.ITEMS.values().forEach(output::accept);
					com.ultimatesurvival.mob.Mobs.EGGS.values().forEach(output::accept);
					for (ItemDef d : Content.ITEMS) {
						if (!d.json.has("hidden")) {
							output.accept(BY_ID.get(d.id));
						}
					}
				})
				.build());
	}

	private static Item create(ItemDef def, ResourceKey<Item> key) {
		Item.Properties p = new Item.Properties().setId(key)
				.rarity(Rarity.valueOf(def.rarity.toUpperCase(Locale.ROOT)));
		if (def.glint) {
			p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		if (def.has("stack")) {
			p.stacksTo(def.json.get("stack").getAsInt());
		}
		return switch (def.kind) {
			case "sword", "staff" -> new UsmWeaponItem(melee(p, def), def);
			case "gadget" -> new UsmWeaponItem(repair(p.stacksTo(1), def), def);
			case "bow" -> new UsmBow(repair(p.durability((int) def.stat("durability", 800)).enchantable((int) def.stat("enchant", 15)), def), def);
			case "codex" -> new com.ultimatesurvival.codex.CodexItem(p.stacksTo(1));
			case "sigil" -> new SigilItem(p.stacksTo(16), def);
			case "bag" -> new BagItem(p.stacksTo(16), def);
			case "armor" -> new Item(armor(p, def));
			case "accessory" -> new Item(p.stacksTo(1));
			case "consumable" -> new UsmConsumable(consumable(p, def), def);
			case "pickaxe" -> new DigItems.Pickaxe(tool(p, def).pickaxe(material(def), (float) (dmg(def) - 1), (float) (speed(def) - 4)), def);
			case "shovel" -> new DigItems.Shovel(material(def), (float) (dmg(def) - 1), (float) (speed(def) - 4), tool(p, def), def);
			case "axe" -> new DigItems.Axe(material(def), (float) (dmg(def) - 1), (float) (speed(def) - 4), tool(p, def), def);
			case "hoe" -> new DigItems.Hoe(material(def), (float) (dmg(def) - 1), (float) (speed(def) - 4), tool(p, def), def);
			default -> new Item(p);
		};
	}

	// ---- stat helpers -----------------------------------------------------------------------
	private static double dmg(ItemDef def) {
		return def.stat("damage", 5) * UsmConfig.get().weaponDamageMultiplier;
	}

	private static double speed(ItemDef def) {
		return def.stat("speed", 1.6);
	}

	private static ToolMaterial material(ItemDef def) {
		var incorrect = def.gate.equals("start") ? BlockTags.INCORRECT_FOR_IRON_TOOL : BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		return new ToolMaterial(incorrect, (int) def.stat("durability", 1000), (float) def.stat("mine", 8), 0.0F,
				(int) def.stat("enchant", 15), ItemTags.NETHERITE_TOOL_MATERIALS);
	}

	/** Shared extras for tools: repair material. */
	private static Item.Properties tool(Item.Properties p, ItemDef def) {
		return repair(p, def);
	}

	private static Item.Properties repair(Item.Properties p, ItemDef def) {
		String rep = def.str("repair", null);
		if (rep != null) {
			Identifier id = rep.contains(":") ? Identifier.parse(rep) : UltimateSurvival.id(rep);
			BuiltInRegistries.ITEM.get(id).ifPresent(h -> p.repairable(h.value()));
		}
		return p;
	}

	/** Melee weapon: damage / speed / reach come straight from the balance-derived stats. */
	private static Item.Properties melee(Item.Properties p, ItemDef def) {
		double damage = dmg(def);
		double speed = speed(def);
		double reach = def.stat("reach", 0);
		p.sword(material(def), (float) (damage - 1.0), (float) (speed - 4.0));
		ItemAttributeModifiers.Builder attrs = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		if (reach != 0) {
			attrs.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(UltimateSurvival.id("weapon_reach"), reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
		p.component(DataComponents.SWING_ANIMATION, swing(def.str("arch", "sword")));
		return repair(p.attributes(attrs.build()), def);
	}

	/** Thin blades and spears thrust, heavy weapons take long, slow arcs - first and third person alike. */
	private static SwingAnimation swing(String arch) {
		return switch (arch) {
			case "dagger" -> new SwingAnimation(SwingAnimationType.STAB, 4);
			case "rapier" -> new SwingAnimation(SwingAnimationType.STAB, 5);
			case "spear" -> new SwingAnimation(SwingAnimationType.STAB, 8);
			case "staff" -> new SwingAnimation(SwingAnimationType.STAB, 7);
			case "axe" -> new SwingAnimation(SwingAnimationType.WHACK, 8);
			case "scythe" -> new SwingAnimation(SwingAnimationType.WHACK, 9);
			case "greatsword" -> new SwingAnimation(SwingAnimationType.WHACK, 11);
			case "hammer" -> new SwingAnimation(SwingAnimationType.WHACK, 14);
			default -> new SwingAnimation(SwingAnimationType.WHACK, 6);
		};
	}

	/** Humanoid armor piece: vanilla equippable machinery + the balance-table numbers + optional extra attributes. */
	private static Item.Properties armor(Item.Properties p, ItemDef def) {
		ArmorType type = switch (def.str("slot", "chest")) {
			case "head" -> ArmorType.HELMET;
			case "legs" -> ArmorType.LEGGINGS;
			case "feet" -> ArmorType.BOOTS;
			default -> ArmorType.CHESTPLATE;
		};
		int defense = (int) Math.round(def.stat("armor", 3) * UsmConfig.get().armorMultiplier);
		var sound = def.tier <= 1 ? SoundEvents.ARMOR_EQUIP_IRON : def.tier == 2 ? SoundEvents.ARMOR_EQUIP_DIAMOND : SoundEvents.ARMOR_EQUIP_NETHERITE;
		ArmorMaterial mat = new ArmorMaterial((int) def.stat("durability_mult", 30), Map.of(type, defense), (int) def.stat("enchant", 15), sound,
				(float) def.stat("toughness", 0), (float) def.stat("kb", 0), ItemTags.NETHERITE_TOOL_MATERIALS,
				ResourceKey.create(EquipmentAssets.ROOT_ID, UltimateSurvival.id(def.str("set", def.id))));
		p.humanoidArmor(mat, type);
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
		String pre = "a_" + def.id;
		ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(UltimateSurvival.id(pre + "_armor"), defense, AttributeModifier.Operation.ADD_VALUE), group)
				.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(UltimateSurvival.id(pre + "_tough"), def.stat("toughness", 0), AttributeModifier.Operation.ADD_VALUE), group);
		if (def.stat("kb", 0) > 0) {
			b.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(UltimateSurvival.id(pre + "_kb"), def.stat("kb", 0), AttributeModifier.Operation.ADD_VALUE), group);
		}
		for (AttrSpec a : AttrSpec.of(def)) {
			b.add(a.attr(), new AttributeModifier(a.id(pre), a.amount(), a.op()), group);
		}
		return repair(p.attributes(b.build()), def);
	}

	private static Item.Properties consumable(Item.Properties p, ItemDef def) {
		boolean food = def.str("anim", "drink").equals("eat");
		Consumable.Builder c = Consumable.builder().consumeSeconds((float) def.stat("seconds", food ? 1.6 : 1.2))
				.animation(food ? ItemUseAnimation.EAT : ItemUseAnimation.DRINK)
				.sound(food ? SoundEvents.GENERIC_EAT : SoundEvents.GENERIC_DRINK)
				.hasConsumeParticles(food);
		p.component(DataComponents.CONSUMABLE, c.build()).stacksTo((int) def.stat("stack", 16));
		if (food) {
			p.food(new net.minecraft.world.food.FoodProperties((int) def.stat("nutrition", 6), (float) def.stat("saturation", 0.8), true), c.build());
		}
		return p;
	}

	public static Identifier idOf(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}
}
