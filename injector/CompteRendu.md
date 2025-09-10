1. > Before le supplier (objet mais a moitier) :
```java
package com.github.forax.framework.injector;

import java.util.HashMap;
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
        var object = classObjectMap.get(type);
        if(object == null){
            throw new IllegalStateException("Not injected " + type.getName());
        }
        return type.cast(object);
    }

    public <T> void registerProvider(Class<T> type, Supplier<T> supplier){
        Objects.requireNonNull(type, "Type is null");
        Objects.requireNonNull(supplier, "Supplier is null");
        
        var result = classObjectMap.putIfAbsent(type, supplier);
        if(result != null){
            throw new IllegalStateException("Already injected " + type.getName());
        }
        
    }
}
```

2. > Avec des Suppliers
```java
package com.github.forax.framework.injector;

import jdk.jshell.execution.Util;

import java.beans.PropertyDescriptor;
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

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> implementation){
        Objects.requireNonNull(type, "Type is null");
        Objects.requireNonNull(implementation, "Implementation is null");

        var constructor = Utils.defaultConstructor(implementation);
        var properties = findInjectableProperties(type);
        registerProvider(type, () -> {
            var object = Utils.newInstance(constructor);
            for(var property : properties){
                var setter = property.getWriteMethod();
                var propertyType= property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(object, setter, value);
            }
            return object;
        });

    }
}
```
6. > Avant le changement de contructor qui a des arguments 
```java
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

    private static <T> Constructor<T> getConstructor(Class<T> type){
        var constructors = type.getConstructors();
        var arrayConstructor = Arrays.stream(constructors)
                .<Constructor<T>>filter(constructor -> {

                    return constructor.isAnnotationPresent(Inject.class);
                })
                .toList();
        if(arrayConstructor.size() == 1){
            return (Constructor<T>) arrayConstructor.getFirst();
        } else if (arrayConstructor.isEmpty()){
            return Utils.defaultConstructor(type);
        } else {
            throw new IllegalStateException("In existing constructor " + type.getName());
        }
    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> implementation){
        Objects.requireNonNull(type, "Type is null");
        Objects.requireNonNull(implementation, "Implementation is null");

        var constructor = getConstructor(implementation);

        var properties = findInjectableProperties(type);
        registerProvider(type, () -> {
            var object = Utils.newInstance(constructor);
            for(var property : properties){
                var setter = property.getWriteMethod();
                var propertyType= property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(object, setter, value);
            }
            return object;
        });

    }
}
```
6. > Apres le changement de constructeur qui prend des arguments en paramêtre des constructeurs.
   > 
