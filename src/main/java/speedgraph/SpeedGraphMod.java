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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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

    private CircularBuffer<Float> speeds;
    private final float[] lineColor = new float[] {1, 1, 1};
    private final float[] avgLineColor = new float[] {1, 0, 0};
    private final float[] maxLineColor = new float[] {1, 0, 0};

    private float avgSpeed = 0;
    private float maxSpeed = 0;
    private float newestSpeed = 0;

    private final int graphListIndex = GL11.glGenLists(2);

    private FloatBuffer vertexBuffer;

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

        ByteBuffer bb = ByteBuffer.allocateDirect(config.getBufferSize() * 2 * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        speeds = new CircularBuffer<>(config.getBufferSize());

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

            speeds.insert((float) Math.sqrt(dX * dX + dZ * dZ));

            updateRenderList();
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !config.getShowGraph() || speeds == null)
            return;

        float posX = event.resolution.getScaledWidth() / 2f;
        float posY = event.resolution.getScaledHeight() / 2f;

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

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 0);

        GlStateManager.pushMatrix();
        if (config.getInterpolation()) {
            float prev = (config.getGraphWidth() / (float) speeds.size()) * 2;
            float offset = prev - prev * getRenderPartialTicks();
            GL11.glTranslatef(offset, 0, 0);
        }
        GlStateManager.enableAlpha();
        GL11.glCallList(graphListIndex);
        GlStateManager.disableAlpha();
        GlStateManager.popMatrix();

        GL11.glCallList(graphListIndex+1);

        if (config.getShowCurrent()) {
            float displaySpeed = Math.round(newestSpeed * 1000);
            if (config.getUnit().equals("m/s"))
                displaySpeed /= 50;
            String text = ("" + displaySpeed).replaceAll("\\.0+$", "");
            if (config.getShowUnit())
                text += " " + config.getUnit();
            int size = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, -size / 2.f + 100,  10, 0xFFFFFFFF);
        }

        markerText(maxSpeed, config.getShowMax());
        markerText(avgSpeed, config.getShowAvg());

        GlStateManager.popMatrix();
    }

    private void updateRenderList() {
        calculateGraphVertices();

        // Graph
        GL11.glNewList(graphListIndex, GL11.GL_COMPILE);
        {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (config.getSmoothLines())
                GL11.glEnable(GL11.GL_LINE_SMOOTH);

            GL11.glLineWidth((float) config.getLineThickness());
            GL11.glColor3f(lineColor[0], lineColor[1], lineColor[2]);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glVertexPointer(2, 0, vertexBuffer);
            GL11.glDrawArrays(GL11.GL_LINE_STRIP, 0, speeds.size() - 1);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        GL11.glEndList();

        // Marker
        GL11.glNewList(graphListIndex+1, GL11.GL_COMPILE);
        {
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glBegin(GL11.GL_LINES);
            if (config.getShowMax()) {
                GL11.glColor3f(maxLineColor[0], maxLineColor[1], maxLineColor[2]);
                GL11.glVertex2d(0, - maxSpeed * 100);
                GL11.glVertex2d(config.getGraphWidth(), - maxSpeed * 100);
            }
            if (config.getShowAvg()) {
                GL11.glColor3f(avgLineColor[0], avgLineColor[1], avgLineColor[2]);
                GL11.glVertex2d(0, - avgSpeed * 100);
                GL11.glVertex2d(config.getGraphWidth(), - avgSpeed * 100);
            }
            GL11.glEnd();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        GL11.glEndList();
    }

    private void calculateGraphVertices() {
        avgSpeed = maxSpeed = newestSpeed = 0;
        vertexBuffer.clear();
        float increment = config.getGraphWidth() / (float) speeds.size();
        int index = 0;
        for (Float speed : speeds) {
            if (maxSpeed < speed)
                maxSpeed = speed;
            avgSpeed += speed;
            newestSpeed = speed;
            vertexBuffer.put(index++ * increment);
            vertexBuffer.put(-(speed * 100));
        }
        avgSpeed = avgSpeed / speeds.size();
        vertexBuffer.flip();
    }

    private void markerText(double speed, boolean show) {
        if (!show) {
            return;
        }
        double displaySpeed = Math.round(speed * 1000);
        if (config.getUnit().equals("m/s"))
            displaySpeed /= 50;
        String text = ("" + displaySpeed).replaceAll("\\.0+$", "");
        int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, - width - 5, - Math.round(speed * 100) - 3.5f, 0xFFFFFFFF);
    }

    private Timer timer;

    {
        Minecraft mc = Minecraft.getMinecraft();
        for (Field f : mc.getClass().getDeclaredFields()) {
            if (f.getType() == Timer.class) {
                f.setAccessible(true);
                try {
                    timer = (Timer) f.get(mc);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private float getRenderPartialTicks() {
        try {
            return timer.renderPartialTicks;
        } catch (NullPointerException e) {
            return 1;
        }
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
