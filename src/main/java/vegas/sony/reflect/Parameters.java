package vegas.sony.reflect;

import vegas.sony.reflect.ParameterResolver;

public class Parameters {
    public static boolean checkParameterTypes(Class<?>[] parameterTypes, Class<?> returnType, String descriptor) {
        return ParameterResolver.matchesDescriptor(parameterTypes, returnType, descriptor);
    }
}
