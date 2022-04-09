package speedgraph;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
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
    name = "Speed Graph",
    version = "1.3.0",
    useMetadata = true,
    clientSideOnly = true,
    guiFactory = "speedgraph.GuiFactoryImpl"
)
public class SpeedGraphMod {
    public static final String MODID = "speedgraph";

    public static Logger logger;
    public static Configuration config;
    public static File configFile;

    private CircularArrayList<Double> speeds;

    private Property showGraph;
    private Property smoothLines;
    private Property showCurrent;
    private Property showMax;
    private Property showAvg;
    private Property showUnit;
    private Property relativeToX;
    private Property relativeToY;
    private Property offsetX;
    private Property offsetY;
    private Property unit;
    private Property bufferSize;
    private Property graphWidth;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        configFile = new File(Loader.instance().getConfigDir(), "speedgraph.cfg");
        config = new Configuration(configFile);
        config.load();

        showGraph = config.get(Configuration.CATEGORY_GENERAL, "Show graph", true,
                "Disable to hide the mod completely");
        bufferSize = config.get(Configuration.CATEGORY_GENERAL, "Buffer size", 200,
                "Max speed entries for the graph", 10, 1000);
        graphWidth = config.get(Configuration.CATEGORY_GENERAL, "Graph width", 200,
                "Max speed entries for the graph", 100, 1000);
        smoothLines = config.get(Configuration.CATEGORY_GENERAL, "Smooth lines", true,
                "Render smooth lines (Performance cost!)");
        showCurrent = config.get(Configuration.CATEGORY_GENERAL, "Show current", true,
                "Show the current speed below the graph");
        showMax = config.get(Configuration.CATEGORY_GENERAL, "Show max", true,
                "Show the current maximum speed");
        showAvg = config.get(Configuration.CATEGORY_GENERAL, "Show avg", true,
                "Shows the average speed");
        showUnit = config.get(Configuration.CATEGORY_GENERAL, "Show unit", true,
                "Speed unit besides the current speed");
        relativeToX = config.get(Configuration.CATEGORY_GENERAL, "Horizontal align", "CENTER",
                "Alignment for the X-Offset", new String[]{"LEFT", "RIGHT", "CENTER"});
        offsetX = config.get(Configuration.CATEGORY_GENERAL, "X-Offset", -100,
                "X-Offset");
        relativeToY = config.get(Configuration.CATEGORY_GENERAL, "Vertical align", "DOWN",
                "Alignment for the Y-Offset", new String[]{"UP", "DOWN", "CENTER"});
        offsetY = config.get(Configuration.CATEGORY_GENERAL, "Y-Offset", -70,
                "Y-Offset");
        unit = config.get(Configuration.CATEGORY_GENERAL, "Unit", "u/t",
                "Unit to display", new String[]{"u/t", "m/s"});

        bufferSize.setRequiresWorldRestart(true);

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

        speeds = new CircularArrayList<>(bufferSize.getInt());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.type != TickEvent.Type.CLIENT || !showGraph.getBoolean() || speeds == null)
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
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !showGraph.getBoolean() || speeds == null)
            return;

        int posX = event.resolution.getScaledWidth() / 2;
        int posY = event.resolution.getScaledHeight() / 2;

        switch (relativeToX.getString()) {
            case "LEFT":
                posX = 0;
                break;
            case "RIGHT":
                posX = event.resolution.getScaledWidth();
                break;
        }

        switch (relativeToY.getString()) {
            case "UP":
                posY = 0;
                break;
            case "DOWN":
                posY = event.resolution.getScaledHeight();
                break;
        }

        posX += offsetX.getInt();
        posY += offsetY.getInt();

        double avgSpeed = 0;
        double maxSpeed = 0;
        double newestSpeed = 0;

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        if (smoothLines.getBoolean())
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        Double prevSpeed = speeds.getOldest();
        double increment = graphWidth.getInt() / (double) speeds.size();
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
        if (showMax.getBoolean()) {
            GL11.glVertex2d(posX, posY - maxSpeed * 100);
            GL11.glVertex2d(posX + graphWidth.getInt(), posY - maxSpeed * 100);
        }
        if (showAvg.getBoolean()) {
            GL11.glVertex2d(posX, posY - avgSpeed * 100);
            GL11.glVertex2d(posX + graphWidth.getInt(), posY - avgSpeed * 100);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();

        GlStateManager.color(1, 1, 1, 1);

        if (showCurrent.getBoolean()) {
            double displaySpeed = Math.round(newestSpeed * 1000);
            if (unit.getString().equals("m/s"))
                displaySpeed /= 50;
            String text = ("" + displaySpeed).replaceAll("\\.0+$", "");
            if (showUnit.getBoolean())
                text += " " + unit.getString();
            int size = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, posX - size / 2.f + 100, posY + 10, 0xFFFFFFFF);
        }

        markerText(posX, posY, maxSpeed, showMax);
        markerText(posX, posY, avgSpeed, showAvg);

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
    }

    private void markerText(int posX, int posY, double avgSpeed, Property prop) {
        if (prop.getBoolean()) {
            double displaySpeed = Math.round(avgSpeed * 1000);
            if (unit.getString().equals("m/s"))
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

}
