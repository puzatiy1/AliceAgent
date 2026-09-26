# Build Log
Downloading https://services.gradle.org/distributions/gradle-8.3-bin.zip
............10%............20%.............30%............40%.............50%............60%.............70%............80%.............90%............100%
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.3/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 34

This Android Gradle plugin (8.1.0) was tested up to compileSdk = 33 (and compileSdkPreview = "UpsideDownCakePrivacySandbox").

You are strongly encouraged to update your project to use a newer
Android Gradle plugin that has been tested with compileSdk = 34.

If you are already using the latest version of the Android Gradle plugin,
you may need to wait until a newer version with support for compileSdk = 34 is available.

To suppress this warning, add/update
    android.suppressUnsupportedCompileSdk=34
to this project's gradle.properties.
Checking the license for package Android SDK Build-Tools 33.0.1 in /usr/local/lib/android/sdk/licenses
License for package Android SDK Build-Tools 33.0.1 accepted.
Preparing "Install Android SDK Build-Tools 33.0.1 v.33.0.1".
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" ready.
Installing Android SDK Build-Tools 33.0.1 in /usr/local/lib/android/sdk/build-tools/33.0.1
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" complete.
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" finished.
> Task :app:buildKotlinToolingMetadata
> Task :app:preBuild UP-TO-DATE
> Task :app:preReleaseBuild UP-TO-DATE
> Task :app:generateReleaseResValues
> Task :app:generateReleaseResources
> Task :app:dataBindingMergeDependencyArtifactsRelease
> Task :app:packageReleaseResources
> Task :app:mergeReleaseResources
> Task :app:generateReleaseBuildConfig
> Task :app:parseReleaseLocalResources
> Task :app:checkReleaseAarMetadata
> Task :app:dataBindingGenBaseClassesRelease
> Task :app:mapReleaseSourceSetPaths
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:javaPreCompileRelease
> Task :app:extractProguardFiles
> Task :app:mergeReleaseJniLibFolders
> Task :app:mergeReleaseNativeLibs NO-SOURCE
> Task :app:stripReleaseDebugSymbols NO-SOURCE
> Task :app:processReleaseManifestForPackage
> Task :app:extractReleaseNativeSymbolTables NO-SOURCE
> Task :app:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :app:desugarReleaseFileDependencies
> Task :app:checkReleaseDuplicateClasses
> Task :app:mergeReleaseArtProfile
> Task :app:processReleaseResources
> Task :app:mergeExtDexRelease
> Task :app:mergeReleaseShaders
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets
> Task :app:compressReleaseAssets
> Task :app:collectReleaseDependencies
> Task :app:sdkReleaseDependencyData
> Task :app:validateSigningRelease
> Task :app:writeReleaseAppMetadata
> Task :app:writeReleaseSigningConfigVersions
> Task :app:optimizeReleaseResources

> Task :app:compileReleaseKotlin
w: file:///home/runner/work/AliceAgent/AliceAgent/app/src/main/java/com/aiagent/alice/MainActivity.kt:167:37 Parameter 'userText' is never used

> Task :app:compileReleaseJavaWithJavac
> Task :app:dexBuilderRelease
> Task :app:mergeReleaseGlobalSynthetics
> Task :app:processReleaseJavaRes
> Task :app:mergeReleaseJavaResource
> Task :app:mergeDexRelease
> Task :app:compileReleaseArtProfile
> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:lintVitalAnalyzeRelease
> Task :app:lintVitalReportRelease
> Task :app:lintVitalRelease
> Task :app:assembleRelease

BUILD SUCCESSFUL in 1m 28s
45 actionable tasks: 45 executed
