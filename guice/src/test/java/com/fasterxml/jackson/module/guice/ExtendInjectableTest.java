package com.fasterxml.jackson.module.guice;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;

/**
 */
public class ExtendInjectableTest
{
    final ConstructorDependency constructorInjected = new ConstructorDependency();
    final ConstructorDependency constructorInjectedWithCustomAnnotation = new ConstructorDependency();
    final ConstructorDependency constructorInjectedWithGuiceAnnotation = new ConstructorDependency();
    final ConstructorDependency constructorInjectedWithJavaAnnotation = new ConstructorDependency();
    final FieldDependency fieldInjected = new FieldDependency();
    final FieldDependency fieldInjectedWithCustomAnnotation = new FieldDependency();
    final FieldDependency fieldInjectedWithGuiceAnnotation = new FieldDependency();
    final FieldDependency fieldInjectedWithJavaAnnotation = new FieldDependency();
    final MethodDependency methodInjected = new MethodDependency();
    final MethodDependency methodInjectedWithCustomAnnotation = new MethodDependency();
    final MethodDependency methodInjectedWithGuiceAnnotation = new MethodDependency();
    final MethodDependency methodInjectedWithJavaAnnotation = new MethodDependency();

    @Test
    public void testModulesRegisteredThroughInjectionWithKey() throws Exception
    {
        final Injector injector = Guice.createInjector(new ObjectMapperModule(),
                                                       new Module() {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(ConstructorDependency.class).toInstance(constructorInjected);
                    binder.bind(ConstructorDependency.class).annotatedWith(Ann.class).toInstance(constructorInjectedWithCustomAnnotation);
                    binder.bind(ConstructorDependency.class).annotatedWith(Names.named("guice")).toInstance(constructorInjectedWithGuiceAnnotation);
                    binder.bind(ConstructorDependency.class).annotatedWith(Names.named("javax")).toInstance(constructorInjectedWithJavaAnnotation);
                    binder.bind(FieldDependency.class).toInstance(fieldInjected);
                    binder.bind(FieldDependency.class).annotatedWith(Ann.class).toInstance(fieldInjectedWithCustomAnnotation);
                    binder.bind(FieldDependency.class).annotatedWith(Names.named("guice")).toInstance(fieldInjectedWithGuiceAnnotation);
                    binder.bind(FieldDependency.class).annotatedWith(Names.named("javax")).toInstance(fieldInjectedWithJavaAnnotation);
                    binder.bind(MethodDependency.class).toInstance(methodInjected);
                    binder.bind(MethodDependency.class).annotatedWith(Ann.class).toInstance(methodInjectedWithCustomAnnotation);
                    binder.bind(MethodDependency.class).annotatedWith(Names.named("guice")).toInstance(methodInjectedWithGuiceAnnotation);
                    binder.bind(MethodDependency.class).annotatedWith(Names.named("javax")).toInstance(methodInjectedWithJavaAnnotation);
                    binder.bind(String.class).toInstance("injectedString");
                }
        });

        /* First of all, just get an InjectableBean out of guice to make sure it's correct (test the test) */
        verifyInjections("From Guice's Injector", injector.getInstance(InjectableBean.class));

        /* Now let's try injections via our ObjectMapper (plus some values) */
        final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

        final ReadableBean bean = mapper.readValue("{\"constructor_value\":\"myConstructor\",\"field_value\":\"myField\",\"method_value\":\"myMethod\"}", ReadableBean.class);

        Assert.assertEquals("myConstructor", bean.constructorValue);
        Assert.assertEquals("myMethod",      bean.methodValue);
        Assert.assertEquals("myField",       bean.fieldValue);

        verifyInjections("From Jackson's ObjectMapper", bean);

        final DefaultStringInjectableBean defaultStringInjectableBean = mapper.readValue("{\"injectedString\":\"jsonProvidedString\"}", DefaultStringInjectableBean.class);
        final UseIncludeTrueStringInjectableBean useIncludeTrueStringInjectableBean = mapper.readValue("{\"injectedString\":\"jsonProvidedString\"}", UseIncludeTrueStringInjectableBean.class);
        final UseIncludeFalseStringInjectableBean useIncludeFalseStringInjectableBean = mapper.readValue("{\"injectedString\":\"jsonProvidedString\"}", UseIncludeFalseStringInjectableBean.class);

        Assert.assertEquals("jsonProvidedString", defaultStringInjectableBean.injectedString);
        Assert.assertEquals("jsonProvidedString", useIncludeTrueStringInjectableBean.injectedString);
        Assert.assertEquals("injectedString", useIncludeFalseStringInjectableBean.injectedString);

    }

    private void verifyInjections(String message, InjectableBean injected)
    {
        Assert.assertSame(message, constructorInjected,                     injected.constructorInjected);
        Assert.assertSame(message, constructorInjectedWithJavaAnnotation,   injected.constructorInjectedWithJavaAnnotation);
        Assert.assertSame(message, constructorInjectedWithGuiceAnnotation,  injected.constructorInjectedWithGuiceAnnotation);
        Assert.assertSame(message, constructorInjectedWithCustomAnnotation, injected.constructorInjectedWithCustomAnnotation);

        Assert.assertSame(message, methodInjected,                     injected.methodInjected);
        Assert.assertSame(message, methodInjectedWithJavaAnnotation,   injected.methodInjectedWithJavaAnnotation);
        Assert.assertSame(message, methodInjectedWithGuiceAnnotation,  injected.methodInjectedWithGuiceAnnotation);
        Assert.assertSame(message, methodInjectedWithCustomAnnotation, injected.methodInjectedWithCustomAnnotation);

        Assert.assertSame(message, fieldInjected,                     injected.fieldInjected);
        Assert.assertSame(message, fieldInjectedWithJavaAnnotation,   injected.fieldInjectedWithJavaAnnotation);
        Assert.assertSame(message, fieldInjectedWithGuiceAnnotation,  injected.fieldInjectedWithGuiceAnnotation);
        Assert.assertSame(message, fieldInjectedWithCustomAnnotation, injected.fieldInjectedWithCustomAnnotation);
    }

    /* ===================================================================== */
    /* SUPPORT CLASSES                                                       */
    /* ===================================================================== */

    static class ReadableBean extends InjectableBean {

        @JsonProperty("field_value")
        String fieldValue;
        String methodValue;
        final String constructorValue;

        @JsonCreator
        private ReadableBean(@JacksonInject ConstructorDependency constructorInjected,
                             @JacksonInject @javax.inject.Named("javax") ConstructorDependency constructorInjectedWithJavaAnnotation,
                             @JacksonInject @com.google.inject.name.Named("guice") ConstructorDependency constructorInjectedWithGuiceAnnotation,
                             @JacksonInject @Ann ConstructorDependency constructorInjectedWithCustomAnnotation,
                             @JsonProperty("constructor_value") String constructorValue) {
            super(constructorInjected,
                    constructorInjectedWithJavaAnnotation,
                    constructorInjectedWithGuiceAnnotation,
                    constructorInjectedWithCustomAnnotation);
            this.constructorValue = constructorValue;
        }

        @JsonProperty("method_value")
        public void setMethodValue(String methodValue) {
            this.methodValue = methodValue;
        }
    }

    static class DefaultStringInjectableBean extends InjectableBean {
        @JsonCreator
        private DefaultStringInjectableBean(@JacksonInject ConstructorDependency constructorInjected,
                             @JacksonInject @javax.inject.Named("javax") ConstructorDependency constructorInjectedWithJavaAnnotation,
                             @JacksonInject @com.google.inject.name.Named("guice") ConstructorDependency constructorInjectedWithGuiceAnnotation,
                             @JacksonInject @Ann ConstructorDependency constructorInjectedWithCustomAnnotation,
                             @JacksonInject String injectedString) {
            super(constructorInjected,
                    constructorInjectedWithJavaAnnotation,
                    constructorInjectedWithGuiceAnnotation,
                    constructorInjectedWithCustomAnnotation);
            this.injectedString = injectedString;
        }

        @JacksonInject
        @JsonProperty
        String injectedString;
    }

    static class UseIncludeTrueStringInjectableBean extends InjectableBean {
        @JsonCreator
        private UseIncludeTrueStringInjectableBean(@JacksonInject ConstructorDependency constructorInjected,
                                            @JacksonInject @javax.inject.Named("javax") ConstructorDependency constructorInjectedWithJavaAnnotation,
                                            @JacksonInject @com.google.inject.name.Named("guice") ConstructorDependency constructorInjectedWithGuiceAnnotation,
                                            @JacksonInject @Ann ConstructorDependency constructorInjectedWithCustomAnnotation,
                                            @JacksonInject(useInput = OptBoolean.TRUE) String injectedString) {
            super(constructorInjected,
                    constructorInjectedWithJavaAnnotation,
                    constructorInjectedWithGuiceAnnotation,
                    constructorInjectedWithCustomAnnotation);
            this.injectedString = injectedString;
        }

        @JacksonInject(useInput = OptBoolean.TRUE)
        @JsonProperty
        String injectedString;
    }

    static class UseIncludeFalseStringInjectableBean extends InjectableBean {
        @JsonCreator
        private UseIncludeFalseStringInjectableBean(@JacksonInject ConstructorDependency constructorInjected,
                                            @JacksonInject @javax.inject.Named("javax") ConstructorDependency constructorInjectedWithJavaAnnotation,
                                            @JacksonInject @com.google.inject.name.Named("guice") ConstructorDependency constructorInjectedWithGuiceAnnotation,
                                            @JacksonInject @Ann ConstructorDependency constructorInjectedWithCustomAnnotation,
                                            @JacksonInject(useInput = OptBoolean.FALSE) String injectedString) {
            super(constructorInjected,
                    constructorInjectedWithJavaAnnotation,
                    constructorInjectedWithGuiceAnnotation,
                    constructorInjectedWithCustomAnnotation);
            this.injectedString = injectedString;
        }

        @JacksonInject(useInput = OptBoolean.FALSE)
        @JsonProperty
        String injectedString;

    }

    /* ===================================================================== */

    static class ConstructorDependency {};
    static class MethodDependency {};
    static class FieldDependency {};

    static class InjectableBean
    {
        @Inject
        FieldDependency fieldInjected;
        MethodDependency methodInjected;
        final ConstructorDependency constructorInjected;

        @JacksonInject
        @Inject
        @javax.inject.Named("javax")
        FieldDependency fieldInjectedWithJavaAnnotation;
        MethodDependency methodInjectedWithJavaAnnotation;
        final ConstructorDependency constructorInjectedWithJavaAnnotation;

        @JacksonInject
        @Inject
        @com.google.inject.name.Named("guice")
        FieldDependency fieldInjectedWithGuiceAnnotation;
        MethodDependency methodInjectedWithGuiceAnnotation;
        final ConstructorDependency constructorInjectedWithGuiceAnnotation;

        @JacksonInject
        @Inject
        @Ann
        FieldDependency fieldInjectedWithCustomAnnotation;
        MethodDependency methodInjectedWithCustomAnnotation;
        final ConstructorDependency constructorInjectedWithCustomAnnotation;

        @Inject // this is simply to make sure we *can* build this correctly
        protected InjectableBean(ConstructorDependency constructorInjected,
                                 @javax.inject.Named("javax")  ConstructorDependency constructorInjectedWithJavaAnnotation,
                                 @com.google.inject.name.Named("guice") ConstructorDependency constructorInjectedWithGuiceAnnotation,
                                 @Ann ConstructorDependency constructorInjectedWithCustomAnnotation)
        {
            this.constructorInjected = constructorInjected;
            this.constructorInjectedWithJavaAnnotation = constructorInjectedWithJavaAnnotation;
            this.constructorInjectedWithGuiceAnnotation = constructorInjectedWithGuiceAnnotation;
            this.constructorInjectedWithCustomAnnotation = constructorInjectedWithCustomAnnotation;
        }

        @JacksonInject
        @Inject // not annotated
        private void inject1(MethodDependency dependency)
        {
            this.methodInjected = dependency;
        }

        @JacksonInject
        @Inject
        private void inject2(@javax.inject.Named("javax") MethodDependency dependency)
        {
            this.methodInjectedWithJavaAnnotation = dependency;
        }

        @JacksonInject
        @Inject
        private void inject3(@com.google.inject.name.Named("guice") MethodDependency dependency)
        {
            this.methodInjectedWithGuiceAnnotation = dependency;
        }

        @JacksonInject
        @Inject
        private void inject4(@Ann MethodDependency dependency)
        {
            this.methodInjectedWithCustomAnnotation = dependency;
        }

    }
}
