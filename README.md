# Sakai Maven Plugin

So this plugin has 2 functions:

- sakai-component packaging. This is Sakai's own packaging type that gets deployed into the component folder
- the deploy/deploy-exploded goal that deploys artifacts from the local maven repository into a copy of tomcat.

## Testing Plugin

The Sakai project normaly binds to a released version of the plugin. To quickly test new versions you
can tell maven to use a newer versions of the plugin on the command line with:

    mvn org.sakaiproject.maven.plugins:sakai:1.4.4-SNAPSHOT:deploy-exploded

## Releasing

This plugin is released to the OSS Sonatype repository using the maven release plugin.
You will need to be setup with an account there and have a GPG key to sign the release.
http://central.sonatype.org/pages/ossrh-guide.html

There is a relase profile which is not used by default but will be used when making the release.
You can test that the release profile works ok for you by doing:

    mvn -Prelease deploy

this will attempt to build and deploy a SNAPSHOT version of the maven Sakai plugin to the
Sonatype OSS repository. If it all builds ok then you can attempt a local release, first
have the release plugin update the versions and create the tag. Having `-DpushChanges=false` 
means that if the release fails for some reason you haven't tainted the central repository
with the version increments.

    mvn -DpushChanges=false release:prepare

Then if it all looks ok you can make the release to the Sonatype staging repository:

    mvn -DlocalCheckout=true release:perform

because you didn't push the changes you need the `-DlocalCheckout=true` option. Once it's appeared
in OSS Sonatype and all looks ok you need to push your changes.

    git push 

Then you can publish the staged artifact in the Sonatype repository.

If you are pretty confident that it's all going to release ok and are happy with the default maven numbers:

    mvn -B release:prepare release:perform

