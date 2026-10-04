package pl.autodrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoDripClient implements ClientModInitializer {

    public enum Speed {
        SLOW("Wolno", 10, 1, 20),
        FAST("Szybko", 3, 2, 10),
        VERY_FAST("Bardzo szybko", 0, 8, 4);

        public final String label;
        final int cooldown;   // przerwa między otwarciami (ticki)
        final int perTick;    // ile trapdorów max w jednym ticku
        final int closeDelay; // po ilu tickach zamknąć trapdora

        Speed(String label, int cooldown, int perTick, int closeDelay) {
            this.label = label;
            this.cooldown = cooldown;
            this.perTick = perTick;
            this.closeDelay = closeDelay;
        }

        public Speed next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final int RANGE = 4;
    private static final int DROP_HEIGHT = 12;

    public static boolean enabled = false;
    public static boolean autoClose = true;
    public static Speed speed = Speed.FAST;

    public static int triggers = 0;
    private static int cooldown = 0;

    private static KeyBinding toggleKey;
    private static KeyBinding guiKey;

    private static class Pending {
        final BlockPos pos;
        int left;
        Pending(BlockPos pos, int left) { this.pos = pos; this.left = left; }
    }
    private static final List<Pending> pending = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "AutoDrip - włącz/wyłącz", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "AutoDrip"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "AutoDrip - menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "AutoDrip"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // HUD ze statusem w rogu ekranu
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (!enabled) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            ctx.drawTextWithShadow(mc.textRenderer,
                    "AutoDrip [" + speed.label + "]  trafienia: " + triggers, 4, 4, 0x55FF55);
        });
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            mc.player.sendMessage(Text.literal("AutoDrip: " + (enabled ? "ON" : "OFF")), true);
        }
        while (guiKey.wasPressed()) {
            mc.setScreen(new AutoDripScreen());
        }
        if (!enabled) { pending.clear(); return; }

        // automatyczne zamykanie trapdorów po zrzucie
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.left > 0) continue;
            BlockState s = mc.world.getBlockState(p.pos);
            if (s.getBlock() instanceof TrapdoorBlock && s.get(TrapdoorBlock.OPEN)) click(mc, p.pos);
            it.remove();
        }

        if (cooldown > 0) { cooldown--; return; }

        int opened = 0;
        BlockPos origin = mc.player.getBlockPos();
        scan:
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -2; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!(state.getBlock() instanceof TrapdoorBlock)) continue;
                    if (state.get(TrapdoorBlock.OPEN)) continue;
                    if (!mc.world.getBlockState(pos.down()).isOf(Blocks.POINTED_DRIPSTONE)) continue;
                    if (!hasTargetBelow(mc, pos)) continue;

                    click(mc, pos);
                    triggers++;
                    opened++;
                    if (autoClose) pending.add(new Pending(pos, speed.closeDelay));
                    if (opened >= speed.perTick) break scan;
                }
            }
        }
        if (opened > 0) cooldown = speed.cooldown;
    }

    private static void click(MinecraftClient mc, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
    }

    private static boolean hasTargetBelow(MinecraftClient mc, BlockPos pos) {
        Box column = new Box(pos.getX(), pos.getY() - DROP_HEIGHT, pos.getZ(),
                pos.getX() + 1, pos.getY(), pos.getZ() + 1);
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.getBoundingBox().intersects(column)) return true;
        }
        return false;
    }
}
