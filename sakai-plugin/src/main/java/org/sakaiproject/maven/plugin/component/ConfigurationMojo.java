package org.sakaiproject.maven.plugin.component;



import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Package Configuration as a Zip for later deployment.
 */
@Mojo(name="configuration", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class ConfigurationMojo
    extends AbstractMojo
{
	/**
	 * The maven project.
	 */
    @Parameter(defaultValue="${project}", required = true, readonly = true)
	protected MavenProject project;

    /**
     * The Zip archiver.
     */
    @Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    /**
     * Directory containing the build files.
     */
    @Parameter(defaultValue="${project.build.directory}/configuration")
    private String configurationDirectory;
    /**
     * Directory containing the build files.
     */
    @Parameter(defaultValue="${project.build.directory}")
    private String outputDirectory;

    /**
     * The name of the generated Configuration.
     */
    @Parameter(defaultValue="${project.build.finalName}", required = true)
	private String configurationName;
	
    /**
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or
     * deploy it to the local repository instead of the default one in an execution.
     */
    @Parameter(property="primaryArtifact", defaultValue = "true")
    private boolean primaryArtifact;

    
    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     */
    @Parameter
	private String classifier;

    protected static File getConfigurationFile( File basedir, String finalName, String classifier )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + ".configuration" );
    }

    public void execute()
        throws MojoExecutionException
    {
        try {
           	File outputDirectoryFile = new File(outputDirectory);
            File buildDirectoryFile = new File(configurationDirectory);
        	File outputFile = getConfigurationFile( outputDirectoryFile, configurationName, classifier);
            zipArchiver.addDirectory( buildDirectoryFile, new String[]{"**/**"}, new String[]{"**/"+outputFile.getName()} );
            zipArchiver.setDestFile( outputFile );
            zipArchiver.createArchive();
            
            String classifier = this.classifier;
            if ( classifier != null )
            {
                projectHelper.attachArtifact( getProject(), "configuration", classifier, outputFile );
            }
            else
            {
                Artifact artifact = getProject().getArtifact();
                if ( primaryArtifact )
                {
                    artifact.setFile( outputFile );
                }
                else if ( artifact.getFile() == null || artifact.getFile().isDirectory() )
                {
                    artifact.setFile( outputFile );
                }
            }

            
        } catch( Exception e ) {
            throw new MojoExecutionException( "Could not zip configuration settings", e );
        }
    }
    
	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

}
