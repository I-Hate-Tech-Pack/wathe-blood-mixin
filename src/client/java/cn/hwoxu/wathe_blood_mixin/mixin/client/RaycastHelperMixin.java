package cn.hwoxu.wathe_blood_mixin.mixin.client;

import cn.hwoxu.wathe_blood_mixin.client.WatheBloodFixClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wathe_blood.RaycastHelper;

/**
 * @description: TODO
 * @author: HowXu
 * @date: 2026/4/5 12:08
 */
@Environment(EnvType.CLIENT)
@Mixin(RaycastHelper.class)
public class RaycastHelperMixin {
    /**
     * @author howxu
     * @reason 799Best
     */
    @Overwrite
    public static @Nullable RaycastHelper.RaycastResult raycastToPlayerBox(PlayerEntity player, Vec3d targetPos) {
        try{
            // 这辈子没想过还要在这里设置非null
            if (targetPos == null) return null;

            // Box
            Box targetBox = new Box(
                    targetPos.x - 0.3, targetPos.y - 0.9, targetPos.z - 0.3,
                    targetPos.x + 0.3, targetPos.y + 0.9, targetPos.z + 0.3
            );


            if (player != null && player.getWorld() != null && !player.isRemoved()) {
                try {
                    Vec3d eyePos = player.getEyePos();
                    Vec3d lookVec = player.getRotationVec(1.0F);
                    Vec3d endPos = eyePos.add(lookVec.multiply(100.0));

                    RaycastHelper.RaycastResult result = raycastBox(eyePos, endPos, targetBox);

                    if (result == null || Math.abs(result.surface.y) > 0.5) {
                        double hitY = Math.max(targetBox.minY + 0.01, Math.min(targetBox.maxY - 0.01, eyePos.y));
                        Vec3d horizontalStart = new Vec3d(eyePos.x, hitY, eyePos.z);
                        Vec3d boxCenter = new Vec3d(targetBox.getCenter().x, hitY, targetBox.getCenter().z);
                        result = raycastBox(horizontalStart, boxCenter, targetBox);
                    }

                    if (result != null) return result;

                    return raycastBox(eyePos, targetBox.getCenter(), targetBox);

                } catch (Throwable e) {
                    // System.err.println("[WatheBloodFix] Caught an exception during raycast, suppressing crash.");
                    WatheBloodFixClient.Logger.error("Caught an exception during raycast, suppressing crash");
                }
            }
            try {
                Vec3d boxCenter = targetBox.getCenter();
                return raycastBox(boxCenter, boxCenter, targetBox);
            } catch (Exception e) {
                return null;
                // 我没招了
                // if null there will be nothing, but the game will not crash
            }
        }catch (Exception e){
            return null; // plz 放过青椒
        }
    }

    @Shadow
    private static @Nullable RaycastHelper.RaycastResult raycastBox(Vec3d start, Vec3d end, Box box) {
        throw new AssertionError();
    }
}
