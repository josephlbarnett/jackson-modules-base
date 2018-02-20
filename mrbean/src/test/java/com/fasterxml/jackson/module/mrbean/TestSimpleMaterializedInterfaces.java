package com.fasterxml.jackson.module.mrbean;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.util.ClassUtil;

import static org.junit.Assert.assertArrayEquals;

public class TestSimpleMaterializedInterfaces
    extends BaseTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    public interface Bean {
        public int getX();
        public String getA();
    }

    public interface BeanWithY extends Bean
    {
        public int getY();
    }
    
    public interface PartialBean {
        public boolean isOk();
        // and then non-getter/setter one:
        public int foobar();
    }
    
    public interface BeanHolder {
        public Bean getBean();
    }

    // then invalid one; conflicting setter/getter types
    public interface InvalidBean {
        public int getX();
        public void setX(String value);
    }

    public interface ArrayBean {
        public int[] getValues();
        public String[] getWords();
        public void setWords(String[] words);
    }

    // how about non-public classes?
    interface NonPublicBean {
        public abstract int getX();
    }
    
    /*
    /**********************************************************
    /* Unit tests, low level
    /**********************************************************
     */

    /**
     * First test verifies that bean builder works as expected
     */
    public void testLowLevelMaterializer() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        DeserializationConfig config = new ObjectMapper().deserializationConfig();
        Class<?> impl = _materializeRawType(mat, config, Bean.class);
        assertNotNull(impl);
        assertTrue(Bean.class.isAssignableFrom(impl));
        // also, let's instantiate to make sure:
        Object ob = ClassUtil.createInstance(impl, false);
        // and just for good measure do actual cast
        Bean bean = (Bean) ob;
        // call something to ensure generation worked...
        assertNull(bean.getA());

        // Also: let's verify that we can handle dup calls:
        Class<?> impl2 = _materializeRawType(mat, config, Bean.class);
        assertNotNull(impl2);
        assertSame(impl, impl2);
    }

    public void testLowLevelMaterializerFailOnIncompatible() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        DeserializationConfig config = new ObjectMapper().deserializationConfig();
        try {
            _materializeRawType(mat, config, InvalidBean.class);
            fail("Expected exception for incompatible property types");
        } catch (IllegalArgumentException e) {
            verifyException(e, "incompatible types");
        }
    }

    public void testLowLevelMaterializerFailOnUnrecognized() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        //  by default early failure is disabled, enable:
        mat.enable(AbstractTypeMaterializer.Feature.FAIL_ON_UNMATERIALIZED_METHOD);
        DeserializationConfig config = new ObjectMapper().deserializationConfig();
        try {
            _materializeRawType(mat, config, PartialBean.class);
            fail("Expected exception for unrecognized method");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Unrecognized abstract method 'foobar'");
        }
    }

    private Class<?> _materializeRawType(AbstractTypeMaterializer mat,
            DeserializationConfig config, Class<?> cls)
    {
        JavaType type = config.constructType(cls);
        return mat.materializeRawType(config,
                AnnotatedClassResolver.resolve(config, type, config));
    }

    /*
    /**********************************************************
    /* Unit tests, higher level
    /**********************************************************
     */

    /**
     * Test simple leaf-level bean with 2 implied _beanProperties
     */
    public void testSimpleInteface() throws Exception
    {
        ObjectMapper mapper = newMrBeanMapper();
        Bean bean = mapper.readValue("{\"a\":\"value\",\"x\":123 }", Bean.class);
        assertNotNull(bean);
        assertEquals("value", bean.getA());
        assertEquals(123, bean.getX());
    }

    /**
     * Then one bean holding a reference to another (leaf-level) bean
     */
    public void testBeanHolder() throws Exception
    {
        ObjectMapper mapper = newMrBeanMapper();
        BeanHolder holder = mapper.readValue("{\"bean\":{\"a\":\"b\",\"x\":-4 }}", BeanHolder.class);
        assertNotNull(holder);
        Bean bean = holder.getBean();
        assertNotNull(bean);
        assertEquals("b", bean.getA());
        assertEquals(-4, bean.getX());
    }    
    
    public void testArrayInterface() throws Exception
    {
        ObjectMapper mapper = newMrBeanMapper();
        ArrayBean bean = mapper.readValue("{\"values\":[1,2,3], \"words\": [ \"cool\", \"beans\" ] }",
                ArrayBean.class);
        assertNotNull(bean);
        assertArrayEquals(new int[] { 1, 2, 3} , bean.getValues());
        assertArrayEquals(new String[] { "cool", "beans" } , bean.getWords());
    }

    public void testSubInterface() throws Exception
    {
        ObjectMapper mapper = newMrBeanMapper();
        BeanWithY bean = mapper.readValue("{\"a\":\"b\",\"x\":1, \"y\":2 }", BeanWithY.class);
        assertNotNull(bean);
        assertEquals("b", bean.getA());
        assertEquals(1, bean.getX());
        assertEquals(2, bean.getY());
    }
    
    /*
    /**********************************************************
    /* Unit tests, higher level, error handling
    /**********************************************************
     */

    /**
     * Test to verify that materializer will by default create exception-throwing methods
     * for "unknown" abstract methods
     */
    public void testPartialBean() throws Exception
    {
        AbstractTypeMaterializer mat = new AbstractTypeMaterializer();
        // ensure that we will only get deferred error methods
        mat.disable(AbstractTypeMaterializer.Feature.FAIL_ON_UNMATERIALIZED_METHOD);
        ObjectMapper mapper = ObjectMapper.builder()
                .addModule(new MrBeanModule(mat))
                .build();
        PartialBean bean = mapper.readValue("{\"ok\":true}", PartialBean.class);
        assertNotNull(bean);
        assertTrue(bean.isOk());
        // and then exception
        try {
            bean.foobar();
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Unimplemented method 'foobar'");
        }
    }

    // fail gracefully if super type not public
    public void testNonPublic() throws Exception
    {
        ObjectMapper mapper = newMrBeanMapper();
        try {
            mapper.readValue("{\"x\":3}", NonPublicBean.class);
            fail("Should have thrown an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "is not public");
        }
    }    
    
}
