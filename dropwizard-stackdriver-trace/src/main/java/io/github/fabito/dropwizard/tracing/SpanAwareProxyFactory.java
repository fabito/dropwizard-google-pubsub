package io.github.fabito.dropwizard.tracing;

import com.google.api.client.util.Maps;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.guice.annotation.Labeler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

import javax.inject.Provider;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * A factory for creating proxies for components that use Hibernate data access objects
 * outside Jersey resources.
 * <p>A created proxy will be aware of the {@link Span} annotation
 * on the original class methods and will around them.</p>
 */
public class SpanAwareProxyFactory {

    private final TraceBundle<?> traceBundle;

    public SpanAwareProxyFactory(TraceBundle<?> bundle) {
        traceBundle = bundle;
    }

    /**
     * Creates a new <b>@Span</b> aware proxy of a class with the default constructor.
     *
     * @param clazz the specified class definition
     * @param <T>   the type of the class
     * @return a new proxy
     */
    public <T> T create(Class<T> clazz) {
        return create(clazz, new Class<?>[]{}, new Object[]{});
    }

    /**
     * Creates a new <b>@Span</b> aware proxy of a class with an one-parameter constructor.
     *
     * @param clazz                the specified class definition
     * @param constructorParamType the type of the constructor parameter
     * @param constructorArguments the argument passed to the constructor
     * @param <T>                  the type of the class
     * @return a new proxy
     */
    public <T> T create(Class<T> clazz, Class<?> constructorParamType, Object constructorArguments) {
        return create(clazz, new Class<?>[]{constructorParamType}, new Object[]{constructorArguments});
    }

    /**
     * Creates a new <b>@Span</b> aware proxy of a class with a complex constructor.
     *
     * @param clazz                 the specified class definition
     * @param constructorParamTypes the types of the constructor parameters
     * @param constructorArguments  the arguments passed to the constructor
     * @param <T>                   the type of the class
     * @return a new proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz, Class<?>[] constructorParamTypes, Object[] constructorArguments) {
        final ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);

        try {
            final Proxy proxy = (Proxy) (constructorParamTypes.length == 0 ?
                    factory.createClass().newInstance() :
                    factory.create(constructorParamTypes, constructorArguments));
            proxy.setHandler(new TracerSpanMethodHandler(traceBundle.getTracer(), new Provider<Map<String, Labeler>>() {
                @Override
                public Map<String, Labeler> get() {
                    return Maps.newHashMap();
                }
            }, ""));
            return (T) proxy;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new IllegalStateException("Unable to create a proxy for the class '" + clazz + "'", e);
        }
    }
}