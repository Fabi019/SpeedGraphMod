package speedgraph;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiConfigImpl extends GuiConfig {

    public GuiConfigImpl(GuiScreen parentScreen) {
        super(parentScreen,
                new ConfigElement(SpeedGraphMod.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                SpeedGraphMod.MODID,
                false,
                false,
                "SpeedGraph Configuration");
        titleLine2 = SpeedGraphMod.configFile.getName();
    }

    @Override
    public void onGuiClosed() {
        SpeedGraphMod.config.save();
    }
}
