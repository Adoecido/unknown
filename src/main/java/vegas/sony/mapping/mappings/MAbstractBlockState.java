package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.utils.datas.BlockData;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MAbstractBlockState
extends Mapping {
    private MappingMethod getShapeMethod;
    private MappingMethod isSuffocatingMethod;

    public MAbstractBlockState() {
        this(BlockData.W());
    }

    private MAbstractBlockState(String[] controlFlowState) {
        super(MappedClasses.Fj);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_20_6.d()) {
                this.getShapeMethod = this.Y("getShape", true, MappedClasses.la, new Class[]{MappedClasses.zJ, MappedClasses.lf});
            } else {
                this.getShapeMethod = this.Y("func_196954_c", Wrapper.isNativeAvailable, MappedClasses.la, new Class[]{MappedClasses.zJ, MappedClasses.lf});
                this.isSuffocatingMethod = this.Y("func_229980_m_", Wrapper.isNativeAvailable, Boolean.TYPE, new Class[]{MappedClasses.zJ, MappedClasses.lf});
            }
            return;
        }
        this.isSuffocatingMethod = this.Y("func_229980_m_", Wrapper.isNativeAvailable, Boolean.TYPE, new Class[]{MappedClasses.zJ, MappedClasses.lf});
    }

    public Object getShape(Object blockState, Object blockReader, Object blockPosition) {
        return this.getShapeMethod.invokeObject(blockState, blockReader, blockPosition);
    }

    public boolean isSuffocating(Object blockState, Object blockReader, Object blockPosition) {
        return this.isSuffocatingMethod.invokeBoolean(blockState, blockReader, blockPosition);
    }

}
