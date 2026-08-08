package com.tom.wardrobe.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminWriteAccessAnnotationTest {

    private static final List<Class<?>> PUBLIC_READ_ADMIN_WRITE_CONTROLLERS = List.of(
            ClothesController.class,
            BrandController.class,
            TypeController.class,
            SizeController.class
    );

    @Test
    void readEndpointsAreExplicitlyPublic() {
        PUBLIC_READ_ADMIN_WRITE_CONTROLLERS.forEach(controller -> {
            List<Method> readMethods = methodsAnnotatedWith(controller, GetMapping.class);

            assertTrue(
                    readMethods.stream().allMatch(method -> method.isAnnotationPresent(SaIgnore.class)),
                    () -> controller.getSimpleName() + " has a read endpoint without @SaIgnore: "
                            + methodNamesWithoutAnnotation(readMethods, SaIgnore.class)
            );
        });
    }

    @Test
    void writeEndpointsRequireAdminRole() {
        PUBLIC_READ_ADMIN_WRITE_CONTROLLERS.forEach(controller -> {
            List<Method> writeMethods = Arrays.stream(controller.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(PostMapping.class)
                            || method.isAnnotationPresent(PutMapping.class)
                            || method.isAnnotationPresent(DeleteMapping.class))
                    .toList();

            assertTrue(
                    writeMethods.stream().allMatch(method -> method.isAnnotationPresent(SaCheckRole.class)),
                    () -> controller.getSimpleName() + " has a write endpoint without @SaCheckRole: "
                            + methodNamesWithoutAnnotation(writeMethods, SaCheckRole.class)
            );
            writeMethods.forEach(method -> assertEquals(
                    "admin",
                    method.getAnnotation(SaCheckRole.class).value()[0],
                    () -> controller.getSimpleName() + "#" + method.getName() + " should require admin role"
            ));
        });
    }

    private static List<Method> methodsAnnotatedWith(Class<?> controller, Class<?> annotationType) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType.asSubclass(java.lang.annotation.Annotation.class)))
                .toList();
    }

    private static String methodNamesWithoutAnnotation(
            List<Method> methods,
            Class<? extends java.lang.annotation.Annotation> annotationType
    ) {
        return methods.stream()
                .filter(method -> !method.isAnnotationPresent(annotationType))
                .map(Method::getName)
                .collect(Collectors.joining(", "));
    }
}
