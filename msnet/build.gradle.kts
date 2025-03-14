import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)

    `maven-publish`
    signing
}

android {
    namespace = "msnet"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }

    publishing {
        singleVariant("release") {
            // if you don't want sources/javadoc, remove these lines
            withSourcesJar()
//            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okio)

    compileOnly(libs.cronet.api)

    api(libs.okcronet)
}

//---------- maven upload info -----------------------------------

val versionName = "1.0.3"

var signingKeyId = ""//签名的密钥后8位
var signingPassword = ""//签名设置的密码
var secretKeyRingFile = ""//生成的secring.gpg文件目录


val localProperties: File = project.rootProject.file("local.properties")

if (localProperties.exists()) {
    println("Found secret props file, loading props")
    val properties = Properties()

    InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }
    signingKeyId = properties.getProperty("signing.keyId")
    signingPassword = properties.getProperty("signing.password")
    secretKeyRingFile = properties.getProperty("signing.secretKeyRingFile")

} else {
    println("No props file, loading env vars")
}

afterEvaluate {

    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release"))
                groupId = "io.github.limuyang2"
                artifactId = "msnet"
                version = versionName

                pom {
                    name.value("msnet")
                    description.value("A network request library.")
                    url.value("https://github.com/limuyang2/msnet")

                    licenses {
                        license {
                            //协议类型
                            name.value("The MIT License")
                            url.value("https://github.com/limuyang2/msnet/blob/main/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.value("limuyang2")
                            name.value("limuyang")
                            email.value("limuyang2@hotmail.com")
                        }
                    }

                    scm {
                        connection.value("scm:git@github.com:limuyang2/okcronet.git")
                        developerConnection.value("scm:git@github.com:limuyang2/okcronet.git")
                        url.value("https://github.com/limuyang2/msnet")
                    }
                }
            }

        }

        repositories {
            maven {
                setUrl("$rootDir/RepoDir")
            }
        }



    }

}

gradle.taskGraph.whenReady {
    if (allTasks.any { it is Sign }) {

        allprojects {
            extra["signing.keyId"] = signingKeyId
            extra["signing.secretKeyRingFile"] = secretKeyRingFile
            extra["signing.password"] = signingPassword
        }
    }
}

signing {
    sign(publishing.publications)
}