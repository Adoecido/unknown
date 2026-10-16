package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.TextInputComponentBase;
import vegas.sony.ui.click.component.value.SearchBlockListDropdownLayer;
import vegas.sony.ui.unmap.SearchBlock;
import vegas.sony.unmap.ColorUtil;

public class SearchBlockListAddInputComponent
extends TextInputComponentBase {
    private Runnable afterAdd;
    private boolean resetFocusAnimation = false;

    @Override
    public void F() {
        if (this.resetFocusAnimation) {
            this.borderAnimation.J();
            this.resetFocusAnimation = false;
        }
        super.F();
    }

    public SearchBlockListAddInputComponent(String placeholder, Runnable afterAdd) {
        super(placeholder);
        this.setShowDisabledOverlay(false);
        this.afterAdd = afterAdd;
    }

    public SearchBlockListAddInputComponent(String placeholder) {
        super(placeholder);
        this.setShowDisabledOverlay(false);
    }

    @Override
    public double getComponentWidth() {
        if (this.afterAdd != null) {
            return this.A();
        }
        return 110.0;
    }

    @Override
    public double C() {
        return 20.0;
    }


    @Override
    public double x() {
        return 110.0;
    }

    @Override
    public void submit() {
        if (!this.hasNonBlankText()) {
            this.setText("");
            return;
        }
        SearchBlock searchBlock = new SearchBlock(this.getText(), ColorUtil.createRandomReadableColor().getRGB());
        SonyVegas.INSTANCE.getSearch().addSearchBlock(searchBlock);
        if (this.afterAdd != null) {
            this.afterAdd.run();
        } else {
            ((SearchBlockListDropdownLayer)this.getParentFrameComponent()).refreshContents();
        }
        this.setText("");
    }
}
