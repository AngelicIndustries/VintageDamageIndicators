package industries.angelic.vintagedamageindicators.client;

import industries.angelic.vintagedamageindicators.Config;

public final class HudOverlayLayout {

    private HudOverlayLayout() {}

    /**
     * Where the panel is drawn, as wide as its content (the potion strip can widen it), before Dashboard moves it. The
     * panel's own drawing and its Dashboard element both use this.
     */
    public static OverlayRect getDrawnHudRect(int screenWidth, int screenHeight, int contentWidth) {
        int width = Math.round(contentWidth * Config.hudIndicatorSize);
        int height = Math.round(HudPreviewMath.PANEL_HEIGHT * Config.hudIndicatorSize);
        int x = Config.hudIndicatorAlignLeft ? Config.hudIndicatorPositionX
            : screenWidth - width - Config.hudIndicatorPositionX;
        int y = Config.hudIndicatorAlignTop ? Config.hudIndicatorPositionY
            : screenHeight - height - Config.hudIndicatorPositionY;
        return new OverlayRect(Math.max(0, x), Math.max(0, y), width, height);
    }

    public static final class OverlayRect {

        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        public OverlayRect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.right = left + width;
            this.bottom = top + height;
        }
    }
}
