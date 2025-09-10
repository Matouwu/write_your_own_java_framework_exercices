package com.github.forax.framework.injector;

import jdk.jshell.execution.Util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class InjectorRegistry {
    private final HashMap<Class<?>, Supplier<?>> classObjectMap = new HashMap<>();

    public <T> void registerInstance(Class<T> type, T objet){
        Objects.requireNonNull(type, "Type is null.");
        Objects.requireNonNull(objet, "Objet is null.");

        registerProvider(type, () -> objet);
    }

    public <T> T lookupInstance(Class<T> type){
        Objects.requireNonNull(type, "Type is null.");
        var supplier = classObjectMap.get(type);
        if(supplier == null){
            throw new IllegalStateException("Not injected " + type.getName());
        }
        return type.cast(supplier.get());
    }

    public <T> void registerProvider(Class<T> type, Supplier<T> supplier){
        Objects.requireNonNull(type, "Type is null");
        Objects.requireNonNull(supplier, "Supplier is null");

        var result = classObjectMap.putIfAbsent(type, supplier);
        if(result != null){
            throw new IllegalStateException("Already injected " + type.getName());
        }
    }

    //package private
    static List<PropertyDescriptor> findInjectableProperties(Class<?> type){
        var beanInfo = Utils.beanInfo(type);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(property -> {
                    var setter = property.getWriteMethod();
                    return setter != null && setter.isAnnotationPresent(Inject.class);
                })
                .toList();
    }

    private static <T> Constructor<?> getInjectableConstructor(Class<T> type){
        var constructors = type.getConstructors();
        var arrayConstructor = Arrays.stream(constructors)
                .filter(constructor -> {

                    return constructor.isAnnotationPresent(Inject.class);
                })
                .toList();
        return switch (arrayConstructor.size()){
            case 0 -> Utils.defaultConstructor(type);
            case 1 -> arrayConstructor.getFirst();
            default -> throw new IllegalStateException("More then 1 constructor with @Inject " + Arrays.toString(constructors));
        };
    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> implementation){
        Objects.requireNonNull(type, "Type is null");
        Objects.requireNonNull(implementation, "Implementation is null");

        var constructor = getInjectableConstructor(implementation);
        var parameterTypes = constructor.getParameterTypes();
        var properties = findInjectableProperties(type);

        registerProvider(type, () -> {
            var paramArray = Arrays.stream(parameterTypes)
                    .map(this::lookupInstance)
                    .toArray();

            var object = Utils.newInstance(constructor, paramArray);
            for(var property : properties){
                var setter = property.getWriteMethod();
                var propertyType= property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(object, setter, value);
            }
            return type.cast(object);
        });

    }

    public void registerProviderClass(Class<?> serviceClass){
        registerProviderClass2(serviceClass);
    }
    private <T> void registerProviderClass2(Class<T> serviceClass){
        Objects.requireNonNull(serviceClass, "serviceClass is null");
        registerProviderClass(serviceClass, serviceClass);
    }
}