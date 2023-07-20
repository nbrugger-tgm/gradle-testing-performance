plugins {
    id("java")
    id("testing")
}
val java = extensions.getByType<JavaPluginExtension>();
val main = java.sourceSets.getByName("main");
val generateJava = tasks.register<Sync>("generateTests") {
//    destinationDir = file("$buildDir/generated/test")
    into("$buildDir/generated/test")

    for (i in 1..200) {
        with(copySpec {
            from("$rootDir/template/src/test/java")
                .expand(mapOf("number" to i.toString()))
                .rename("\\\$number", i.toString())
        })
    }
}

val test = java.sourceSets.getByName("test");
test.java.srcDir("$buildDir/generated/test")
tasks.withType<JavaCompile> {
    dependsOn(generateJava)
}