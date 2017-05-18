package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;


public class CachingWithSpringTest {

    @Test
    public void test() {

        ApplicationContext ctx = new ClassPathXmlApplicationContext("app-test.xml");
        
        IActiveDirectoryTokenGroupsRegistry registry = ctx.getBean(IActiveDirectoryTokenGroupsRegistry.class);
        
        assertNotNull(registry);
        
    }

}
