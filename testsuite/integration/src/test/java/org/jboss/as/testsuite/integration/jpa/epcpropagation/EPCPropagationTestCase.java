/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.testsuite.integration.jpa.epcpropagation;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
/**
 *
 * For managed debugging:
 * -Djboss.options="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y"
 *
 * For embedded debugging:
 * -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787"
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class EPCPropagationTestCase
{

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:/H2DS</jta-data-source>" +
            "  <class>org.jboss.as.testsuite.integration.jpa.epcpropagation.MyEntity</class>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>" +
            "  </persistence-unit>" +
            "</persistence>"
        ;

  @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "EPCPropagationTestCaseArchive.jar");
        jar.addClasses(
            CMTEPCStatefulBean.class, CMTStatefulBean.class, EPCStatefulBean.class,
            InitEPCStatefulBean.class, IntermediateStatefulBean.class, IntermediateStatefulInterface.class,
            MyEntity.class, NoTxEPCStatefulBean.class, NoTxStatefulBean.class,
            StatefulBean.class, StatefulInterface.class, StatelessBean.class,
            StatelessInterface.class, AbstractStatefulInterface.class, EPCPropagationTestCase.class
        );

        jar.addResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addResource(new StringAsset(""), "META-INF/MANIFEST.MF");
        return jar;
    }

   @Test
   public void testBMTPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(1, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("StatefulBean");
      boolean equal = stateful.execute(1, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }
   
   @Test
   public void testBMTEPCPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(2, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("EPCStatefulBean");
      boolean equal = stateful.execute(2, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }
   
   @Test
   public void testCMTPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(3, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("CMTStatefulBean");
      boolean equal = stateful.execute(3, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }
   
   @Test
   public void testCMTEPCPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(4, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("CMTEPCStatefulBean");
      boolean equal = stateful.execute(4, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }
   
   @Test
   public void testNoTxPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(5, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("NoTxStatefulBean");
      boolean equal = stateful.execute(5, "EntityName");
      
      assertFalse("Name changes should not propagate", equal);
   }
   
   @Test
   public void testNoTxEPCPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(6, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("NoTxEPCStatefulBean");
      boolean equal = stateful.execute(6, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }
   
   @Test
   public void testIntermediateEPCPropagation() throws Exception
   {
      StatelessInterface stateless = (StatelessInterface) new InitialContext().lookup("StatelessBean");
      stateless.createEntity(7, "EntityName");
      
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("InitEPCStatefulBean");
      boolean equal = stateful.execute(7, "EntityName");
      
      assertTrue("Name changes should propagate", equal);
   }

   @Test
   public void testXPCPostConstruct() throws Exception
   {
      StatefulInterface stateful = (StatefulInterface) new InitialContext().lookup("EPCStatefulBean");
      assertNull("stateful postConstruct operation should success: " + stateful.getPostConstructErrorMessage(), stateful.getPostConstructErrorMessage());
   }


}