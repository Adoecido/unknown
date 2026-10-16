package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.Enemy;
import vegas.sony.friend.ui.EnemySettingsAddEnemyInputComponent;
import vegas.sony.friend.ui.EnemySettingsEntryRow;
import vegas.sony.friend.ui.EnemySettingsFrameToggleHeaderComponent;
import vegas.sony.friend.ui.EnemySettingsRemoveEntryClickHandler;
import vegas.sony.ui.click.component.ColorDividerComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.component.value.ColorValueEditorComponent;
import vegas.sony.ui.click.frame.CollapsibleFrame;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.FrameHeaderComponent;

public class EnemySettingsFrame
extends Frame
implements CollapsibleFrame {
    private BooleanToggleComponent spoofAliasToggle;
    private BooleanToggleComponent recolorVisualsToggle;
    private ColorValueEditorComponent enemyColorEditor;
    private BooleanToggleComponent useAliasToggle;
    private ColorDividerComponent colorDivider;
    private BooleanToggleComponent useEnemiesToggle;
    private boolean expanded = true;

    static ColorValueEditorComponent getEnemyColorEditor(EnemySettingsFrame frame) {
        return frame.enemyColorEditor;
    }

    @Override
    public void v() {
    }

    @Override
    public boolean q() {
        return this.expanded;
    }

    public EnemySettingsFrame() {
        this.colorDivider = new ColorDividerComponent(EnemySettingsFrame.J.l);
        this.setDisabledOverlayColor(EnemySettingsFrame.J.i);
        this.K(300.0);
        this.S(100.0);
        this.setVisible(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.Y(new EnemySettingsFrameToggleHeaderComponent(this, this, "newfriends", "Enemies"));
        this.enemyColorEditor = new ColorValueEditorComponent(SonyVegas.INSTANCE.getEnemyManager().enemyColor);
        this.recolorVisualsToggle = new BooleanToggleComponent(SonyVegas.INSTANCE.getFriendManager().recolorVisuals);
        this.useEnemiesToggle = new BooleanToggleComponent(SonyVegas.INSTANCE.getEnemyManager().useEnemies);
        this.useAliasToggle = new BooleanToggleComponent(SonyVegas.INSTANCE.getFriendManager().useAlias);
        this.spoofAliasToggle = new BooleanToggleComponent(SonyVegas.INSTANCE.getFriendManager().spoofAlias);
        this.enemyColorEditor.setDisabledOverlayColor(EnemySettingsFrame.J.r);
        this.recolorVisualsToggle.setDisabledOverlayColor(EnemySettingsFrame.J.r);
        this.useEnemiesToggle.setDisabledOverlayColor(EnemySettingsFrame.J.r);
        this.useAliasToggle.setDisabledOverlayColor(EnemySettingsFrame.J.r);
        this.spoofAliasToggle.setDisabledOverlayColor(EnemySettingsFrame.J.r);
        this.enemyColorEditor.setVisible(false);
        this.recolorVisualsToggle.setVisible(false);
        this.useEnemiesToggle.setVisible(false);
        this.useAliasToggle.setVisible(false);
        this.spoofAliasToggle.setVisible(false);
        this.colorDivider.setVisible(false);
    }

    public void refreshEntries() {
        this.removeMarkedChildren();
        this.addChildren(this.enemyColorEditor, this.recolorVisualsToggle, this.useEnemiesToggle, this.useAliasToggle, this.spoofAliasToggle, this.colorDivider);
        this.h(new EnemySettingsAddEnemyInputComponent("Username / Alias"), new Object[0]);
        for (Enemy enemy : SonyVegas.INSTANCE.getEnemyManager().getEnemies()) {
            this.h(new EnemySettingsEntryRow(enemy).setDeleteActionListener(new EnemySettingsRemoveEntryClickHandler(this, enemy)), new Object[0]);
        }
        this.l$src$V$1mibm4x();
    }

    static BooleanToggleComponent getUseEnemiesToggle(EnemySettingsFrame frame) {
        return frame.useEnemiesToggle;
    }

    @Override
    public void Y() {
    }

    @Override
    public void w() {
        this.expanded = !this.expanded;
        for (GuiComponent guiComponent : this.f()) {
            if (guiComponent instanceof FrameHeaderComponent) continue;
            guiComponent.setVisible(this.expanded);
        }
        this.l$src$V$1mibm4x();
    }

    static BooleanToggleComponent getRecolorVisualsToggle(EnemySettingsFrame frame) {
        return frame.recolorVisualsToggle;
    }

    static ColorDividerComponent getColorDivider(EnemySettingsFrame frame) {
        return frame.colorDivider;
    }


    static BooleanToggleComponent getSpoofAliasToggle(EnemySettingsFrame frame) {
        return frame.spoofAliasToggle;
    }

    static BooleanToggleComponent getUseAliasToggle(EnemySettingsFrame frame) {
        return frame.useAliasToggle;
    }

    @Override
    public String getName() {
        return "Enemies";
    }
}

