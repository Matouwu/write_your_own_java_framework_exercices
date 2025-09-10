package com.github.forax.framework.mapper;

import jdk.jfr.Percentage;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JSONWriter {
    private interface JSONFuntion {
        String apply(JSONWriter writer, Object instance);
    }

    //private record Property(String prefix, Method getter){}

    private static final class PropertyDescriptorClassValue extends ClassValue<List<JSONFuntion>>{
        @Override
        protected List<JSONFuntion> computeValue(Class<?> type){
            var beanInfo = Utils.beanInfo(type);
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                    .filter(propertyDescriptor -> !propertyDescriptor.getName().equals("class"))
                    .<JSONFuntion>map(propertyDescriptor -> {
                        var name = propertyDescriptor.getName();
                        var getter = propertyDescriptor.getReadMethod();
                        var prefix = "\"" + name + "\": ";
                        return (JSONWriter writer, Object o) -> {
                            var value = Utils.invokeMethod(o,getter);
                            return prefix + writer.toJSON(value);
                        };
                    })
                    .toList();
        }
    }
    private static final PropertyDescriptorClassValue CLASS_VALUE = new PropertyDescriptorClassValue();

    private String toJSONObject(Object o){
        var propertyDescriptors = CLASS_VALUE.get(o.getClass());
        //var beanInfo = Utils.beanInfo(o.getClass());
        return propertyDescriptors.stream()
                .map(jsonFuntion -> jsonFuntion.apply(this, o))
                .collect(Collectors.joining(", ","{","}"));
    }

    public String toJSON(Object o){
        return switch(o){
            case Boolean b -> "" + b;
            case Integer i -> "" + i;
            case String s -> '"' + s + "\"";
            case Double d -> "" + d;
            case null -> "null";
            case Object _ -> {
                yield toJSONObject(o);
            }

            //no need if case Object qui prend tout : default -> throw new IllegalArgumentException("Unexpected value:" + o);
        };
    }
}

