package com.fasterxml.jackson.module.blackbird.deser.inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.module.blackbird.BlackbirdTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class TestInjectables extends BlackbirdTestBase
{
    static class InjectedBean
    {
        @JacksonInject
        protected String stuff;

        @JacksonInject("myId")
        protected String otherStuff;

        protected long third;
        
        public int value;

        @JacksonInject
        public void injectThird(long v) {
            third = v;
        }
    }

    static class CtorBean {
        protected String name;
        protected int age;
        
        public CtorBean(@JacksonInject String n, @JsonProperty("age") int a)
        {
            name = n;
            age = a;
        }
    }

    static class CtorBean2 {
        protected String name;
        protected Integer age;
        
        public CtorBean2(@JacksonInject String n, @JacksonInject("number") Integer a)
        {
            name = n;
            age = a;
        }
    }

    static class IssueGH471Bean {

        protected final Object constructorInjected;
        protected final String constructorValue;

        @JacksonInject("field_injected") protected Object fieldInjected;
        @JsonProperty("field_value")     protected String fieldValue;

        protected Object methodInjected;
        protected String methodValue;

        public int x;
        
        @JsonCreator
        private IssueGH471Bean(@JacksonInject("constructor_injected") Object constructorInjected,
                               @JsonProperty("constructor_value") String constructorValue) {
            this.constructorInjected = constructorInjected;
            this.constructorValue = constructorValue;
        }

        @JacksonInject("method_injected")
        private void setMethodInjected(Object methodInjected) {
            this.methodInjected = methodInjected;
        }

        @JsonProperty("method_value")
        public void setMethodValue(String methodValue) {
            this.methodValue = methodValue;
        }
    }

    // [databind#77]
    static class TransientBean {
        @JacksonInject("transient")
        transient Object injected;

        public int value;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();
    
    @Test
    public void testSimple() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "stuffValue")
            .addValue("myId", "xyz")
            .addValue(Long.TYPE, Long.valueOf(37))
            );
        InjectedBean bean = mapper.readValue("{\"value\":3}", InjectedBean.class);
        assertEquals(3, bean.value);
        assertEquals("stuffValue", bean.stuff);
        assertEquals("xyz", bean.otherStuff);
        assertEquals(37L, bean.third);
    }
    
    @Test
    public void testWithCtors() throws Exception
    {
        CtorBean bean = MAPPER.readerFor(CtorBean.class)
            .with(new InjectableValues.Std()
                .addValue(String.class, "Bubba"))
            .readValue("{\"age\":55}");
        assertEquals(55, bean.age);
        assertEquals("Bubba", bean.name);
    }

    @Test
    public void testTwoInjectablesViaCreator() throws Exception
    {
        CtorBean2 bean = MAPPER.readerFor(CtorBean2.class)
                .with(new InjectableValues.Std()
                    .addValue(String.class, "Bob")
                    .addValue("number", Integer.valueOf(13))
                ).readValue("{ }");
        assertEquals(Integer.valueOf(13), bean.age);
        assertEquals("Bob", bean.name);
    }

    // [databind#77]
    @Test
    public void testTransientField() throws Exception
    {
        TransientBean bean = MAPPER.readerFor(TransientBean.class)
                .with(new InjectableValues.Std()
                        .addValue("transient", "Injected!"))
                .readValue("{\"value\":28}");
        assertEquals(28, bean.value);
        assertEquals("Injected!", bean.injected);
    }

    @Test
    public void testIssueGH471() throws Exception
    {
        final Object constructorInjected = "constructorInjected";
        final Object methodInjected = "methodInjected";
        final Object fieldInjected = "fieldInjected";

        ObjectMapper mapper = newObjectMapper()
                        .setInjectableValues(new InjectableValues.Std()
                                .addValue("constructor_injected", constructorInjected)
                                .addValue("method_injected", methodInjected)
                                .addValue("field_injected", fieldInjected));

       IssueGH471Bean bean = mapper.readValue(
"{\"x\":13,\"constructor_value\":\"constructor\",\"method_value\":\"method\",\"field_value\":\"field\"}",
                IssueGH471Bean.class);

        // Assert *SAME* instance
        assertSame(constructorInjected, bean.constructorInjected);
        assertSame(methodInjected, bean.methodInjected);
        assertSame(fieldInjected, bean.fieldInjected);

        // Check that basic properties still work (better safe than sorry)
        assertEquals("constructor", bean.constructorValue);
        assertEquals("method", bean.methodValue);
        assertEquals("field", bean.fieldValue);

        assertEquals(13, bean.x);
    }
}
