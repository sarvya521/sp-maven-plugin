# Experian Maven Plugin

This is the [experian-maven-plugin](https://bitbucketglobal.experian.local/users/dameroy/repos/experian-maven-plugin/).
It supports the following goals:

* **experian:baseline** : Ensure that the module version is bumped correctly when a change occurs.
* **experian:bump-version** : Bump the version of the POM file based on the given parameters.
* **experian:help** :  Display help information on experian-maven-plugin.
* **experian:light-effective** : Generate a light effective POM file.
* **experian:revert** : Revert the changes made on the POM file and restore the backup.
* **experian:major-bump-helper** : Do a major bump of the specified project and a micro-SNAPSHOT bump of upstream dependencies if required. It also ensures that the modified poms will be using the latest parent version and that the new versions are synchronized in the experian dependency management pom.
* **experian:bump-new-release** : Prepares experian projects for a new release bumping modules according to the supplied parameters.

The plugin also defined some EnforcerRule like:
* **FixedParentVersionRule** : Fail the build if the parent version starts with '[' or '('
* **NoDependencyManagementRule** : Warn or Fail the build if the POM contains a dependencyManagement section
* **NoPluginManagementRule** : Warn or Fail the build if the POM contains a pluginManagement section
* **NoDependencyOrPluginVersionOverrideRule** : Warn or Fail the build if the dependency or plugin are overridden
* **ExperianPomRule** : Checks poms versions so that they are in line with experian git flow requirements

# Goals

## experian:help

Display the goals supported by this plugin.

## experian:baseline

This goal is based on the maven-bundle-plugin. It should be used to ensure that the module version is bumped correctly when a change occurs.

Like the maven-bundle-plugin, the plugin considers the @ConsumerType and @ProviderType and reacts in the same way.

The goal executes the following steps:

1. Retrieve the baseline version. By default, the baseline is the latest released version but it can be specified using the property comparisonVersion
2. It compares the bytecode of all the packages to detect changes.
3. It retrieves the exposed packages
    * For OSGi modules, it retrieves them from the Manifest
    * For Netbeans modules, it retrieves them from the Manifest
    * For pure Java JAR file, packages containing ".impl." or ".internal." are considered as private, all other are considered as exposed
4. It determines the highest changes from all exposed packages
5. It checks that the module version is aligned with the expected one (baseline version incremented by the highest detected change on an exposed package)
6.a If replace option disabled (default) the version is not the expected one, it fails and the build is stopped
6.b If replace option enabled it will change the version of the module with the expected one in SNAPSHOT

### Params
-DreplaceVersions --> optional (default = false) - use it to specify that the command will not only check the versions but also bump the module to the expected one
-DgenerateBackupPoms --> optional (default = true) - generates a backup of the modified pom

## experian:light-effective

This goal could be used to generate an effective POM file.

Compared to the maven-help-plugin:effective-pom, this goal doesn't generates the full effective pom.
It'll consolidate the dependencyManagement, pluginManagement, dependencies (optional), plugins (optional), properties (optional).
For dependencyManagement and pluginManagement, the goal also filter them to ensure that only the ones effectively used are
listed in the generated effective POM file.

The idea is to add a step in the Master build process (not on branches) before the usual “mvn clean install”
```    mvn com.experian.eda:experian-maven-plugin:1.2.0:light-effective versions:resolve-ranges```

And the following one after the Artifactory deployment (even if the build has failed)
```    mvn com.experian.eda:experian-maven-plugin:1.2.0:revert```

Doing that, the Master build process will:

1. Modify the pom file to contain all the dependencyManagement and pluginManagement sections (inherited from parents).
2. Resolve all the ranges to have a POM with fixed versions that we can replay anytime.
3. Perform the usual compilation
4. Deploy the artifacts and POM (with fixed versions) onto Artifactory
5. Revert the changes made on the POM

## experian:bump-version

This goal is used to bump the version of the POM file based on the given parameters.

### Execute the goal without parameters
```
~>mvn experian:bump-version
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal com.experian.eda:experian-maven-plugin:1.1.1:bump-version (default-cli) on project Framework_Scm_Client_Interfaces:
[ERROR] The parameters 'bumpToSnapshot', 'bumpType' for goal com.experian.eda:experian-maven-plugin:1.1.1:bump-version are missing or invalid
```

### Execute the goal without the bumpToSnapshot parameter
```
~>mvn experian:bump-version -DbumpType=MICRO
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal com.experian.eda:experian-maven-plugin:1.1.1:bump-version (default-cli) on project Framework_Scm_Client_Interfaces:
[ERROR] The parameters 'bumpToSnapshot' for goal com.experian.eda:experian-maven-plugin:1.1.1:bump-version are missing or invalid
```

### Bump to the next micro-SNAPSHOT version (in the traces you can see that the previous command worked)
```
~>mvn experian:bump-version -DbumpType=MICRO -DbumpToSnapshot=true
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Bump to the next micro version (in the traces you can see that the previous command worked)
```
~>mvn experian:bump-version -DbumpType=MICRO -DbumpToSnapshot=false
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.2-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Bump to the next minor version (in the traces you can see that the previous command worked)
```
~>mvn experian:bump-version -DbumpType=MINOR -DbumpToSnapshot=false
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.3
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Bump to the next major version (in the traces you can see that the previous command worked)
```
~>mvn experian:bump-version -DbumpType=MAJOR -DbumpToSnapshot=false
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.14.0
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Perform a clean to check that the major version bump has been correctly applied
```
~>mvn clean
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 2.0.0
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Scm_Client_Interfaces ---
[INFO] Deleting C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_Interfaces\target
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```





## experian:revert

This goal is used to revert the changes made on the POM file and restore the backup which has been made previously.

### Perform a micro version bump on an aggregator
```
~>mvn experian:bump-version -DbumpType=MICRO -DbumpToSnapshot=false
[INFO] Reactor Build Order:
[INFO] com.experian.eda.framework.properties.gui
[INFO] com.experian.eda.framework.scm.client.interfaces
[INFO] com.experian.eda.framework.scm.client.business
[INFO] com.experian.eda.framework.scm.client.gui
[INFO] com.experian.eda.framework.resources.palette
[INFO] Solution_Framework Aggregation POM
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.properties.gui 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Properties_GUI ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.business 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Business ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.gui 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Scm_Client_Gui ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.resources.palette 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Framework_Resources_Palette ---
[INFO] ------------------------------------------------------------------------
[INFO] Building Solution_Framework Aggregation POM 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:bump-version (default-cli) @ Solution_Framework_Aggregation_POM ---
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] Reactor Summary:
[INFO] com.experian.eda.framework.properties.gui .......... SUCCESS [  2.040 s]
[INFO] com.experian.eda.framework.scm.client.interfaces ... SUCCESS [  0.016 s]
[INFO] com.experian.eda.framework.scm.client.business ..... SUCCESS [  0.017 s]
[INFO] com.experian.eda.framework.scm.client.gui .......... SUCCESS [  0.018 s]
[INFO] com.experian.eda.framework.resources.palette ....... SUCCESS [  0.014 s]
[INFO] Solution_Framework Aggregation POM ................. SUCCESS [  0.598 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Perform a revert (in the traces you can see that the previous command worked)
```
~>mvn experian:revert
[INFO] Reactor Build Order:
[INFO] com.experian.eda.framework.properties.gui
[INFO] com.experian.eda.framework.scm.client.interfaces
[INFO] com.experian.eda.framework.scm.client.business
[INFO] com.experian.eda.framework.scm.client.gui
[INFO] com.experian.eda.framework.resources.palette
[INFO] Solution_Framework Aggregation POM
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.properties.gui 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Framework_Properties_GUI ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_Properties_GUI\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_Properties_GUI\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Framework_Scm_Client_Interfaces ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_Interfaces\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_Interfaces\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.business 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Framework_Scm_Client_Business ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_Business\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_Business\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.gui 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Framework_Scm_Client_Gui ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_GUI\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_SCM_Client_GUI\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.resources.palette 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Framework_Resources_Palette ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_Resources_Palette\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\Framework_Resources_Palette\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO] Building Solution_Framework Aggregation POM 1.13.2
[INFO] ------------------------------------------------------------------------
[INFO] --- experian-maven-plugin:1.1.1:revert (default-cli) @ Solution_Framework_Aggregation_POM ---
[INFO] Restoring C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\pom.xml from C:\Powercurve-WIP\common-components\capability-framework\modules\Solution_Framework\pom.xml.pomBackup
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] Reactor Summary:
[INFO] com.experian.eda.framework.properties.gui .......... SUCCESS [  2.038 s]
[INFO] com.experian.eda.framework.scm.client.interfaces ... SUCCESS [  0.027 s]
[INFO] com.experian.eda.framework.scm.client.business ..... SUCCESS [  0.028 s]
[INFO] com.experian.eda.framework.scm.client.gui .......... SUCCESS [  0.031 s]
[INFO] com.experian.eda.framework.resources.palette ....... SUCCESS [  0.031 s]
[INFO] Solution_Framework Aggregation POM ................. SUCCESS [  0.885 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Perform a clean (in the traces you can see that the previous command worked)
```
~>mvn clean
[INFO] Reactor Build Order:
[INFO] com.experian.eda.framework.properties.gui
[INFO] com.experian.eda.framework.scm.client.interfaces
[INFO] com.experian.eda.framework.scm.client.business
[INFO] com.experian.eda.framework.scm.client.gui
[INFO] com.experian.eda.framework.resources.palette
[INFO] Solution_Framework Aggregation POM
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.properties.gui 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Properties_GUI ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.interfaces 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Scm_Client_Interfaces ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.business 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Scm_Client_Business ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.scm.client.gui 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Scm_Client_Gui ---
[INFO] ------------------------------------------------------------------------
[INFO] Building com.experian.eda.framework.resources.palette 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Framework_Resources_Palette ---
[INFO] ------------------------------------------------------------------------
[INFO] Building Solution_Framework Aggregation POM 1.13.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ Solution_Framework_Aggregation_POM ---
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] Reactor Summary:
[INFO] com.experian.eda.framework.properties.gui .......... SUCCESS [  0.188 s]
[INFO] com.experian.eda.framework.scm.client.interfaces ... SUCCESS [  0.015 s]
[INFO] com.experian.eda.framework.scm.client.business ..... SUCCESS [  0.018 s]
[INFO] com.experian.eda.framework.scm.client.gui .......... SUCCESS [  0.021 s]
[INFO] com.experian.eda.framework.resources.palette ....... SUCCESS [  0.018 s]
[INFO] Solution_Framework Aggregation POM ................. SUCCESS [  0.060 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```




## experian:major-bump-helper

Available since v1.5.3
WARNING: this goal must be triggered at the root of the powercurve folder (com.experian.eda:master) to work.

This goal is used to help user doing a major bump in the Experian pom structure following this process:
1) Identify all modules using the specifed one
2) Do a major-SNAPSHOT bump of the specifed module
3) Ensure the specified module uses the latest com.experian.eda:parent pom version
4)For each of the modules identified in step 1, if not in -SNAPSHOT version do the following:
4.1 - Do a micro-SNAPSHOT bump
4.2 - Ensure it uses the latest com.experian.eda:parent pom version
5) For each of the poms bumped during step 2 & 4.1, synchronize their version in com.experian.eda.depmgmt:experian

### Usage
```   mvn com.experian.eda:experian-maven-plugin:1.5.3:major-bump-helper -DartifactId=Framework_Scm_Client_Interfaces -DgroupId=com.experian.eda.cap.framework -DgenerateBackupPoms=false  ```

### Params
-DgroupId --> mandatory - use it to specify the maven group id for the module which requires major bump
-DartifactId --> mandatory - use it to specify the maven artifact id for the module which requires major bump
-DgenerateBackupPoms --> optional (default = true) - generates a backup of the modified pom





## experian:micro-bump-release

Available since v1.5.4
WARNING: this goal must be triggered at the root of the powercurve folder (com.experian.eda:master) to work.

This goal will locate all modules which are in released version & using a parent which is not in release (ie is in SNAPSHOT).
For each of these it will:
1) Do a micro snapshot bump
2) Ensure that the parent version of the module is set to the value passed as parameter
3) Save the pom changes

### Usage
```   mvn com.experian.eda:experian-maven-plugin:1.5.4:micro-bump-release -DparentVersion=1.1.0-SNAPSHOT -DgenerateBackupPoms=false  ```

### Params
-DparentVersion --> mandatory - the parent version to set for the modules which requires bumping
-DgenerateBackupPoms --> optional (default = true) - generates a backup of the modified pom




## experian:bump-new-release

Available since v1.5.10
WARNING: this goal must be triggered from the release folder of the powercurve monorepo to work (com.experian.eda:release-manager).

This goal will look for all the modules in the aggregator, bump them depending on the supplied parameters, synchronize their parent & the associated dependency management poms.

### Usage
```   mvn com.experian.eda:experian-maven-plugin:1.5.10:bump-new-release -Dqualifier=-19R1-alpha -DbumpAll=false  ```

### Params
-DbumpAll --> mandatory - if true, all modules of the reactor will be bumped according to the supplied parameters
-Dqualifier --> optional (default = "") - if empty or not supplied, will do a micro bump of the selected modules. If not empty, will use the supplied value and append or replace it as version qualifier to the existing version.
-DreplaceQualifier --> optional (default = "true") - if true will replace the existing qualifier with the supplied qualifier. If false, will happend the supplied qualifier to the existing.
-Dsnapshot --> optional (default = "true") - if true will add the -SNAPSHOT value to the computed version.
