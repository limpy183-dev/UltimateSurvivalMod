package com.ultimatesurvival.client.rig;

import com.ultimatesurvival.mob.UsmMob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A model built at runtime from a {@link RigDef}. Pixel coordinates in the rig are feet-up; vanilla model space is
 * y-down with the feet at y=24, so conversion happens here once. Animation is a list of declarative rules.
 */
public final class RigModel extends EntityModel<RigRenderState> {
	private static final class BoundRule {
		final RigDef.Rule r;
		final ModelPart part;
		final int axis;
		final int kind;
		final int whenKind;
		final String whenArg;

		BoundRule(RigDef.Rule r, ModelPart part) {
			this.r = r;
			this.part = part;
			this.axis = switch (r.axis()) {
				case "rx" -> 0;
				case "ry" -> 1;
				case "rz" -> 2;
				case "px" -> 3;
				case "py" -> 4;
				case "pz" -> 5;
				case "sx" -> 6;
				case "sy" -> 7;
				default -> 8;
			};
			this.kind = switch (r.fn()) {
				case "sin" -> 0;
				case "walk" -> 1;
				case "kf" -> 2;
				case "const" -> 3;
				case "look" -> 4;
				case "spin" -> 5;
				default -> 3;
			};
			String w = r.when();
			int c = w.indexOf(':');
			this.whenArg = c >= 0 ? w.substring(c + 1) : "";
			this.whenKind = switch (c >= 0 ? w.substring(0, c) : w) {
				case "always" -> 0;
				case "idle" -> 1;
				case "walk" -> 2;
				case "attack" -> 3;
				case "spawn" -> 4;
				case "dead" -> 5;
				case "vuln" -> 6;
				case "enraged" -> 7;
				case "phase" -> 8;
				default -> 0;
			};
		}
	}

	private final List<BoundRule> rules = new ArrayList<>();

	public RigModel(ModelPart root, RigDef def) {
		super(root);
		Map<String, ModelPart> parts = new HashMap<>();
		parts.put("root", root);
		for (RigDef.Bone b : def.bones) {
			ModelPart parent = b.parent() == null ? root : parts.get(b.parent());
			parts.put(b.name(), parent.getChild(b.name()));
		}
		for (RigDef.Rule r : def.rules) {
			ModelPart p = parts.get(r.bone());
			if (p != null) {
				rules.add(new BoundRule(r, p));
			}
		}
	}

	/** Builds the vanilla mesh for a rig: bone pivots become part poses, boxes become cubes. */
	public static LayerDefinition createLayer(RigDef def) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		Map<String, PartDefinition> defs = new HashMap<>();
		Map<String, RigDef.Bone> bones = new HashMap<>();
		for (RigDef.Bone b : def.bones) {
			PartDefinition parent = b.parent() == null ? root : defs.get(b.parent());
			float ppx = 0;
			float ppy = 24;
			float ppz = 0;
			if (b.parent() != null) {
				RigDef.Bone pb = bones.get(b.parent());
				ppx = pb.px();
				ppy = pb.py();
				ppz = pb.pz();
			} else {
				ppy = 24;                                               // model origin sits 24px above the feet-up zero
			}
			CubeListBuilder cubes = CubeListBuilder.create();
			for (RigDef.Cube c : b.cubes()) {
				cubes.texOffs(c.u(), c.v()).mirror(c.mirror()).addBox(c.x() - b.px(), b.py() - c.y() - c.h(), c.z() - b.pz(), c.w(), c.h(), c.d(),
						new CubeDeformation(c.inflate()));
			}
			float ox = b.px() - ppx;
			float oy = b.parent() == null ? 24 - b.py() : -(b.py() - ppy);
			float oz = b.pz() - ppz;
			PartDefinition pd = parent.addOrReplaceChild(b.name(), cubes, PartPose.offsetAndRotation(ox, oy, oz, b.rx(), b.ry(), b.rz()));
			defs.put(b.name(), pd);
			bones.put(b.name(), b);
		}
		return LayerDefinition.create(mesh, def.texW, def.texH);
	}

	@Override
	public void setupAnim(RigRenderState s) {
		super.setupAnim(s);
		for (BoundRule br : rules) {
			if (!applies(br, s)) {
				continue;
			}
			float v = (float) value(br, s);
			ModelPart p = br.part;
			switch (br.axis) {
				case 0 -> p.xRot += v;
				case 1 -> p.yRot += v;
				case 2 -> p.zRot += v;
				case 3 -> p.x += v;
				case 4 -> p.y -= v;
				case 5 -> p.z -= v;
				case 6 -> p.xScale += v;
				case 7 -> p.yScale += v;
				default -> p.zScale += v;
			}
		}
	}

	private static boolean applies(BoundRule br, RigRenderState s) {
		boolean playing = s.animProgress >= 0;
		return switch (br.whenKind) {
			case 0, 2 -> true;
			case 1 -> !playing;
			case 3 -> playing && s.anim.equals(br.whenArg);
			case 4 -> playing && s.anim.equals("spawn");
			case 5 -> playing && s.anim.equals("dead");
			case 6 -> (s.flags & UsmMob.F_VULN) != 0;
			case 7 -> (s.flags & UsmMob.F_ENRAGED) != 0;
			case 8 -> s.phase >= parse(br.whenArg);
			default -> true;
		};
	}

	private static int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static double value(BoundRule br, RigRenderState s) {
		RigDef.Rule r = br.r;
		return switch (br.kind) {
			case 0 -> r.amp() * Math.sin(s.ageInTicks * r.speed() + r.phase()) + r.off();
			case 1 -> r.amp() * Math.sin(s.walkAnimationPos * r.speed() + r.phase()) * Math.min(1.0f, s.walkAnimationSpeed * 1.5f);
			case 2 -> keyframes(r.keys(), s.animProgress);
			case 4 -> br.axis == 1 ? s.yRot * Mth.DEG_TO_RAD * r.mul() : s.xRot * Mth.DEG_TO_RAD * r.mul();
			case 5 -> s.ageInTicks * r.speed();
			default -> r.value();
		};
	}

	/** Smoothly interpolated keyframes over progress 0..1 (clamped outside the first/last key). */
	private static double keyframes(float[][] k, float t) {
		if (k == null || k.length == 0) {
			return 0;
		}
		if (t <= k[0][0]) {
			return k[0][1];
		}
		for (int i = 1; i < k.length; i++) {
			if (t <= k[i][0]) {
				float span = k[i][0] - k[i - 1][0];
				float f = span < 1e-6f ? 1f : (t - k[i - 1][0]) / span;
				f = f * f * (3f - 2f * f);
				return k[i - 1][1] + (k[i][1] - k[i - 1][1]) * f;
			}
		}
		return k[k.length - 1][1];
	}
}
