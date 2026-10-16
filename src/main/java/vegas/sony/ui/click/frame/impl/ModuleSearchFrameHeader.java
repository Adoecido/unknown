package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.IconButtonComponent;
import vegas.sony.ui.click.component.SquareIconButtonComponent;
import vegas.sony.ui.click.component.input.ModuleSearchInputComponent;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.FrameHeaderComponent;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModuleNameSuggestionProvider;
import java.util.List;

public class ModuleSearchFrameHeader extends FrameHeaderComponent {
    private final ModuleSearchFrame frame;
    private final SquareIconButtonComponent closeButton;
    private final IconButtonComponent searchButton = new IconButtonComponent("newsearch", 0.9);
    private final ModuleSearchInputComponent searchInput;

    public void K$src$V$10w6uwu() {
        this.frame.G(null);
        ClientSettings.getFrame(ModuleSearchFrame.class).D(this.searchInput.getText());
    }

    @Override
    public double C() {
        return 16.0;
    }

    public void l$src$V$11ec2hr() {
        List<GuiComponent> components = ClientSettings.getFrame(ModuleSearchFrame.class).f();
        for (GuiComponent component : components) {
            if (component instanceof ModuleComponent) {
                ((ModuleComponent)component).collapseValueComponents();
            }
        }
    }

    @Override
    public void I() {
    }

    @Override
    public void Y(double height) {
        super.Y(height);
    }

    @Override
    public void u() {
        this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().l$src$V$1mibm4x();
    }

    public IconButtonComponent G$src$Lvegas_sony_ui_click_component_IconButtonComponent_$1pnwa51() {
        return this.searchButton;
    }

    public ModuleSearchFrameHeader(ModuleSearchFrame moduleSearchFrame) {
        super(moduleSearchFrame);
        this.closeButton = new SquareIconButtonComponent("newclose");
        this.searchInput = new ModuleSearchInputComponent(this);
        this.frame = moduleSearchFrame;
        this.setPropagateMouseEvents(true);
        this.searchButton.addClickListener(() -> ClientSettings.activeComponent = this.searchInput);
        this.closeButton.addClickListener(this::s);
        this.searchInput.addRefreshListener(new ModuleSearchFrameHeaderRefreshListener(this));
        ModuleNameSuggestionProvider suggestionProvider = new ModuleNameSuggestionProvider(true);
        suggestionProvider.setComparator(null);
        this.searchInput.setSuggestionProvider(suggestionProvider);
        this.addChildren(this.searchButton, this.closeButton, this.searchInput);
    }

    public void s() {
        this.l$src$V$11ec2hr();
        this.searchInput.setText("");
        this.K$src$V$10w6uwu();
    }

    @Override
    public void F() {
    }

    @Override
    public void H() {
        this.frame.p();
        boolean floating = ((ModeSelection)ClientSettings.INSTANCE.searchBarStyle.getValue())
                .equals(ClientSettings.INSTANCE.floatingSearchBarMode);
        this.searchInput.setUseExplicitHeight(true);
        if (floating) {
            this.searchInput.K(this.G$src$D$1b2f02a() + 6.0);
            this.searchInput.S(this.n());
            this.searchInput.o(this.A() - this.closeButton.A() - 3.0);
            this.searchInput.Y(this.L());
            if (!this.searchInput.getText().isEmpty()) {
                this.searchButton.setVisible(false);
                this.closeButton.setVisible(true);
                this.closeButton.K(this.G$src$D$1b2f02a() + this.A() - 13.0);
                this.closeButton.S(this.n() + 1.0);
                this.closeButton.o(10.0);
                this.closeButton.Y(this.L());
            } else {
                this.closeButton.setVisible(false);
                this.searchButton.setVisible(true);
                this.searchButton.K(this.G$src$D$1b2f02a() + this.A() - 14.0);
                this.searchButton.S(this.n() + 0.5);
                this.searchButton.o(10.0);
                this.searchButton.Y(this.L());
            }
            this.searchInput.setVisible(true);
            this.o(110.0);
            this.setExplicitWidth(110.0);
            this.setUseExplicitWidth(true);
            return;
        }
        boolean wasVisible = this.searchInput.V$src$Z$1xhop3l();
        this.searchInput.setVisible(false);
        this.searchButton.setVisible(false);
        this.closeButton.setVisible(false);
        if (wasVisible) {
            this.s();
        }
        this.o(22.0);
        this.setExplicitWidth(22.0);
        this.setUseExplicitWidth(true);
    }

    public ModuleSearchInputComponent A$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1efzz7n() {
        return this.searchInput;
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }
}
