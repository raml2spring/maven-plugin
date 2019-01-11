package com.github.raml2spring.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class NamingHelperTest {

    @Test
    public void testClassName() {
        assertEquals("TestClass", NamingHelper.getClassName("test class"));
    }

    @Test
    public void testMethodName() {
        assertEquals("getData", NamingHelper.getMethodName("Get data"));
    }
}
