package org.jboss.forge.addon.javaee.jpa;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Embeddable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.ProjectHelper;
import org.jboss.forge.addon.javaee.jpa.ui.NewEmbeddableCommand;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.AddonDeployments;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.roaster.model.JavaClass;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class NewEmbeddableCommandTest
{

   @Deployment
   @AddonDeployments({
            @AddonDeployment(name = "org.jboss.forge.addon:ui"),
            @AddonDeployment(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDeployment(name = "org.jboss.forge.addon:javaee"),
            @AddonDeployment(name = "org.jboss.forge.addon:maven")
   })
   public static AddonArchive getDeployment()
   {
      return ShrinkWrap
               .create(AddonArchive.class)
               .addBeansXML()
               .addClass(ProjectHelper.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness")
               );
   }

   @Inject
   private UITestHarness uiTestHarness;

   @Inject
   private ProjectHelper projectHelper;

   private Project project;

   @Before
   public void setUp()
   {
      project = projectHelper.createJavaLibraryProject();
      projectHelper.installJPA_2_0(project);
   }

   @Test
   public void checkCommandMetadata() throws Exception
   {
      CommandController controller = uiTestHarness.createCommandController(NewEmbeddableCommand.class,
               project.getRoot());
      controller.initialize();
      // Checks the command metadata
      assertTrue(controller.getCommand() instanceof NewEmbeddableCommand);
      UICommandMetadata metadata = controller.getMetadata();
      assertEquals("JPA: New Embeddable", metadata.getName());
      assertEquals("Java EE", metadata.getCategory().getName());
      assertEquals("JPA", metadata.getCategory().getSubCategory().getName());
      assertEquals(3, controller.getInputs().size());
      assertTrue(controller.hasInput("targetLocation")); // TODO this should be false, fix the NewEmbeddableCommand
      assertTrue(controller.hasInput("named"));
      assertTrue(controller.hasInput("targetPackage"));
      assertTrue(controller.getValueFor("targetPackage").toString().endsWith(".model"));
   }

   @Test
   public void testCreateEmbeddable() throws Exception
   {
      CommandController controller = uiTestHarness.createCommandController(NewEmbeddableCommand.class,
               project.getRoot());
      controller.initialize();
      controller.setValueFor("named", "MyEmbeddable");
      Assert.assertTrue(controller.isValid());
      Assert.assertTrue(controller.canExecute());
      Result result = controller.execute();
      Assert.assertThat(result, is(not(instanceOf(Failed.class))));

      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

      final List<JavaClass<?>> embeddables = new ArrayList<>();
      facet.visitJavaSources(new JavaResourceVisitor()
      {
         @Override
         public void visit(VisitContext context, JavaResource resource)
         {
            try
            {
               JavaType<?> type = resource.getJavaType();
               if (type.hasAnnotation(Embeddable.class) && type.isClass())
               {
                  embeddables.add((JavaClassSource) type);
               }
            }
            catch (FileNotFoundException e)
            {
               throw new IllegalStateException(e);
            }
         }
      });

      assertEquals(1, embeddables.size());
      JavaClass<?> embeddableEntity = embeddables.get(0);
      assertEquals(0, embeddableEntity.getSyntaxErrors().size());
      assertTrue(embeddableEntity.hasAnnotation(Embeddable.class));
      assertTrue(embeddableEntity.hasInterface(Serializable.class));
      assertEquals(org.jboss.forge.roaster.model.Visibility.PUBLIC, embeddableEntity.getVisibility());
      assertEquals(0, embeddableEntity.getFields().size());
      assertEquals(0, embeddableEntity.getMethods().size());
      assertEquals(0, embeddableEntity.getMembers().size());
      assertEquals(0, embeddableEntity.getProperties().size());
      assertFalse(embeddableEntity.hasJavaDoc());
      assertTrue(embeddableEntity.getName().equals("MyEmbeddable"));
   }
}
