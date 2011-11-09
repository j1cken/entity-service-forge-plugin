package de.adorsys.forge.plugin;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.*;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 09.11.11
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
@Alias("entity-service")
@RequiresFacet(JavaSourceFacet.class)
@Help("A plugin that helps creating entity service facades.")
public class EntityServicePlugin implements Plugin {

   static {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        Velocity.init(properties);
    }

   @Inject
   private Project project;

   @Inject
   private Event<PickupResource> pickup;

   @Inject
   @Current
   private JavaResource resource;

   @Inject
   private Shell shell;

   @Command(value = "create-from",
            help = "Create a new entity service class with CRUD methods for an existing entity bean.")
   public void createFrom(
         @Option(name = "entity", required = true, type = PromptType.JAVA_CLASS) JavaResource entity,
         final PipeOut out) throws FileNotFoundException {

      JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

      JavaSource<?> entitySource = entity.getJavaSource();

      VelocityContext context = new VelocityContext();
      context.put("package", java.getBasePackage() + ".service");
      context.put("entityImport", entitySource.getQualifiedName());
      context.put("entityName", entitySource.getName());
      context.put("cdiName", entitySource.getName().toLowerCase());

      // Service class
      StringWriter writer = new StringWriter();
      Velocity.mergeTemplate("TemplateService.vtl", "UTF-8", context, writer);

      JavaClass serviceClass = JavaParser.parse(JavaClass.class, writer.toString());
      java.saveJavaSource(serviceClass);

      pickup.fire(new PickupResource(java.getJavaResource(serviceClass)));

      // ServiceTest class
      StringWriter writerTest = new StringWriter();
      Velocity.mergeTemplate("TemplateServiceTest.vtl", "UTF-8", context, writerTest);

      JavaClass serviceTestClass = JavaParser.parse(JavaClass.class, writerTest.toString());
      java.saveTestJavaSource(serviceTestClass);

      pickup.fire(new PickupResource(java.getTestJavaResource(serviceTestClass)));
   }
}
