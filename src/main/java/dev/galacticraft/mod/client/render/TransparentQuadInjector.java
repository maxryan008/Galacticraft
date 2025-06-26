package dev.galacticraft.mod.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TransparentQuadInjector {
    public record QuadVertex(float x, float y, float z, float u, float v, int color, int light, int overlay, float nx, float ny, float nz) {}

    public record InjectedQuad(QuadVertex v1, QuadVertex v2, QuadVertex v3, QuadVertex v4) {
        public boolean isInsideSection(SectionPos section) {
            return Stream.of(v1, v2, v3, v4).allMatch(v -> {
                BlockPos p = BlockPos.containing(v.x(), v.y(), v.z());
                return SectionPos.of(p).equals(section);
            });
        }

        public SectionPos section() {
            return SectionPos.of(new BlockPos((int) v1.x(), (int) v1.y(), (int) v1.z()));
        }
    }

    public static final Map<ResourceKey<Level>, Map<SectionPos, List<InjectedQuad>>> quadMapByDimension = new HashMap<>(); //fixme make private not pub

    public static void addQuad(Level level, InjectedQuad quad) {
        ResourceKey<Level> dim = level.dimension();
        SectionPos section = quad.section();
        if (!quad.isInsideSection(section)) return;

        quadMapByDimension
                .computeIfAbsent(dim, d -> new HashMap<>())
                .computeIfAbsent(section, s -> new ArrayList<>())
                .add(quad);
    }

    public static void removeQuad(Level level, InjectedQuad quad) {
        ResourceKey<Level> dim = level.dimension();
        SectionPos section = quad.section();
        Map<SectionPos, List<InjectedQuad>> dimMap = quadMapByDimension.get(dim);
        if (dimMap != null) {
            List<InjectedQuad> list = dimMap.get(section);
            if (list != null) list.remove(quad);
        }
    }

    public static void removeAllInSection(Level level, SectionPos section) {
        Map<SectionPos, List<InjectedQuad>> dimMap = quadMapByDimension.get(level.dimension());
        if (dimMap != null) {
            dimMap.remove(section);
        }
    }

    public static void clearRegion(Level level, BlockPos min, BlockPos max) {
        ResourceKey<Level> dim = level.dimension();
        Map<SectionPos, List<InjectedQuad>> dimMap = quadMapByDimension.get(dim);
        if (dimMap == null) return;

        List<SectionPos> toRemove = new ArrayList<>();
        for (Map.Entry<SectionPos, List<InjectedQuad>> entry : dimMap.entrySet()) {
            SectionPos section = entry.getKey();
            BlockPos sectionOrigin = section.origin();
            if (sectionOrigin.getX() >= min.getX() && sectionOrigin.getX() <= max.getX() &&
                    sectionOrigin.getY() >= min.getY() && sectionOrigin.getY() <= max.getY() &&
                    sectionOrigin.getZ() >= min.getZ() && sectionOrigin.getZ() <= max.getZ()) {
                toRemove.add(section);
            }
        }

        for (SectionPos s : toRemove) {
            dimMap.remove(s);
        }
    }

    public static List<InjectedQuad> getQuadsForSection(Level level, SectionPos section) {
        return quadMapByDimension
                .getOrDefault(level.dimension(), Map.of())
                .getOrDefault(section, List.of());
    }

    public static InjectedQuad faceQuad(float x1, float y1, float z1, float x2, float y2, float z2, TextureAtlasSprite sprite, Direction face, int color, int light, int overlay) {
        return switch (face) {
            case UP -> new InjectedQuad(
                    new QuadVertex(x1, y2, z2, sprite.getU0(), sprite.getV1(), color, light, overlay, 0, 1, 0),
                    new QuadVertex(x2, y2, z2, sprite.getU1(), sprite.getV1(), color, light, overlay, 0, 1, 0),
                    new QuadVertex(x2, y2, z1, sprite.getU1(), sprite.getV0(), color, light, overlay, 0, 1, 0),
                    new QuadVertex(x1, y2, z1, sprite.getU0(), sprite.getV0(), color, light, overlay, 0, 1, 0)
            );
            default -> new InjectedQuad(
                    new QuadVertex(x1, y1, z1, 0, 0, color, light, overlay, -1, 0, 0),
                    new QuadVertex(x1, y1, z2, 1, 0, color, light, overlay, -1, 0, 0),
                    new QuadVertex(x1, y2, z2, 1, 1, color, light, overlay, -1, 0, 0),
                    new QuadVertex(x1, y2, z1, 0, 1, color, light, overlay, -1, 0, 0)
            );
            // Add more directions here...
            //default -> throw new IllegalArgumentException("Unsupported face: " + face);
        };
    }

    public static void removeQuadsInRegion(Level level, BlockPos min, BlockPos max) {
        ResourceKey<Level> dim = level.dimension();
        Map<SectionPos, List<InjectedQuad>> dimMap = quadMapByDimension.get(dim);
        if (dimMap == null) return;

        for (Map.Entry<SectionPos, List<InjectedQuad>> entry : dimMap.entrySet()) {
            List<InjectedQuad> quads = entry.getValue();
            quads.removeIf(quad -> {
                double centerX = (quad.v1().x() + quad.v2().x() + quad.v3().x() + quad.v4().x()) / 4.0;
                double centerY = (quad.v1().y() + quad.v2().y() + quad.v3().y() + quad.v4().y()) / 4.0;
                double centerZ = (quad.v1().z() + quad.v2().z() + quad.v3().z() + quad.v4().z()) / 4.0;

                return centerX >= min.getX() && centerX <= max.getX()
                        && centerY >= min.getY() && centerY <= max.getY()
                        && centerZ >= min.getZ() && centerZ <= max.getZ();
            });
        }

        // Remove empty section entries
        dimMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}