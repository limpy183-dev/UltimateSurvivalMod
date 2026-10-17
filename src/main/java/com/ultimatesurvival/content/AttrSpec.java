package com.ultimatesurvival.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** An attribute change declared in the content tables: {"attr": "max_health", "amount": 4, "op": "add"}. */
public record AttrSpec(Holder<Attribute> attr, String name, double amount, AttributeModifier.Operation op) {
	public static List<AttrSpec> of(ItemDef def) {
		List<AttrSpec> out = new ArrayList<>();
		if (def.json.has("attrs")) {
			for (JsonElement e : def.json.getAsJsonArray("attrs")) {
				JsonObject o = e.getAsJsonObject();
				String name = o.get("attr").getAsString();
				Holder<Attribute> h = BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace(name))
						.orElseThrow(() -> new IllegalArgumentException("unknown attribute " + name + " on " + def.id));
				AttributeModifier.Operation op = switch (o.has("op") ? o.get("op").getAsString() : "add") {
					case "mul_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
					case "mul" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
					default -> AttributeModifier.Operation.ADD_VALUE;
				};
				out.add(new AttrSpec(h, name, o.get("amount").getAsDouble(), op));
			}
		}
		return out;
	}

	public Identifier id(String prefix) {
		return UltimateSurvival.id(prefix + "_" + name + "_" + op.ordinal());
	}
}
