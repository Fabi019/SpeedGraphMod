package speedgraph;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.regex.Pattern;

public class Config {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[a-fA-F0-9]{6}$");

    private final Configuration config;

    private final Property showGraph;
    private final Property smoothLines;
    private final Property showCurrent;
    private final Property showMax;
    private final Property showAvg;
    private final Property showUnit;
    private final Property relativeToX;
    private final Property relativeToY;
    private final Property offsetX;
    private final Property offsetY;
    private final Property unit;
    private final Property bufferSize;
    private final Property graphWidth;
    private final Property graphColorHex;
    private final Property avgColorHex;
    private final Property maxColorHex;
    private final Property lineThickness;

    public Config(File configFile) {
        config = new Configuration(configFile);
        config.load();

        graphColorHex = config.get(Configuration.CATEGORY_GENERAL, "Graph color", "#FFFFFF");
        avgColorHex = config.get(Configuration.CATEGORY_GENERAL, "Max line color", "#FF0000");
        maxColorHex = config.get(Configuration.CATEGORY_GENERAL, "Avg line color", "#FF0000");

        lineThickness = config.get(Configuration.CATEGORY_GENERAL, "Line thickness", 1.,
                "Thickness of the graph line", 1., 5.);
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

        graphColorHex.setValidationPattern(HEX_COLOR_PATTERN);
        graphColorHex.setRequiresWorldRestart(true);
        avgColorHex.setValidationPattern(HEX_COLOR_PATTERN);
        avgColorHex.setRequiresWorldRestart(true);
        maxColorHex.setValidationPattern(HEX_COLOR_PATTERN);
        maxColorHex.setRequiresWorldRestart(true);

    }

    public Configuration getConfig() {
        return config;
    }

    public void save() {
        if (this.config.hasChanged()) {
            this.config.save();
        }
    }

    public boolean getShowGraph() {
        return showGraph.getBoolean();
    }

    public boolean getSmoothLines() {
        return smoothLines.getBoolean();
    }

    public boolean getShowCurrent() {
        return showCurrent.getBoolean();
    }

    public boolean getShowMax() {
        return showMax.getBoolean();
    }

    public boolean getShowAvg() {
        return showAvg.getBoolean();
    }

    public boolean getShowUnit() {
        return showUnit.getBoolean();
    }

    public String getRelativeToX() {
        return relativeToX.getString();
    }

    public String getRelativeToY() {
        return relativeToY.getString();
    }

    public Property getOffsetX() {
        return offsetX;
    }

    public Property getOffsetY() {
        return offsetY;
    }

    public String getUnit() {
        return unit.getString();
    }

    public int getBufferSize() {
        return bufferSize.getInt();
    }

    public int getGraphWidth() {
        return graphWidth.getInt();
    }

    public String getGraphColorHex() {
        return graphColorHex.getString();
    }

    public String getAvgColorHex() {
        return avgColorHex.getString();
    }

    public String getMaxColorHex() {
        return maxColorHex.getString();
    }

    public double getLineThickness() {
        return lineThickness.getDouble();
    }
}
