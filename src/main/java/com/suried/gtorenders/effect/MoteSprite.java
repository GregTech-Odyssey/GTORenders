package com.suried.gtorenders.effect;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

/**
 * A mod's particle sprite set seen from the particle atlas: frame UVs are read once and re-read after every resource reload.
 */
@OnlyIn(Dist.CLIENT)
public final class MoteSprite {

    static int generation;
    private final SpriteSet set;
    private int seen = -1;
    private float[] uvs = new float[0];
    private int frames;
    int tableRow = -1;

    private MoteSprite(SpriteSet set) {
        this.set = set;
    }

    public static MoteSprite of(SpriteSet set) {
        return new MoteSprite(set);
    }

    private void resolve() {
        if (seen == generation) return;
        seen = generation;
        tableRow = -1;
        var list = new ArrayList<TextureAtlasSprite>();
        TextureAtlasSprite last = null;
        for (int i = 0; i <= 64; i++) {
            TextureAtlasSprite s;
            try {
                s = set.get(i, 64);
            } catch (RuntimeException e) {
                break;
            }
            if (s != null && s != last && !list.contains(s)) list.add(s);
            last = s;
        }
        frames = list.size();
        uvs = new float[frames * 4];
        for (int i = 0; i < frames; i++) {
            var s = list.get(i);
            uvs[i * 4] = s.getU0();
            uvs[i * 4 + 1] = s.getV0();
            uvs[i * 4 + 2] = s.getU1();
            uvs[i * 4 + 3] = s.getV1();
        }
    }

    int frames() {
        resolve();
        return frames;
    }

    float[] uvs() {
        resolve();
        return uvs;
    }
}
