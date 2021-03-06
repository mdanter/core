/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.javaee.cdi.ui;

import static org.hamcrest.CoreMatchers.*;
import static org.jboss.forge.addon.javaee.JavaEEFacet.DEFAULT_CDI_PACKAGE;
import static org.junit.Assert.*;

import java.lang.annotation.Inherited;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.interceptor.InterceptorBinding;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.ProjectHelper;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.AddonDeployments;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.roaster.model.JavaAnnotation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="antonio.goncalves@gmail.com">Antonio Goncalves</a>
 */
@RunWith(Arquillian.class)
public class CDINewInterceptorBindingCommandTest
{
   @Deployment
   @AddonDeployments({
            @AddonDeployment(name = "org.jboss.forge.addon:ui"),
            @AddonDeployment(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDeployment(name = "org.jboss.forge.addon:shell-test-harness"),
            @AddonDeployment(name = "org.jboss.forge.addon:javaee"),
            @AddonDeployment(name = "org.jboss.forge.addon:maven")
   })
   public static AddonArchive getDeployment()
   {
      return ShrinkWrap
               .create(AddonArchive.class)
               .addClass(ProjectHelper.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness")
               );
   }

   @Inject
   private UITestHarness uiTestHarness;

   @Inject
   private ShellTest shellTest;

   @Inject
   private ProjectHelper projectHelper;

   private Project project;

   @Before
   public void setUp()
   {
      project = projectHelper.createJavaLibraryProject();
      projectHelper.installCDI_1_0(project);
   }

   @Test
   public void checkCommandMetadata() throws Exception
   {
      try (CommandController controller = uiTestHarness.createCommandController(CDINewInterceptorBindingCommand.class,
               project.getRoot()))
      {
         controller.initialize();
         // Checks the command metadata
         assertTrue(controller.getCommand() instanceof CDINewInterceptorBindingCommand);
         UICommandMetadata metadata = controller.getMetadata();
         assertEquals("CDI: New Interceptor Binding", metadata.getName());
         assertEquals("Java EE", metadata.getCategory().getName());
         assertEquals("CDI", metadata.getCategory().getSubCategory().getName());
         assertEquals(3, controller.getInputs().size());
         assertFalse("Project is created, shouldn't have targetLocation", controller.hasInput("targetLocation"));
         assertTrue(controller.hasInput("named"));
         assertTrue(controller.hasInput("targetPackage"));
         assertTrue(controller.hasInput("overwrite"));
         assertTrue(controller.getValueFor("targetPackage").toString().endsWith(DEFAULT_CDI_PACKAGE));
      }
   }

   @Test
   public void checkCommandShell() throws Exception
   {
      shellTest.getShell().setCurrentResource(project.getRoot());
      Result result = shellTest.execute("cdi-new-interceptor-binding --named Dummy", 10, TimeUnit.SECONDS);

      Assert.assertThat(result, not(instanceOf(Failed.class)));
      Assert.assertTrue(project.hasFacet(CDIFacet.class));
   }

   @Test
   public void testCreateNewInterceptorBinding() throws Exception
   {
      try (CommandController controller = uiTestHarness.createCommandController(CDINewInterceptorBindingCommand.class,
               project.getRoot()))
      {
         controller.initialize();
         controller.setValueFor("named", "MyInterceptorBinding");
         controller.setValueFor("targetPackage", "org.jboss.forge.test");
         Assert.assertTrue(controller.isValid());
         Assert.assertTrue(controller.canExecute());
         Result result = controller.execute();
         Assert.assertThat(result, is(not(instanceOf(Failed.class))));
      }

      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
      JavaResource javaResource = facet.getJavaResource("org.jboss.forge.test.MyInterceptorBinding");
      Assert.assertNotNull(javaResource);
      Assert.assertThat(javaResource.getJavaType(), is(instanceOf(JavaAnnotation.class)));
      JavaAnnotation<?> ann = javaResource.getJavaType();
      Assert.assertTrue(ann.hasAnnotation(InterceptorBinding.class));
      Assert.assertFalse(ann.hasAnnotation(Inherited.class));
   }
}