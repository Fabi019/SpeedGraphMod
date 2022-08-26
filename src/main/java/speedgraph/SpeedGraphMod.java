package speedgraph;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.lang.reflect.Field;

@Mod(
        modid = SpeedGraphMod.MODID,
        useMetadata = true,
        clientSideOnly = true,
        guiFactory = "speedgraph.GuiFactoryImpl"
)
public class SpeedGraphMod {
    public static final String MODID = "speedgraph";

    @Mod.Instance
    public static SpeedGraphMod instance;

    public Logger logger;
    public Config config;

    private CircularArrayList<Double> speeds;
    private final float[] lineColor = new float[] {1, 1, 1};
    private final float[] avgLineColor = new float[] {1, 0, 0};
    private final float[] maxLineColor = new float[] {1, 0, 0};

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        File configFile = new File(Loader.instance().getConfigDir(), "speedgraph.cfg");
        config = new Config(configFile);

        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity != Minecraft.getMinecraft().thePlayer)
            return;

        speeds = new CircularArrayList<>(config.getBufferSize());

        updateColor(config.getGraphColorHex(), lineColor);
        updateColor(config.getAvgColorHex(), avgLineColor);
        updateColor(config.getMaxColorHex(), maxLineColor);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.type != TickEvent.Type.CLIENT || !config.getShowGraph() || speeds == null)
            return;

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            double dX = Math.abs(player.posX - player.prevPosX);
            double dZ = Math.abs(player.posZ - player.prevPosZ);
            double speed = Math.sqrt(dX * dX + dZ * dZ);
            speeds.insert(speed);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !config.getShowGraph() || speeds == null)
            return;

        int posX = event.resolution.getScaledWidth() / 2;
        int posY = event.resolution.getScaledHeight() / 2;

        switch (config.getRelativeToX()) {
            case "LEFT":
                posX = 0;
                break;
            case "RIGHT":
                posX = event.resolution.getScaledWidth();
                break;
        }

        switch (config.getRelativeToY()) {
            case "UP":
                posY = 0;
                break;
            case "DOWN":
                posY = event.resolution.getScaledHeight();
                break;
        }

        posX += config.getOffsetX().getInt();
        posY += config.getOffsetY().getInt();

        double avgSpeed = 0;
        double maxSpeed = 0;
        double newestSpeed = 0;

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color(lineColor[0], lineColor[1], lineColor[2], 1);
        if (config.getSmoothLines())
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth((float) config.getLineThickness());
        GL11.glBegin(GL11.GL_LINE_STRIP);
        Double prevSpeed = speeds.getOldest();
        double increment = config.getGraphWidth() / (double) speeds.size();
        for (int entry = 0; entry < speeds.size(); entry++) {
            Double speed = speeds.get(entry);
            if (speed == null || prevSpeed == null)
                continue;
            if (maxSpeed < speed)
                maxSpeed = speed;
            avgSpeed += speed;
            prevSpeed = speed;
            newestSpeed = speed;
            GL11.glVertex2d(posX + (entry * increment) - getRenderPartialTicks(), posY - (speed * 100));
        }
        avgSpeed = avgSpeed / speeds.size();
        GL11.glEnd();
        GlStateManager.color(1, 0, 0, 0.3f);
        GL11.glBegin(GL11.GL_LINES);
        if (config.getShowMax()) {
            GlStateManager.color(maxLineColor[0], maxLineColor[1], maxLineColor[2], 1);
            GL11.glVertex2d(posX, posY - maxSpeed * 100);
            GL11.glVertex2d(posX + config.getGraphWidth(), posY - maxSpeed * 100);
        }
        if (config.getShowAvg()) {
            GlStateManager.color(avgLineColor[0], avgLineColor[1], avgLineColor[2], 1);
            GL11.glVertex2d(posX, posY - avgSpeed * 100);
            GL11.glVertex2d(posX + config.getGraphWidth(), posY - avgSpeed * 100);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.resetColor();

        if (config.getShowCurrent()) {
            double displaySpeed = Math.round(newestSpeed * 1000);
            if (config.getUnit().equals("m/s"))
                displaySpeed /= 50;
            String text = ("" + displaySpeed).replaceAll("\\.0+$", "");
            if (config.getShowUnit())
                text += " " + config.getUnit();
            int size = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, posX - size / 2.f + 100, posY + 10, 0xFFFFFFFF);
        }

        markerText(posX, posY, maxSpeed, config.getShowMax());
        markerText(posX, posY, avgSpeed, config.getShowAvg());

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
    }

    private void markerText(int posX, int posY, double avgSpeed, boolean show) {
        if (show) {
            double displaySpeed = Math.round(avgSpeed * 1000);
            if (config.getUnit().equals("m/s"))
                displaySpeed /= 50;
            String text = ("" + displaySpeed).replaceAll("\\.0+$", "");
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, posX - 20, posY - Math.round(avgSpeed * 100) - 3.5f, 0xFFFFFFFF);
        }
    }

    private Timer timer;

    private float getRenderPartialTicks() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (timer == null) {
                for (Field f : mc.getClass().getDeclaredFields()) {
                    if (f.getType() == Timer.class) {
                        f.setAccessible(true);
                        timer = (Timer) f.get(mc);
                        break;
                    }
                }
            }
            assert timer != null;
            return timer.renderPartialTicks;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 1f;
    }

    private void updateColor(String color, float[] target) {
        try {
            int colorInt = Integer.decode(color);
            target[0] = ((colorInt >> 16) & 0xff) / 255f; // red
            target[1] = ((colorInt >> 8) & 0xff) / 255f; // green
            target[2] = (colorInt & 0xff) / 255f; // blue
        } catch (NumberFormatException e) {
            logger.warn("Invalid color provided!", e);
        }
    }

    public static SpeedGraphMod getInstance() {
        return instance;
    }
}
