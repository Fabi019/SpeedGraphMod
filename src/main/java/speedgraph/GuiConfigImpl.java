package speedgraph;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiConfigImpl extends GuiConfig {

    public GuiConfigImpl(GuiScreen parentScreen) {
        super(parentScreen,
                new ConfigElement(SpeedGraphMod.getInstance().config.getConfig().getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                SpeedGraphMod.MODID,
                false,
                false,
                "SpeedGraph Configuration");
        titleLine2 = SpeedGraphMod.getInstance().config.getConfig().getConfigFile().getName();
    }

    @Override
    public void onGuiClosed() {
        SpeedGraphMod.getInstance().config.save();
    }
}
