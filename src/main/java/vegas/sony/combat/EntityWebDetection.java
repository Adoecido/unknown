package vegas.sony.combat;

import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.Blocks;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.World;

/** Detects a body inside cobwebs while permitting contact at their edges. */
public final class EntityWebDetection {
    private static final double EDGE_EPSILON = 1.0E-7;
    private static final double HORIZONTAL_CORE_FRACTION = 1.0 / 3.0;
    private static final double VERTICAL_EDGE_MARGIN = 0.1;

    private EntityWebDetection() {
    }

    public static boolean isInWeb(EntityLivingBase entity) {
        if (entity == null || entity.isNull()) return false;
        World world = Minecraft.theWorld();
        if (world == null || world.isNull()) return false;
        try {
            double halfWidth = entity.f$src$F$fst3ac() * 0.5;
            double minX = entity.z() - halfWidth;
            double minY = entity.N();
            double minZ = entity.h() - halfWidth;
            double maxX = entity.z() + halfWidth;
            double maxY = minY + entity.Y();
            double maxZ = entity.h() + halfWidth;
            try {
                AxisAlignedBB box = entity.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
                if (box != null && box.isNotNull()) {
                    minX = box.getMinX();
                    minY = box.getMinY();
                    minZ = box.getMinZ();
                    maxX = box.getMaxX();
                    maxY = box.getMaxY();
                    maxZ = box.getMaxZ();
                }
            } catch (Throwable unavailableBoxMapping) {
                // Position and dimensions still locate the player's body.
            }
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)
                    || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)
                    || maxX <= minX || maxY <= minY || maxZ <= minZ) return false;

            // Require the central third of the body's footprint to be inside
            // webs. A single corner/side grazing a web no longer ignores the
            // target. Checking every cell in this core also handles adjacent
            // webs without opening an attackable gap at their shared boundary.
            double insetX = (maxX - minX) * (1.0 - HORIZONTAL_CORE_FRACTION) * 0.5;
            double insetZ = (maxZ - minZ) * (1.0 - HORIZONTAL_CORE_FRACTION) * 0.5;
            double insetY = Math.min(VERTICAL_EDGE_MARGIN, (maxY - minY) * 0.25);
            minX += insetX;
            maxX -= insetX;
            minZ += insetZ;
            maxZ -= insetZ;
            minY += insetY;
            maxY -= insetY;

            Block web = null;
            try {
                web = Blocks.web();
            } catch (Throwable unavailableWebMapping) {
                // Older mapping profiles can still identify block ID 30.
            }
            boolean legacyBlockIds = ForgeVersion.getCurrentProtocolVersion()
                    <= ForgeVersion.MC_1_12_2.getProtocolVersion();
            int startX = (int)Math.floor(minX + EDGE_EPSILON);
            int startY = (int)Math.floor(minY + EDGE_EPSILON);
            int startZ = (int)Math.floor(minZ + EDGE_EPSILON);
            int endX = (int)Math.floor(maxX - EDGE_EPSILON);
            int endY = (int)Math.floor(maxY - EDGE_EPSILON);
            int endZ = (int)Math.floor(maxZ - EDGE_EPSILON);
            for (int y = startY; y <= endY; ++y) {
                boolean bodyInsideWeb = true;
                for (int x = startX; x <= endX && bodyInsideWeb; ++x) {
                    for (int z = startZ; z <= endZ; ++z) {
                        if (!isWeb(world.getBlockByPos(x, y, z), web, legacyBlockIds)) {
                            bodyInsideWeb = false;
                            break;
                        }
                    }
                }
                if (bodyInsideWeb) return true;
            }
        } catch (Throwable unavailableWorldMapping) {
            // Follow the existing target filters when the world is unavailable.
        }
        return false;
    }

    private static boolean isWeb(Block block, Block web, boolean legacyBlockIds) {
        if (block == null || block.isNull()) return false;
        if (web != null && web.isNotNull()) return block.equals(web);
        if (legacyBlockIds) {
            try {
                if (block.M() == 30) return true;
            } catch (Throwable unavailableIdMapping) {
                // Translation keys provide a second mapped fallback.
            }
        }
        try {
            String name = block.U();
            return "tile.web".equals(name) || "tile.web.name".equals(name)
                    || "block.minecraft.cobweb".equals(name)
                    || "block.minecraft.web".equals(name);
        } catch (Throwable unavailableNameMapping) {
            return false;
        }
    }
}
