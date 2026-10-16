package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MRenderPlayer
extends Mapping {
    private MappingField modelBipedMainField;
    public MappingMethod shouldRenderPassMethod;
    private MappingField entityModelField;
    public MappingMethod renderEquippedItemsMethod;
    public MappingMethod renderMethod;
    private MappingMethod getMainModelMethod;


    public MRenderPlayer() {
        this(MRenderManager.O());
    }

    private MRenderPlayer(String[] renderManagerControlFlowState) {
        super(MappedClasses.D0);
        if (ForgeVersion.MC_1_7_10.Y()) {
            if (ForgeVersion.MC_1_16_5.d()) {
                if (ForgeVersion.MC_1_21_10.d()) {
                    this.renderMethod = null;
                } else if (ForgeVersion.MC_1_21_4.d()) {
                    this.renderMethod = this.registerInstanceMethodForOwner(
                            MappedClasses.Fq, "render", true, Void.TYPE,
                            MappedClasses.uo, MappedClasses.DQ, MappedClasses.ZK, Integer.TYPE);
                } else {
                    this.renderMethod = this.Y("render", true, Void.TYPE,
                            MappedClasses.zt, Float.TYPE, Float.TYPE,
                            MappedClasses.DQ, MappedClasses.ZK, Integer.TYPE);
                }
                this.entityModelField = this.registerInstanceFieldForOwner(
                        MappedClasses.Fq, "entityModel", true, MappedClasses.V6);
            } else {
                this.renderMethod = this.Y("doRender", true, Void.TYPE,
                        MappedClasses.zt, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
                this.getMainModelMethod = this.Y("getMainModel", true, MappedClasses.ud);
            }
        } else {
            this.renderMethod = this.Y("doRender", true, Void.TYPE,
                    MappedClasses.zt, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
            this.modelBipedMainField = this.J("modelBipedMain", true, MappedClasses.zV);
            if (Wrapper.isNativeAvailable) {
                this.renderEquippedItemsMethod = this.Y(
                        "renderEquippedItems", true, Void.TYPE, MappedClasses.zm, Float.TYPE);
            } else {
                this.renderEquippedItemsMethod = this.Y(
                        "renderEquippedItems", true, Void.TYPE, MappedClasses.zt, Float.TYPE);
            }
            this.shouldRenderPassMethod = this.Y(
                    "shouldRenderPass", true, Integer.TYPE, MappedClasses.zt, Integer.TYPE, Float.TYPE);
        }
    }

    public Object getMainModel(Object renderPlayerHandle) {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.entityModelField.getObject(renderPlayerHandle);
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            return this.getMainModelMethod.invokeObject(renderPlayerHandle);
        }
        return this.modelBipedMainField.getObject(renderPlayerHandle);
    }
}

