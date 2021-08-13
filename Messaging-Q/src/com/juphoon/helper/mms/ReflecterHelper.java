package com.juphoon.helper.mms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ReflecterHelper {

    public static Object invokeStaticMethod(String className, String methodName, Object[] paramObjects, Class<?>[] paramTypes)
            throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, IllegalAccessException {
        Class<?> clazz = Class.forName(className);
        return findMethodBestMatch(clazz, methodName, paramTypes).invoke(clazz, paramObjects);
    }

    /**
     *
     * @param clazz
     * @param methodName
     * @param paramTypes
     * @return
     * @throws NoSuchMethodException
     */
    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                // ignore and try find in superclass
            }
        }
        throw new NoSuchMethodException("Cannot find such method " + methodName + " in class " + clazz.getName());
    }

    public static Object invokeMethod(Object receiver, String methodName, Class<?>[] paramTypes, Object[] args)
            throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        System.out.println("DEBUG::MLJ invokeMethod receiver.getClass()="+receiver.getClass());
        return findMethodBestMatch(receiver.getClass(), methodName, paramTypes).invoke(receiver, args);
    }

}
