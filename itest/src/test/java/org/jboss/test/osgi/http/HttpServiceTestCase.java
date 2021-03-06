/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi.http;

//$Id$

import static org.jboss.osgi.http.HttpServiceCapability.DEFAULT_HTTP_SERVICE_PORT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.osgi.http.HttpServiceCapability;
import org.jboss.osgi.testing.OSGiFrameworkTest;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.http.bundle.EndpointServlet;
import org.jboss.test.osgi.http.bundle.HttpServiceTestActivator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * An HttpService test case.
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Apr-2010
 */
public class HttpServiceTestCase extends OSGiFrameworkTest
{
   private static Bundle testBundle;

   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      if (testBundle == null)
      {
         // Build a test bundle with shrinkwrap
         final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "http-service-test");
         archive.addClass(HttpServiceTestActivator.class);
         archive.addClass(EndpointServlet.class);
         archive.addAsResource(getResourceFile("http/message.txt"), "res/message.txt");
         archive.setManifest(new Asset()
         {
            public InputStream openStream()
            {
               OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
               builder.addBundleSymbolicName(archive.getName());
               builder.addBundleManifestVersion(2);
               builder.addBundleActivator(HttpServiceTestActivator.class.getName());
               builder.addImportPackages("org.osgi.framework", "org.osgi.service.http", "org.osgi.util.tracker");
               builder.addImportPackages("javax.servlet", "javax.servlet.http");
               return builder.openStream();
            }
         });

         // Install/Start the test bundle
         testBundle = installBundle(archive);
         testBundle.start();
         assertBundleState(Bundle.ACTIVE, testBundle.getState());
      }
   }

   @Test
   public void testServletAccess() throws Exception
   {
      String line = getHttpResponse("/servlet?test=plain", 5000);
      assertEquals("Hello from Servlet", line);
   }

   @Test
   public void testServletInitProps() throws Exception
   {
      String line = getHttpResponse("/servlet?test=initProp", 5000);
      assertEquals("initProp=SomeValue", line);
   }

   @Test
   public void testServletBundleContext() throws Exception
   {
      String line = getHttpResponse("/servlet?test=context", 5000);
      assertEquals("http-service-test", line);
   }

   @Test
   public void testResourceAccess() throws Exception
   {
      String line = getHttpResponse("/file/message.txt", 5000);
      assertEquals("Hello from Resource", line);
   }

   private String getHttpResponse(String reqPath, int timeout) throws IOException
   {
      return HttpServiceCapability.getHttpResponse(getServerHost(), DEFAULT_HTTP_SERVICE_PORT, reqPath, timeout);
   }
}