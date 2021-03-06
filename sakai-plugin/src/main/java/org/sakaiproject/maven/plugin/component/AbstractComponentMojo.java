package org.sakaiproject.maven.plugin.component;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.*;

public abstract class AbstractComponentMojo extends AbstractMojo {
	/**
	 * The maven project.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;

	/**
	 * The directory containing generated classes.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
	private File classesDirectory;

	/**
	 * Whether a JAR file will be created for the classes in the webapp. Using
	 * this optional configuration parameter will make the generated classes to
	 * be archived into a jar file and the classes directory will then be
	 * excluded from the webapp.
	 * 
	 */
	@Parameter(property = "archiveClasses", defaultValue = "false")
	private boolean archiveClasses;

	/**
	 * The Jar archiver needed for archiving classes directory into jar file
	 * under WEB-INF/lib.
	 */
	@Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
	private JarArchiver jarArchiver;

	/**
	 * The directory where the webapp is built.
	 */
	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
	private File webappDirectory;

	/**
	 * Single directory for extra files to include in the WAR.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
	private File warSourceDirectory;

	/**
	 * The list of webResources we want to transfer.
	 */
	@Parameter
	private Resource[] webResources;

	/**
	 * Filters (property files) to include during the interpolation of the
	 * pom.xml.
	 */
	@Parameter(defaultValue="${project.build.filters}")
	private List filters;

	/**
	 * The path to the web.xml file to use.
	 */
	@Parameter(property="maven.war.webxml")
	private File webXml;

	/**
	 * The path to the context.xml file to use.
	 */
	@Parameter(property="maven.war.containerConfigXML")
	private File containerConfigXML;

	/**
	 * Directory to unpack dependent WARs into if needed
	 */
	@Parameter(defaultValue="${project.build.directory}/war/work", required = true)
	private File workDirectory;
	
	@Component
    protected ArtifactFactory artifactFactory;
	@Component
    protected ArtifactResolver artifactResolver;

	@Parameter(defaultValue="${localRepository}")
    protected ArtifactRepository artifactRepository;

	@Parameter(defaultValue="${project.remoteArtifactRepositories}")
    protected List remoteRepositories;



	/**
	 * To look up Archiver/UnArchiver implementations
	 */
	@Component(role = org.codehaus.plexus.archiver.manager.ArchiverManager.class)
	protected ArchiverManager archiverManager;

	private static final String WEB_INF = "WEB-INF";

	private static final String META_INF = "META-INF";

	private static final String[] DEFAULT_INCLUDES = { "**/**" };

	/**
	 * The comma separated list of tokens to include in the WAR. Default is
	 * '**'.
	 */
	@Parameter(alias="includes")
	private String warSourceIncludes = "**";

	/**
	 * The comma separated list of tokens to exclude from the WAR.
	 */
	@Parameter(alias="excludes")
	private String warSourceExcludes;

	/**
	 * The comma separated list of tokens to include when doing a war overlay.
	 * Default is '**'
	 */
	@Parameter
	private String dependentWarIncludes = "**";

	/**
	 * The comma separated list of tokens to exclude when doing a war overlay.
	 */
	@Parameter
	private String dependentWarExcludes;

	/**
	 * The maven archive configuration to use.
	 */
	@Parameter
	protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	private static final String[] EMPTY_STRING_ARRAY = {};

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public File getClassesDirectory() {
		return classesDirectory;
	}

	public void setClassesDirectory(File classesDirectory) {
		this.classesDirectory = classesDirectory;
	}

	public File getWebappDirectory() {
		return webappDirectory;
	}

	public void setWebappDirectory(File webappDirectory) {
		this.webappDirectory = webappDirectory;
	}

	public File getWarSourceDirectory() {
		return warSourceDirectory;
	}

	public void setWarSourceDirectory(File warSourceDirectory) {
		this.warSourceDirectory = warSourceDirectory;
	}

	public File getWebXml() {
		return webXml;
	}

	public void setWebXml(File webXml) {
		this.webXml = webXml;
	}

	public File getContainerConfigXML() {
		return containerConfigXML;
	}

	public void setContainerConfigXML(File containerConfigXML) {
		this.containerConfigXML = containerConfigXML;
	}


	/**
	 * Returns a string array of the excludes to be used when assembling/copying
	 * the war.
	 * 
	 * @return an array of tokens to exclude
	 */
	protected String[] getExcludes() {
		List excludeList = new ArrayList();
		if (StringUtils.isNotEmpty(warSourceExcludes)) {
			excludeList.addAll(Arrays.asList(StringUtils.split(
					warSourceExcludes, ",")));
		}

		// if webXML is specified, omit the one in the source directory
		if (webXml != null && StringUtils.isNotEmpty(webXml.getName())) {
			excludeList.add("**/" + WEB_INF + "/web.xml");
		}

		// if contextXML is specified, omit the one in the source directory
		if (containerConfigXML != null
				&& StringUtils.isNotEmpty(containerConfigXML.getName())) {
			excludeList.add("**/" + META_INF + "/"
					+ containerConfigXML.getName());
		}

		return (String[]) excludeList.toArray(EMPTY_STRING_ARRAY);
	}

	/**
	 * Returns a string array of the includes to be used when assembling/copying
	 * the war.
	 * 
	 * @return an array of tokens to include
	 */
	protected String[] getIncludes() {
		return StringUtils.split(StringUtils.defaultString(warSourceIncludes),
				",");
	}

	/**
	 * Returns a string array of the excludes to be used when adding dependent
	 * wars as an overlay onto this war.
	 * 
	 * @return an array of tokens to exclude
	 */
	protected String[] getDependentWarExcludes() {
		String[] excludes;
		if (StringUtils.isNotEmpty(dependentWarExcludes)) {
			excludes = StringUtils.split(dependentWarExcludes, ",");
		} else {
			excludes = EMPTY_STRING_ARRAY;
		}
		return excludes;
	}

	/**
	 * Returns a string array of the includes to be used when adding dependent
	 * wars as an overlay onto this war.
	 * 
	 * @return an array of tokens to include
	 */
	protected String[] getDependentWarIncludes() {
		return StringUtils.split(StringUtils
				.defaultString(dependentWarIncludes), ",");
	}



	protected String getProjectId() {
		return project.getGroupId()+":"+project.getArtifactId()+":"+project.getPackaging()+":"+project.getVersion();
	}

        public void deleteAll(File dir) {
              if ( dir.isDirectory() ) {
                 File[] files = dir.listFiles();
                 if (files != null) {
                     for ( int i = 0;i < files.length; i++ ) {
                        if ( files[i].isDirectory() ) {
                             deleteAll(files[i]);
                        } else {
                            files[i].delete();
                        }
                     }
                 } else {
		            getLog().error("deleteAll: files is null");
                 }
              } 
	      dir.delete();
        }

	public void buildExplodedWebapp(File webappDirectory)
			throws MojoExecutionException, MojoFailureException {
		getLog().info("Exploding webapp...");

		webappDirectory.mkdirs();

		try {
			buildWebapp(project, webappDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("Could not explode webapp...", e);
		}
	}

	private Map getBuildFilterProperties() throws MojoExecutionException {

		Map filterProperties = new Properties();

		// System properties
		filterProperties.putAll(System.getProperties());

		// Project properties
		filterProperties.putAll(project.getProperties());

		for (Iterator i = filters.iterator(); i.hasNext();) {
			String filtersfile = (String) i.next();

			try {
				Properties properties = PropertyUtils.loadPropertyFile(
						new File(filtersfile), true, true);

				filterProperties.putAll(properties);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Error loading property file '" + filtersfile + "'", e);
			}
		}

		// can't putAll, as ReflectionProperties doesn't enumerate - so we make
		// a composite map with the project variables as dominant
		return new CompositeMap(new ReflectionProperties(project),
				filterProperties);
	}

	/**
	 * Copies webapp webResources from the specified directory. <p/> Note that
	 * the <tt>webXml</tt> parameter could be null and may specify a file
	 * which is not named <tt>web.xml<tt>. If the file
	 * exists, it will be copied to the <tt>META-INF</tt> directory and
	 * renamed accordingly.
	 *
	 * @param resource         the resource to copy
	 * @param webappDirectory  the target directory
	 * @param filterProperties
	 * @throws java.io.IOException if an error occured while copying webResources
	 */
	public void copyResources(Resource resource, File webappDirectory,
			Map filterProperties) throws IOException {
		if (!resource.getDirectory().equals(webappDirectory.getPath())) {
			getLog().info(
					"Copy webapp webResources to "
							+ webappDirectory.getAbsolutePath());
			if (webappDirectory.exists()) {
				String[] fileNames = getWarFiles(resource);
				String targetPath = (resource.getTargetPath() == null) ? ""
						: resource.getTargetPath();
				File destination = new File(webappDirectory, targetPath);
				for (int i = 0; i < fileNames.length; i++) {
					if (resource.isFiltering()) {
						copyFilteredFile(new File(resource.getDirectory(),
								fileNames[i]), new File(destination,
								fileNames[i]), null, getFilterWrappers(),
								filterProperties);
					} else {
						FileUtils.copyFileIfModified(new File(resource.getDirectory(),
								fileNames[i]), new File(destination,
								fileNames[i]));
					}
				}
			}
		}
	}

	/**
	 * Copies webapp webResources from the specified directory. <p/> Note that
	 * the <tt>webXml</tt> parameter could be null and may specify a file
	 * which is not named <tt>web.xml<tt>. If the file
	 * exists, it will be copied to the <tt>META-INF</tt> directory and
	 * renamed accordingly.
	 *
	 * @param sourceDirectory the source directory
	 * @param webappDirectory the target directory
	 * @throws java.io.IOException if an error occured while copying webResources
	 */
	public void copyResources(File sourceDirectory, File webappDirectory)
			throws IOException {
		if (!sourceDirectory.equals(webappDirectory)) {
			getLog().info(
					"Copy webapp webResources to "
							+ webappDirectory.getAbsolutePath());
			if (warSourceDirectory.exists()) {
				String[] fileNames = getWarFiles(sourceDirectory);
				for (int i = 0; i < fileNames.length; i++) {
					FileUtils.copyFileIfModified(new File(sourceDirectory, fileNames[i]),
							new File(webappDirectory, fileNames[i]));
				}
			}
		}
	}

	/**
	 * Generates the JAR.
	 * 
	 * @todo Add license files in META-INF directory.
	 */
	public void createJarArchive(File libDirectory)
			throws MojoExecutionException {
		String archiveName = project.getBuild().getFinalName() + ".jar";

		File jarFile = new File(libDirectory, archiveName);

		MavenArchiver archiver = new MavenArchiver();

		archiver.setArchiver(jarArchiver);

		archiver.setOutputFile(jarFile);

		try {
			archiver.getArchiver().addDirectory(classesDirectory,
					getIncludes(), getExcludes());

			archiver.createArchive(null, project, archive);
		} catch (Exception e) {
			// TODO: improve error handling
			throw new MojoExecutionException("Error assembling JAR", e);
		}
	}

	protected void checkComponentWebXmlExists(File webXml) {
		try {
			if (!webXml.exists()) {
				FileWriter fw = new FileWriter(webXml);
				fw.write("");
				fw.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Builds the webapp for the specified project. <p/> Classes, libraries and
	 * tld files are copied to the <tt>webappDirectory</tt> during this phase.
	 * 
	 * @param project
	 *            the maven project
	 * @param webappDirectory
	 * @throws java.io.IOException
	 *             if an error occured while building the webapp
	 */
	public void buildWebapp(MavenProject project, File webappDirectory)
			throws MojoExecutionException, IOException, MojoFailureException {
		getLog().info(
				"Assembling webapp " + project.getArtifactId() + " in "
						+ webappDirectory);

		File webinfDir = new File(webappDirectory, WEB_INF);
		webinfDir.mkdirs();

		File metainfDir = new File(webappDirectory, META_INF);
		metainfDir.mkdirs();

		List webResources = this.webResources != null ? Arrays
				.asList(this.webResources) : null;
		if (webResources != null && webResources.size() > 0) {
			Map filterProperties = getBuildFilterProperties();
			for (Iterator it = webResources.iterator(); it.hasNext();) {
				Resource resource = (Resource) it.next();
				copyResources(resource, webappDirectory, filterProperties);
			}
		}

		copyResources(warSourceDirectory, webappDirectory);

		if (webXml != null && StringUtils.isNotEmpty(webXml.getName())) {
			if (!webXml.exists()) {
				throw new MojoFailureException("The specified web.xml file '"
						+ webXml + "' does not exist");
			}

			// rename to web.xml
			FileUtils.copyFileIfModified(webXml, new File(webinfDir, "/web.xml"));
		}
		checkComponentWebXmlExists(new File(webinfDir, "/web.xml"));

		if (containerConfigXML != null
				&& StringUtils.isNotEmpty(containerConfigXML.getName())) {
			metainfDir = new File(webappDirectory, META_INF);
			String xmlFileName = containerConfigXML.getName();
			FileUtils.copyFileIfModified(containerConfigXML, new File(metainfDir,
					xmlFileName));
		}

		File libDirectory = new File(webinfDir, "lib");

		File tldDirectory = new File(webinfDir, "tld");

		File webappClassesDirectory = new File(webappDirectory, WEB_INF
				+ "/classes");

		if (classesDirectory.exists()
				&& !classesDirectory.equals(webappClassesDirectory)) {
			if (archiveClasses) {
				createJarArchive(libDirectory);
			} else {
				FileUtils.copyDirectoryStructureIfModified(classesDirectory,
						webappClassesDirectory);
			}
		}

		Set artifacts = project.getArtifacts();

		List duplicates = findDuplicates(artifacts);

		List dependentWarDirectories = new ArrayList();

		for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			String targetFileName = getDefaultFinalName(artifact);

			getLog().debug("Processing: " + targetFileName);

			if (duplicates.contains(targetFileName)) {
				getLog().debug("Duplicate found: " + targetFileName);
				targetFileName = artifact.getGroupId() + "-" + targetFileName;
				getLog().debug("Renamed to: " + targetFileName);
			}

			// TODO: utilise appropriate methods from project builder
			ScopeArtifactFilter filter = new ScopeArtifactFilter(
					Artifact.SCOPE_RUNTIME);
			if (!artifact.isOptional() && filter.include(artifact)) {
				String type = artifact.getType();
				if ("tld".equals(type)) {
					FileUtils.copyFileIfModified(artifact.getFile(), new File(
							tldDirectory, targetFileName));
				} else {
					if ("jar".equals(type) || "ejb".equals(type)
							|| "ejb-client".equals(type)) {
						FileUtils.copyFileIfModified(artifact.getFile(), new File(
								libDirectory, targetFileName));
					} else {
						if ("par".equals(type)) {
							targetFileName = targetFileName.substring(0,
									targetFileName.lastIndexOf('.'))
									+ ".jar";

							getLog().debug(
									"Copying "
											+ artifact.getFile()
											+ " to "
											+ new File(libDirectory,
													targetFileName));

							FileUtils.copyFileIfModified(artifact.getFile(), new File(
									libDirectory, targetFileName));
						} else {
							if ("war".equals(type)) {
								dependentWarDirectories
										.add(unpackWarToTempDirectory(artifact));
							} else {
								getLog().debug(
										"Skipping artifact of type " + type
												+ " for WEB-INF/lib");
							}
						}
					}
				}
			}
		}

		if (dependentWarDirectories.size() > 0) {
			getLog()
					.info(
							"Overlaying " + dependentWarDirectories.size()
									+ " war(s).");

			// overlay dependent wars
			for (Iterator iter = dependentWarDirectories.iterator(); iter
					.hasNext();) {
				copyDependentWarContents((File) iter.next(), webappDirectory);
			}
		}
	}

	/**
	 * Searches a set of artifacts for duplicate filenames and returns a list of
	 * duplicates.
	 * 
	 * @param artifacts
	 *            set of artifacts
	 * @return List of duplicated artifacts
	 */
	private List findDuplicates(Set artifacts) {
		List duplicates = new ArrayList();
		List identifiers = new ArrayList();
		for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			String candidate = getDefaultFinalName(artifact);
			if (identifiers.contains(candidate)) {
				duplicates.add(candidate);
			} else {
				identifiers.add(candidate);
			}
		}
		return duplicates;
	}

	/**
	 * Unpacks war artifacts into a temporary directory inside
	 * <tt>workDirectory</tt> named with the name of the war.
	 * 
	 * @param artifact
	 *            War artifact to unpack.
	 * @return Directory containing the unpacked war.
	 * @throws MojoExecutionException
	 */
	private File unpackWarToTempDirectory(Artifact artifact)
			throws MojoExecutionException {
		String name = artifact.getFile().getName();
		File tempLocation = new File(workDirectory, name.substring(0, name
				.length() - 4));

		boolean process = false;
		if (!tempLocation.exists()) {
			tempLocation.mkdirs();
			process = true;
		} else if (artifact.getFile().lastModified() > tempLocation
				.lastModified()) {
			process = true;
		}

		if (process) {
			File file = artifact.getFile();
			try {
				unpack(file, tempLocation,false);
			} catch (NoSuchArchiverException e) {
				this.getLog().info(
						"Skip unpacking dependency file with unknown extension: "
								+ file.getPath());
			}
		}

		return tempLocation;
	}

	/**
	 * Unpacks the archive file.
	 * 
	 * @param file
	 *            File to be unpacked.
	 * @param location
	 *            Location where to put the unpacked files.
	 */
	private void unpack(File file, File location,boolean overwrite)
			throws MojoExecutionException, NoSuchArchiverException {
		String archiveExt = FileUtils.getExtension(file.getAbsolutePath())
				.toLowerCase();
		unpack(file,location,archiveExt,overwrite);
	}
	/**
	 * Unpacks an archive with a given type
	 * @param file the file to be unpacked
	 * @param location the location to unpack the file to 
	 * @param archiveExt the archive type/extension
	 * @throws MojoExecutionException
	 * @throws NoSuchArchiverException
	 */
	protected void unpack(File file, File location, String archiveExt, boolean overwrite)
		throws MojoExecutionException, NoSuchArchiverException {


		try {
			UnArchiver unArchiver = archiverManager.getUnArchiver(archiveExt);
			unArchiver.setSourceFile(file);
			unArchiver.setDestDirectory(location);
			unArchiver.setOverwrite(overwrite);
			unArchiver.extract();
		} catch (ArchiverException e) {
			throw new MojoExecutionException("Error unpacking file: " + file
					+ "to: " + location, e);
		}
	}

	/**
	 * Recursively copies contents of <tt>srcDir</tt> into <tt>targetDir</tt>.
	 * This will not overwrite any existing files.
	 * 
	 * @param srcDir
	 *            Directory containing unpacked dependent war contents
	 * @param targetDir
	 *            Directory to overlay srcDir into
	 */
	private void copyDependentWarContents(File srcDir, File targetDir)
			throws MojoExecutionException {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(srcDir);
		scanner.setExcludes(getDependentWarExcludes());
		scanner.addDefaultExcludes();

		scanner.setIncludes(getDependentWarIncludes());

		scanner.scan();

		String[] dirs = scanner.getIncludedDirectories();
		for (int j = 0; j < dirs.length; j++) {
			new File(targetDir, dirs[j]).mkdirs();
		}

		String[] files = scanner.getIncludedFiles();

		for (int j = 0; j < files.length; j++) {
			File targetFile = new File(targetDir, files[j]);

			try {
				// Don't copy if it is in the source directory
				if (!new File(warSourceDirectory, files[j]).exists()) {
					targetFile.getParentFile().mkdirs();
					FileUtils.copyFileIfModified(new File(srcDir, files[j]), targetFile);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Error copying file '"
						+ files[j] + "' to '" + targetFile + "'", e);
			}
		}
	}

	/**
	 * Returns a list of filenames that should be copied over to the destination
	 * directory.
	 * 
	 * @param sourceDir
	 *            the directory to be scanned
	 * @return the array of filenames, relative to the sourceDir
	 */
	private String[] getWarFiles(File sourceDir) {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(sourceDir);
		scanner.setExcludes(getExcludes());
		scanner.addDefaultExcludes();

		scanner.setIncludes(getIncludes());

		scanner.scan();

		return scanner.getIncludedFiles();
	}

	/**
	 * Returns a list of filenames that should be copied over to the destination
	 * directory.
	 * 
	 * @param resource
	 *            the resource to be scanned
	 * @return the array of filenames, relative to the sourceDir
	 */
	private String[] getWarFiles(Resource resource) {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(resource.getDirectory());
		if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
			scanner.setIncludes((String[]) resource.getIncludes().toArray(
					EMPTY_STRING_ARRAY));
		} else {
			scanner.setIncludes(DEFAULT_INCLUDES);
		}
		if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
			scanner.setExcludes((String[]) resource.getExcludes().toArray(
					EMPTY_STRING_ARRAY));
		}

		scanner.addDefaultExcludes();

		scanner.scan();

		return scanner.getIncludedFiles();
	}

	private FilterWrapper[] getFilterWrappers() {
		return new FilterWrapper[] {
		// support ${token}
				new FilterWrapper() {
					public Reader getReader(Reader fileReader,
							Map filterProperties) {
						return new InterpolationFilterReader(fileReader,
								filterProperties, "${", "}");
					}
				},
				// support @token@
				new FilterWrapper() {
					public Reader getReader(Reader fileReader,
							Map filterProperties) {
						return new InterpolationFilterReader(fileReader,
								filterProperties, "@", "@");
					}
				} };
	}

	/**
	 * @param from
	 * @param to
	 * @param encoding
	 * @param wrappers
	 * @param filterProperties
	 * @throws IOException
	 *             TO DO: Remove this method when Maven moves to plexus-utils
	 *             version 1.4
	 */
	private static void copyFilteredFile(File from, File to, String encoding,
			FilterWrapper[] wrappers, Map filterProperties) throws IOException {
		// buffer so it isn't reading a byte at a time!
		Reader fileReader = null;
		Writer fileWriter = null;
		try {
			// fix for MWAR-36, ensures that the parent dir are created first
			to.getParentFile().mkdirs();

			if (encoding == null || encoding.length() < 1) {
				fileReader = new BufferedReader(new FileReader(from));
				fileWriter = new FileWriter(to);
			} else {
				FileInputStream instream = new FileInputStream(from);

				FileOutputStream outstream = new FileOutputStream(to);

				fileReader = new BufferedReader(new InputStreamReader(instream,
						encoding));

				fileWriter = new OutputStreamWriter(outstream, encoding);
			}

			Reader reader = fileReader;
			for (int i = 0; i < wrappers.length; i++) {
				FilterWrapper wrapper = wrappers[i];
				reader = wrapper.getReader(reader, filterProperties);
			}

			IOUtil.copy(reader, fileWriter);
		} finally {
			IOUtil.close(fileReader);
			IOUtil.close(fileWriter);
		}
	}

	/**
	 * TO DO: Remove this interface when Maven moves to plexus-utils version 1.4
	 */
	private interface FilterWrapper {
		Reader getReader(Reader fileReader, Map filterProperties);
	}

	/**
	 * Converts the filename of an artifact to artifactId-version.type format.
	 * 
	 * @param artifact
	 * @return converted filename of the artifact
	 */
	protected String getDefaultFinalName(Artifact artifact) {
		String finalName = artifact.getArtifactId() + "-"
				+ artifact.getVersion();

		String classifier = artifact.getClassifier();
		if ((classifier != null) && !("".equals(classifier.trim()))) {
			finalName += "-" + classifier;
		}

		finalName = finalName + "."
				+ artifact.getArtifactHandler().getExtension();
		return finalName;
	}

}
