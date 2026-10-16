package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventEntityJoinWorld;
import vegas.sony.event.impl.EventLivingUpdate;
import vegas.sony.event.impl.EventPlayerUseItem;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.ArrayListModule;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.TruncatedTextComponent;
import vegas.sony.ui.click.component.layout.PaddedComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.ui.click.frame.impl.target.TargetInfoCombatStatStripComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoDistanceStatStripComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoEntityPreviewComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoHealthBarComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoLiveEntityPreviewComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoPositiveStatStripComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoPreviewHealthBarComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoResettingCombatStatStripComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoSettingsFrame;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.StringUtils;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPotion;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import java.awt.Color;
import java.util.function.Predicate;

public class TargetInfoPreviewComponent
extends PaddedComponent
implements EventListener {
    private static final Color BACKGROUND_COLOR = new Color(8, 8, 8, 230);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 24);
    private static final Color NAME_COLOR = new Color(242, 242, 244);
    private static final Color DEFAULT_ACCENT_COLOR = new Color(255, 72, 78);
    private static final java.util.regex.Pattern COLOR_CODE =
            java.util.regex.Pattern.compile("§.");
    private static final java.util.regex.Pattern CLAN_TAG =
            java.util.regex.Pattern.compile("\\s*\\[[^\\[\\]]{1,16}\\]\\s*");
    private int pendingLocalHealingPotions;
    private final TargetInfoResettingCombatStatStripComponent comboStrip;
    private final SpacerComponent nameSpacer;
    private final FlowLayoutComponent healthRow;
    private int lastTargetEntityId = -1;
    private final PanelComponent entityHeaderPanel;
    private final TimerUtil targetTimeoutTimer = new TimerUtil();
    private final PanelComponent statsPanel;
    private final FlowLayoutComponent nameRow;
    private EntityLivingBase target;
    private final TargetInfoCombatStatStripComponent potsUsedStrip;
    private final TargetInfoDistanceStatStripComponent damageStrip;
    private final FlowLayoutComponent rootLayout;
    private final TargetInfoSettingsFrame settingsFrame;
    private final TargetInfoEntityPreviewComponent entityPreview;
    private final FlowLayoutComponent statsRow;
    private final TargetInfoPositiveStatStripComponent hitsStrip;
    private final TargetInfoHealthBarComponent healthBar;

    @EventHandler
    public void onUpdate(EventLivingUpdate eventLivingUpdate) {
        if (!this.isActive()) {
            return;
        }
        if (this.getTarget() == null || this.getTarget().isNull() || Minecraft.thePlayer().isNull()) {
            return;
        }
        if (Minecraft.thePlayer().getDistanceToEntity(this.getTarget()) > 6.0f) {
            return;
        }
        if (eventLivingUpdate.getEntity().getObject().equals(Minecraft.thePlayer().getObject())) {
            this.hitsStrip.decrement();
            this.comboStrip.decrementCombo();
        }
        if (eventLivingUpdate.getEntity().getObject().equals(this.getTarget().getObject())) {
            this.hitsStrip.increment();
            this.comboStrip.incrementCombo();
        }
    }

    public boolean isPreviewMode() {
        return !ClientSettings.INSTANCE.inputEnabled;
    }

    public EntityLivingBase getTarget() {
        return this.isPreviewMode() ? Minecraft.thePlayer() : this.target;
    }

    public TargetInfoPreviewComponent(TargetInfoSettingsFrame targetInfoSettingsFrame) {
        super(10.0, new FlowLayoutComponent(100.0));
        this.entityHeaderPanel = new PanelComponent(100.0, 10.0);
        this.healthRow = new FlowLayoutComponent(100.0);
        this.nameRow = new FlowLayoutComponent(90.0);
        this.nameSpacer = new SpacerComponent(12.0, 12.0);
        this.entityPreview = new TargetInfoLiveEntityPreviewComponent(this, 12.0, 12.0);
        this.healthBar = new TargetInfoPreviewHealthBarComponent(this, 100, 4);
        this.entityPreview.setBackgroundColor(new Color(20, 20, 20, 225));
        this.statsPanel = new PanelComponent(100.0, 14.0);
        this.statsRow = new FlowLayoutComponent(100.0);
        this.damageStrip = new TargetInfoDistanceStatStripComponent();
        this.hitsStrip = new TargetInfoPositiveStatStripComponent();
        this.potsUsedStrip = new TargetInfoCombatStatStripComponent();
        this.comboStrip = new TargetInfoResettingCombatStatStripComponent();
        this.settingsFrame = targetInfoSettingsFrame;
        this.rootLayout = (FlowLayoutComponent)super.H$src$Lvegas_sony_ui_click_component_GuiComponent_$kfnvup();
        this.rootLayout.h(new SpacerComponent(1.0, 2.0), new Object[0]);
        this.statsPanel.h(this.statsRow, "wrap, alignright");
        this.rootLayout.h(this.statsPanel, new Object[0]);
        this.nameRow.h(this.nameSpacer, new Object[0]);
        this.entityPreview.setFrame(targetInfoSettingsFrame);
        this.entityHeaderPanel.h(this.entityPreview, new Object[0]);
        this.entityHeaderPanel.h(this.nameRow, new Object[0]);
        this.rootLayout.addChildren(this.entityHeaderPanel);
        this.rootLayout.h(new SpacerComponent(100.0, 8.0), new Object[0]);
        this.healthBar.setFrame(targetInfoSettingsFrame);
        this.healthRow.h(this.healthBar, new Object[0]);
        this.rootLayout.addChildren(this.healthRow);
        this.rootLayout.setShowDisabledOverlay(false);
        this.entityHeaderPanel.setShowDisabledOverlay(false);
        this.nameRow.setShowDisabledOverlay(false);
        this.healthRow.setShowDisabledOverlay(false);
        this.statsPanel.setShowDisabledOverlay(false);
        this.nameRow.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().Q(true);
        this.entityHeaderPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().Q(true);
        this.statsRow.setShowDisabledOverlay(false);
        this.hitsStrip.setFrame(targetInfoSettingsFrame);
        this.potsUsedStrip.setFrame(targetInfoSettingsFrame);
        this.comboStrip.setFrame(targetInfoSettingsFrame);
        this.damageStrip.setFrame(targetInfoSettingsFrame);
        this.statsRow.h(this.hitsStrip, new Object[0]);
        this.statsRow.h(this.potsUsedStrip, new Object[0]);
        this.statsRow.h(this.comboStrip, new Object[0]);
        this.statsRow.h(this.damageStrip, new Object[0]);
        EventBus.getInstance().registerListener(this, new Predicate[0]);
    }

    private void updateStatVisibility() {
        this.hitsStrip.setVisible(this.settingsFrame.getSettings().hitsComparator.getEffectiveValue());
        this.potsUsedStrip.setVisible(this.settingsFrame.getSettings().potsUsedComparator.getEffectiveValue());
        this.comboStrip.setVisible(this.settingsFrame.getSettings().comboCounter.getEffectiveValue());
        this.damageStrip.setVisible(this.settingsFrame.getSettings().damageComparator.getEffectiveValue());
        boolean bl = false;
        for (GuiComponent guiComponent : this.statsRow.f()) {
            if (!guiComponent.V$src$Z$1xhop3l()) continue;
            bl = true;
            break;
        }
        this.statsPanel.setVisible(bl);
    }

    private double getContentY() {
        return this.n() + 2.0;
    }

    public void setTarget(EntityLivingBase target) {
        this.target = target;
    }

    @Override
    public void u() {
        Entity entity;
        RayTraceResult rayTraceResult;
        if (!this.isActive()) {
            return;
        }
        this.updateDamageComparison();
        this.updateStatVisibility();
        if (this.targetTimeoutTimer.hasTimeElapsed(1000L) && this.getTarget() != null && !this.getTarget().isNull() && this.getTarget().w$src$F$15l9epb() <= 0.0f) {
            this.showTarget(null);
        }
        if (this.targetTimeoutTimer.hasTimeElapsed(3000L)) {
            this.showTarget(null);
        }
        if (this.settingsFrame.getSettings().showHovered.getEffectiveValue().booleanValue() && (rayTraceResult = Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0()).isNotNull() && (entity = rayTraceResult.getEntity()).isNotNull() && entity.isInstance(MappedClasses.zm) && !entity.isInstance(MappedClasses.FT)) {
            this.showTarget(new EntityLivingBase(entity));
        }
    }

    private void showTarget(EntityLivingBase entityLivingBase) {
        if (entityLivingBase == null) {
            this.setTarget(null);
            return;
        }
        this.rootLayout.setVisible(true);
        this.targetTimeoutTimer.reset();
        if (this.getTarget() != null && this.getTarget().equals(entityLivingBase)) {
            return;
        }
        int n = entityLivingBase.S();
        this.setTarget(entityLivingBase);
        this.healthBar.setEntity(entityLivingBase);
        if (n != this.lastTargetEntityId) {
            this.hitsStrip.setComparisonValue(0);
            this.potsUsedStrip.setComparisonValue(0);
            this.comboStrip.setCombo(0);
        }
        this.lastTargetEntityId = n;
        this.H(true);
    }

    @EventHandler
    public void onPostAttack(EventPostAttack eventPostAttack) {
        if (!this.isActive()) {
            this.showTarget(null);
            return;
        }
        Entity entity = eventPostAttack.getTarget();
        if (entity.isInstance(MappedClasses.zm) && !entity.isInstance(MappedClasses.FT)) {
            this.showTarget(new EntityLivingBase(entity));
        }
    }

    public String getTargetName() {
        if (this.getTarget() == null) {
            return "";
        }
        return this.getTarget().getName();
    }

    private boolean isActive() {
        return this.settingsFrame.y$src$Z$1f55jvh() && Minecraft.theWorld().isNotNull();
    }

    private void updateDamageComparison() {
        if (this.getTarget() == null) {
            return;
        }
        this.damageStrip.setComparisonValue((int)RotationUtil.y(Minecraft.thePlayer(), this.getTarget()));
    }

    @Override
    public void H() {
        this.renderPreview();
    }

    private void renderPreview() {
        this.rootLayout.setVisible(this.getTarget() != null || this.isPreviewMode());
        if (!this.rootLayout.V$src$Z$1xhop3l()) {
            return;
        }
        this.entityPreview.setVisible(this.getTarget().isInstance(MappedClasses.Yl));
        if (this.statsPanel.V$src$Z$1xhop3l()) {
            this.N(5.0);
        } else {
            this.N(10.0);
        }
        this.H(true);
        double cardX = this.G$src$D$1b2f02a();
        double cardY = this.getContentY();
        Color background = this.settingsFrame.applyDefaultEditorAlpha(BACKGROUND_COLOR);
        Color border = this.settingsFrame.applyDefaultEditorAlpha(BORDER_COLOR);
        Color accent = this.settingsFrame.applyDefaultEditorAlpha(getHudAccentColor());
        GuiRenderPrimitives.e(cardX, cardY, this.A(), this.L(), background,
                false, 6.0f, 1.0f);
        GuiRenderPrimitives.P(cardX, cardY, this.A(), this.L(), border,
                6.0f, 0.65f, 1.0f);
        GuiRenderPrimitives.e(cardX + 5.0, cardY + 7.0, 2.0,
                Math.max(4.0, this.L() - 14.0), accent, false, 1.0f, 1.0f);
        String string = this.getTargetName();
        string = cleanPlayerName(string);
        String string2 = StringUtils.l(string);
        if (string2.isEmpty()) {
            string = "\u00a77(Empty Name)";
        }
        TruncatedTextComponent truncatedTextComponent = new TruncatedTextComponent(
                string, "...", 80.0, 1.05,
                this.settingsFrame.applyDefaultEditorAlpha(NAME_COLOR), true);
        double d = this.entityPreview.V$src$Z$1xhop3l() ? this.entityPreview.n() + this.entityPreview.L() / 2.0 - truncatedTextComponent.getTextHeight() / 2.0 : this.entityHeaderPanel.n() + this.entityHeaderPanel.L() / 2.0 - truncatedTextComponent.getTextHeight() / 2.0;
        truncatedTextComponent.renderAt(this.nameSpacer.G$src$D$1b2f02a() + (double)(this.entityPreview.V$src$Z$1xhop3l() ? 5 : 2), d);
    }

    static Color getHudAccentColor() {
        try {
            ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager()
                    .getMod(ArrayListModule.class);
            if (arrayList != null) {
                Color configured = arrayList.getHudAccentColor();
                return new Color(configured.getRed(), configured.getGreen(),
                        configured.getBlue(), 255);
            }
        } catch (Throwable ignored) { }
        return DEFAULT_ACCENT_COLOR;
    }

    static String cleanPlayerName(String raw) {
        if (raw == null) return "";
        String plain = COLOR_CODE.matcher(raw).replaceAll("");
        return CLAN_TAG.matcher(plain).replaceAll(" ").trim();
    }

    @EventHandler
    public void onPlayerUseItem(EventPlayerUseItem eventPlayerUseItem) {
        if (!this.isActive()) {
            return;
        }
        if (this.getTarget() == null || this.getTarget().isNull()) {
            return;
        }
        ItemStack itemStack = eventPlayerUseItem.getItemStack();
        if (itemStack.isNotNull() && MappedClasses.Di.isInstance(itemStack.getItem().getObject()) && ItemStackScoreUtil.i(itemStack)) {
            ++this.pendingLocalHealingPotions;
        }
    }

    @EventHandler
    public void onEntityJoinWorld(EventEntityJoinWorld eventEntityJoinWorld) {
        if (!this.isActive()) {
            return;
        }
        if (this.getTarget() == null || this.getTarget().isNull() || Minecraft.thePlayer().isNull()) {
            return;
        }
        if (!eventEntityJoinWorld.getEntity().isInstance(MappedClasses.Zf)) {
            return;
        }
        EntityPotion entityPotion = new EntityPotion(eventEntityJoinWorld.getEntity());
        if (entityPotion.getPotion().isNull() || !ItemStackScoreUtil.i(entityPotion.getPotion())) {
            return;
        }
        if (this.pendingLocalHealingPotions > 0) {
            this.potsUsedStrip.decrement();
            --this.pendingLocalHealingPotions;
        } else {
            this.potsUsedStrip.increment();
        }
    }

    @Override
    public void I() {
        this.c();
    }

    @Override
    public SmoothFontRenderer getFontRenderer(double scale) {
        ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        if (arrayList != null) {
            return arrayList.getVisualFont((int) Math.max(8, Math.round(scale * 14.0)), false);
        }
        return super.getFontRenderer(scale);
    }

    @Override
    public SmoothFontRenderer getAlternateFontRenderer(double scale) {
        ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        if (arrayList != null) {
            return arrayList.getVisualFont((int) Math.max(8, Math.round(scale * 14.0)), true);
        }
        return super.getAlternateFontRenderer(scale);
    }
}
