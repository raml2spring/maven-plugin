package com.github.raml2spring.plugin;

import com.github.raml2spring.configuration.Raml2SpringConfig;
import com.github.raml2spring.data.RPModel;
import com.github.raml2spring.exception.RamlParseException;
import com.github.raml2spring.exception.RamlIOException;
import com.github.raml2spring.util.CodeGenerator;
import com.github.raml2spring.util.RamlParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.util.StringUtils;

import java.io.File;

@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true, requiresProject = false)
public class Raml2SpringMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;

    @Parameter(property = "ramlPath", required = true, readonly = true)
    public String ramlPath;

    @Parameter(property = "outputPath", readonly = true, defaultValue = "./target/generated-sources/")
    public String outputPath;

    @Parameter(property = "basePackage", required = true, readonly = true)
    public String basePackage;

    @Parameter(property = "schemaLocation", readonly = true)
    public String schemaLocation;

    @Parameter(property = "useOldJavaDate", readonly = true, defaultValue = "false")
    public boolean useOldJavaDate;

    @Parameter(property = "ignoreProperties", readonly = true, defaultValue = "false")
    public boolean ignoreProperties;

    @Parameter(property = "ignoreUnknown", readonly = true, defaultValue = "false")
    public boolean ignoreUnknown;

    @Parameter(property = "generateExceptions", readonly = true, defaultValue = "true")
    public boolean generateExceptions;

    @Parameter(property = "enableHypermediaSupport", readonly = true, defaultValue = "false")
    public boolean enableHypermediaSupport;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        long startTime = System.currentTimeMillis();
        try {
            ramlToSpring();
        } catch (RamlIOException e) {
            throw new MojoExecutionException(e, "Unexpected exception while executing Raml2Spring Plugin.",
                    e.toString());
        } catch (RamlParseException e) {
            throw new MojoExecutionException(e, "Supplied RAML has failed validation and cannot be loaded.", e.toString());
        }

        this.getLog().info("Generation Completed: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void ramlToSpring() {

        String resolvedRamlPath = getAbsoluteRamlPath();
        String resolvedSchemaLocation = getAbsoluteSchemaLocation(resolvedRamlPath);
        String resolvedOutputPath = getAbsoluteOutputPath();

        Raml2SpringConfig.setMojo(this);
        Raml2SpringConfig.setSchemaLocation(toUriString(resolvedSchemaLocation));

        getLog().info("parse RAML ...");
        RamlParser ramlParser = new RamlParser(toUriString(resolvedRamlPath), generateExceptions);
        RPModel model = new RPModel();

        model = ramlParser.readModel(model);
        getLog().info("write Data to Disk ...");
        CodeGenerator.writeCodeToDisk(model, resolvedOutputPath);

        getLog().info("RamlModel generated");
    }

    private String getAbsoluteSchemaLocation(String absoluteRamlPath) {
        if(StringUtils.hasText(schemaLocation)) {
            return getAbsolutePath(schemaLocation);
        } else {
            return new File(absoluteRamlPath).getParent().concat(File.separator).concat("schemas");
        }

    }

    private String getAbsoluteRamlPath() {
        return getAbsolutePath(ramlPath);
    }

    private String getAbsoluteOutputPath() {
        if(StringUtils.hasText(outputPath)) {
            return getAbsolutePath(outputPath);
        } else {
            return getAbsolutePath(Raml2SpringConfig.getOutputPath());
        }
    }

    private String getAbsolutePath(String path) {
        if(new File(path).isAbsolute()) {
            return path;
        } else {
            String rootPath = project.getBasedir().getAbsolutePath();
            if(!path.startsWith("/") && !path.startsWith("\\")) {
                rootPath += File.separator;
            }
            return rootPath.concat(path);
        }
    }

    private String toUriString(String path) {
        return new File(path).toURI().toString();
    }

}
