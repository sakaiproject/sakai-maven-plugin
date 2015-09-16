# Sakai Maven Plugin

This is the Sakai Maven plugin that deploys a copy of Sakai into a copy of Tomcat.

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

