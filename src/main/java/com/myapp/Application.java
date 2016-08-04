package com.myapp;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.jpa.JPAFraction;
import org.wildfly.swarm.logging.LoggingFraction;
import org.wildfly.swarm.spi.api.SwarmProperties;
import org.wildfly.swarm.undertow.WARArchive;

public class Application {

	public static final Boolean debug = false;

	public static void main(String... args) throws Exception {
		
		System.setProperty(SwarmProperties.PROJECT_STAGE_FILE, "project-stages.yml");

		final Container container = new Container(debug);

		final WARArchive deployment = ShrinkWrap.create(WARArchive.class);
//		deployment.as(Secured.class);

		container.fraction(new LoggingFraction());
		container.fraction(new JPAFraction().inhibitDefaultDatasource().defaultDatasource("MyAppDS"));

		addResources(deployment);

		container.start();
		container.deploy(deployment);
	}

	private static void addResources(WARArchive deployment) throws Exception {

		final String WEBAPP_SRC = "src/main/webapp";

		// Class files
		deployment.addPackages(true, "com/myapp");
		deployment.addAllDependencies();

		// META-INF contents
		deployment.addAsResource(new File("src/main/resources/META-INF/", "persistence.xml"),
				"META-INF/persistence.xml");

		// WEB-INF contents
		List<String> webInf = Arrays.asList("web.xml", "beans.xml");
		webInf.forEach(resource -> deployment.addAsWebInfResource(new File(WEBAPP_SRC, "WEB-INF/" + resource)));

		// OTHER FOLDERS in webapp dir
		List<String> resources = Arrays.asList("css", "img", "js");

		resources.forEach(resource -> {
			deployment.merge(ShrinkWrap.create(ExplodedImporter.class).importDirectory("src/main/webapp/" + resource)
					.as(GenericArchive.class), "/" + resource);
		});

		// add remaining files
		List<String> files = Arrays.asList("index.html");
		files.forEach(file -> deployment.addAsWebResource(new File(WEBAPP_SRC, file)));

		deployment.writeTo(System.out, Formatters.VERBOSE);

	}

}
